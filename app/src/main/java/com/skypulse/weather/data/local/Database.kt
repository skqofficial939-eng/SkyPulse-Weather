package com.skypulse.weather.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.skypulse.weather.domain.Place
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val admin1: String?,
    val country: String?,
    val countryCode: String?,
    val latitude: Double,
    val longitude: Double,
    val timezone: String?,
    val isFavorite: Boolean,
    val sortOrder: Int,
)

fun PlaceEntity.toDomain() = Place(id, name, admin1, country, countryCode, latitude, longitude, timezone, isFavorite, sortOrder)
fun Place.toEntity() = PlaceEntity(id, name, admin1, country, countryCode, latitude, longitude, timezone, isFavorite, sortOrder)

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val placeId: Long,
    val forecastJson: String,
    val airJson: String?,
    val fetchedAtMillis: Long,
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val atMillis: Long,
)

/** Persisted so a notification is never repeated, even after a process restart. */
@Entity(tableName = "notified")
data class NotifiedEntity(
    @PrimaryKey val key: String,
    val atMillis: Long,
)

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places ORDER BY sortOrder ASC, name ASC")
    suspend fun getAll(): List<PlaceEntity>

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun get(id: Long): PlaceEntity?

    /** IGNORE makes duplicate adds a no-op; returns -1 when the place already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(place: PlaceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(place: PlaceEntity)

    @Update
    suspend fun update(place: PlaceEntity)

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM places")
    suspend fun nextSortOrder(): Int
}

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE placeId = :placeId")
    suspend fun get(placeId: Long): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache")
    suspend fun clear()

    @Query("DELETE FROM weather_cache WHERE placeId = :placeId")
    suspend fun clear(placeId: Long)
}

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY atMillis DESC LIMIT 8")
    fun observe(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: RecentSearchEntity)

    @Query("DELETE FROM recent_searches")
    suspend fun clear()
}

@Dao
interface NotifiedDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: NotifiedEntity): Long

    @Query("DELETE FROM notified WHERE atMillis < :before")
    suspend fun prune(before: Long)

    @Query("DELETE FROM notified")
    suspend fun clear()
}

@Database(
    entities = [PlaceEntity::class, WeatherCacheEntity::class, RecentSearchEntity::class, NotifiedEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SkyPulseDatabase : RoomDatabase() {
    abstract fun places(): PlaceDao
    abstract fun cache(): WeatherCacheDao
    abstract fun recents(): RecentSearchDao
    abstract fun notified(): NotifiedDao

    companion object {
        fun create(context: Context): SkyPulseDatabase =
            Room.databaseBuilder(context, SkyPulseDatabase::class.java, "skypulse.db").build()
    }
}
