package com.skypulse.weather.data.repo

import com.skypulse.weather.data.local.NotifiedEntity
import com.skypulse.weather.data.local.RecentSearchEntity
import com.skypulse.weather.data.local.SkyPulseDatabase
import com.skypulse.weather.data.local.WeatherCacheEntity
import com.skypulse.weather.data.local.toDomain
import com.skypulse.weather.data.local.toEntity
import com.skypulse.weather.data.remote.AirQualityDto
import com.skypulse.weather.data.remote.AppJson
import com.skypulse.weather.data.remote.ForecastDto
import com.skypulse.weather.data.remote.OpenMeteoApi
import com.skypulse.weather.data.remote.toPlace
import com.skypulse.weather.data.remote.toSnapshot
import com.skypulse.weather.domain.AppError
import com.skypulse.weather.domain.CURRENT_LOCATION_ID
import com.skypulse.weather.domain.CachePolicy
import com.skypulse.weather.domain.NotifiedStore
import com.skypulse.weather.domain.Place
import com.skypulse.weather.domain.WeatherException
import com.skypulse.weather.domain.WeatherSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

fun Throwable.toWeatherException(): WeatherException = when (this) {
    is WeatherException -> this
    is HttpException -> when (code()) {
        429 -> WeatherException(AppError.RATE_LIMITED, "The weather service is rate-limiting requests. Try again in a few minutes.", this)
        in 500..599 -> WeatherException(AppError.SERVER, "The weather service is having trouble right now.", this)
        else -> WeatherException(AppError.SERVER, "The weather service returned an error (${code()}).", this)
    }
    is SerializationException, is IllegalArgumentException ->
        WeatherException(AppError.BAD_DATA, "The weather service sent data the app could not read.", this)
    is IOException -> WeatherException(AppError.OFFLINE, "Can't reach the weather service. Check your connection.", this)
    else -> WeatherException(AppError.UNKNOWN, "Something went wrong.", this)
}

interface WeatherRepository {
    suspend fun cached(placeId: Long): WeatherSnapshot?
    suspend fun refresh(place: Place, force: Boolean = false): Result<WeatherSnapshot>
    suspend fun clearCache()
}

class OpenMeteoWeatherRepository(
    private val api: OpenMeteoApi,
    private val db: SkyPulseDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : WeatherRepository {

    override suspend fun cached(placeId: Long): WeatherSnapshot? {
        val row = db.cache().get(placeId) ?: return null
        return runCatching {
            val f = AppJson.decodeFromString(ForecastDto.serializer(), row.forecastJson)
            val a = row.airJson?.let { AppJson.decodeFromString(AirQualityDto.serializer(), it) }
            f.toSnapshot(placeId, row.fetchedAtMillis, a).copy(fromCache = true)
        }.getOrNull()
    }

    override suspend fun refresh(place: Place, force: Boolean): Result<WeatherSnapshot> {
        if (!force) {
            val hit = db.cache().get(place.id)
            if (hit != null && CachePolicy.isFresh(hit.fetchedAtMillis, clock())) {
                cached(place.id)?.let { return Result.success(it.copy(fromCache = false)) }
            }
        }
        return try {
            val forecast = api.forecast(place.latitude, place.longitude)
            // Air quality is optional: its failure must not hide the forecast.
            val air = try { api.airQuality(place.latitude, place.longitude) } catch (e: CancellationException) { throw e } catch (airError: Exception) { null }
            val now = clock()
            val snapshot = forecast.toSnapshot(place.id, now, air)
            db.cache().put(
                WeatherCacheEntity(
                    placeId = place.id,
                    forecastJson = AppJson.encodeToString(ForecastDto.serializer(), forecast),
                    airJson = air?.let { AppJson.encodeToString(AirQualityDto.serializer(), it) },
                    fetchedAtMillis = now,
                ),
            )
            Result.success(snapshot)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e.toWeatherException())
        }
    }

    override suspend fun clearCache() = db.cache().clear()
}

interface PlaceRepository {
    val places: Flow<List<Place>>
    val recentSearches: Flow<List<String>>
    suspend fun search(query: String): Result<List<Place>>
    suspend fun add(place: Place): Boolean
    suspend fun get(id: Long): Place?
    suspend fun all(): List<Place>
    suspend fun remove(id: Long)
    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun move(id: Long, up: Boolean)
    suspend fun saveCurrentLocation(place: Place)
    suspend fun rememberSearch(query: String)
    suspend fun clearRecents()
}

class OpenMeteoPlaceRepository(
    private val api: OpenMeteoApi,
    private val db: SkyPulseDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : PlaceRepository {

    override val places: Flow<List<Place>> = db.places().observeAll().map { rows -> rows.map { it.toDomain() } }
    override val recentSearches: Flow<List<String>> = db.recents().observe().map { rows -> rows.map { it.query } }

    override suspend fun search(query: String): Result<List<Place>> {
        val q = query.trim()
        if (q.length < 2) return Result.success(emptyList())
        return try {
            val results = api.search(q).results.orEmpty().map { it.toPlace() }.distinctBy { it.id }
            Result.success(results)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e.toWeatherException())
        }
    }

    override suspend fun add(place: Place): Boolean {
        val order = db.places().nextSortOrder()
        return db.places().insertIfAbsent(place.copy(sortOrder = order).toEntity()) != -1L
    }

    override suspend fun get(id: Long): Place? = db.places().get(id)?.toDomain()
    override suspend fun all(): List<Place> = db.places().getAll().map { it.toDomain() }

    override suspend fun remove(id: Long) {
        db.places().delete(id)
        db.cache().clear(id)
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        val p = db.places().get(id) ?: return
        db.places().update(p.copy(isFavorite = favorite))
    }

    override suspend fun move(id: Long, up: Boolean) {
        val list = db.places().getAll()
        val i = list.indexOfFirst { it.id == id }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in list.indices) return
        val a = list[i]; val b = list[j]
        db.places().update(a.copy(sortOrder = b.sortOrder))
        db.places().update(b.copy(sortOrder = a.sortOrder))
        // Equal sortOrders would make the swap a no-op, so normalise afterwards.
        db.places().getAll().forEachIndexed { idx, e -> if (e.sortOrder != idx) db.places().update(e.copy(sortOrder = idx)) }
    }

    override suspend fun saveCurrentLocation(place: Place) {
        require(place.id == CURRENT_LOCATION_ID)
        val existing = db.places().get(CURRENT_LOCATION_ID)
        val order = existing?.sortOrder ?: db.places().nextSortOrder()
        db.places().upsert(place.copy(sortOrder = order, isFavorite = existing?.isFavorite ?: false).toEntity())
        db.cache().clear(CURRENT_LOCATION_ID) // coordinates changed; old forecast is for somewhere else
    }

    override suspend fun rememberSearch(query: String) {
        val q = query.trim()
        if (q.length >= 2) db.recents().put(RecentSearchEntity(q, clock()))
    }

    override suspend fun clearRecents() = db.recents().clear()
}

class RoomNotifiedStore(private val db: SkyPulseDatabase, private val clock: () -> Long = System::currentTimeMillis) : NotifiedStore {
    override suspend fun claim(key: String): Boolean = db.notified().insert(NotifiedEntity(key, clock())) != -1L
    override suspend fun pruneOlderThan(millis: Long) = db.notified().prune(clock() - millis)
}
