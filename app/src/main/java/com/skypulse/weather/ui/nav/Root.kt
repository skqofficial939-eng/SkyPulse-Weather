package com.skypulse.weather.ui.nav

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.skypulse.weather.SkyPulseApp
import com.skypulse.weather.ui.SearchViewModel
import com.skypulse.weather.ui.WeatherViewModel
import com.skypulse.weather.ui.alerts.AlertsScreen
import com.skypulse.weather.ui.factoryOf
import com.skypulse.weather.ui.forecast.ForecastScreen
import com.skypulse.weather.ui.home.HomeScreen
import com.skypulse.weather.ui.saved.SavedScreen
import com.skypulse.weather.ui.search.SearchScreen
import com.skypulse.weather.ui.settings.SettingsScreen
import com.skypulse.weather.ui.theme.SkyPulseTheme
import com.skypulse.weather.ui.theme.resolveDark

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Filled.Home),
    FORECAST("forecast", "Forecast", Icons.Filled.CalendarMonth),
    SAVED("saved", "Places", Icons.Filled.Place),
    ALERTS("alerts", "Alerts", Icons.Filled.NotificationsActive),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}

private const val ROUTE_SEARCH = "search"

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun SkyPulseRoot(vm: WeatherViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    SkyPulseTheme(state.settings.theme) {
        val dark = resolveDark(state.settings.theme)
        val activity = LocalContext.current as ComponentActivity
        DisposableEffect(dark) {
            val transparent = android.graphics.Color.TRANSPARENT
            val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
            activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            onDispose { }
        }

        val nav = rememberNavController()
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }
        val route = nav.currentBackStackEntryAsState().value?.destination?.route
        val container = (LocalContext.current.applicationContext as SkyPulseApp).container

        Scaffold(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (Dest.entries.any { it.route == route }) {
                    NavigationBar(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface) {
                        Dest.entries.forEach { d ->
                            NavigationBarItem(
                                selected = route == d.route,
                                onClick = { nav.goTab(d.route) },
                                icon = { Icon(d.icon, contentDescription = null) },
                                label = { Text(d.label) },
                                colors = NavigationBarItemDefaults.colors(),
                            )
                        }
                    }
                }
            },
        ) { inner ->
            NavHost(nav, startDestination = Dest.HOME.route, modifier = Modifier.padding(inner)) {
                composable(Dest.HOME.route) {
                    HomeScreen(vm, onSearch = { nav.navigate(ROUTE_SEARCH) }, onOpenSaved = { nav.goTab(Dest.SAVED.route) })
                }
                composable(Dest.FORECAST.route) { ForecastScreen(vm, onSearch = { nav.navigate(ROUTE_SEARCH) }) }
                composable(Dest.SAVED.route) {
                    SavedScreen(vm, onAdd = { nav.navigate(ROUTE_SEARCH) }, onOpen = { nav.goTab(Dest.HOME.route) })
                }
                composable(Dest.ALERTS.route) { AlertsScreen(vm) }
                composable(Dest.SETTINGS.route) { SettingsScreen(vm) }
                composable(ROUTE_SEARCH) {
                    val svm: SearchViewModel = viewModel(factory = factoryOf { SearchViewModel(container) })
                    SearchScreen(vm, svm, onBack = { nav.popBackStack() }, onPicked = { nav.goTab(Dest.HOME.route) })
                }
            }
        }
    }
}
