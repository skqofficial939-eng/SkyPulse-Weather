package com.skypulse.weather

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.skypulse.weather.domain.NO_PLACE
import com.skypulse.weather.ui.WeatherViewModel
import com.skypulse.weather.ui.factoryOf
import com.skypulse.weather.ui.nav.SkyPulseRoot

class MainActivity : ComponentActivity() {

    private val vm: WeatherViewModel by viewModels { factoryOf { WeatherViewModel(application as SkyPulseApp) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openPlaceFrom(intent)
        setContent { SkyPulseRoot(vm) }
    }

    override fun onStart() {
        super.onStart()
        vm.refreshIfStale()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openPlaceFrom(intent)
    }

    /** A tapped notification carries the place it was about; show that place. */
    private fun openPlaceFrom(i: Intent?) {
        val id = i?.getLongExtra(EXTRA_PLACE_ID, NO_PLACE) ?: NO_PLACE
        if (id != NO_PLACE) vm.select(id)
    }

    companion object {
        const val EXTRA_PLACE_ID = "com.skypulse.weather.PLACE_ID"
    }
}
