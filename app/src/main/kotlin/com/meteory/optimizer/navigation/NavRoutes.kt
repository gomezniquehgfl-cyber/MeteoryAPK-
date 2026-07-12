package com.meteory.optimizer.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home        : Screen("home",        "Inicio",      Icons.Default.Home)
    object Gaming      : Screen("gaming",      "Gaming",      Icons.Default.SportsEsports)
    object Cleaning    : Screen("cleaning",    "Limpieza",    Icons.Default.CleaningServices)
    object Performance : Screen("performance", "CPU/RAM",     Icons.Default.Speed)
    object Battery     : Screen("battery",     "Batería",     Icons.Default.BatteryFull)
    object Privacy     : Screen("privacy",     "Privacidad",  Icons.Default.Security)
    object Network     : Screen("network",     "Red",         Icons.Default.Wifi)
    object Ai          : Screen("ai",          "IA",          Icons.Default.AutoAwesome)
    object Tools       : Screen("tools",       "Herramientas",Icons.Default.Build)
    object Assistant   : Screen("assistant",   "Asistente",   Icons.Default.SmartToy)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Gaming,
    Screen.Cleaning,
    Screen.Performance,
    Screen.Battery
)

val drawerNavItems = listOf(
    Screen.Privacy,
    Screen.Network,
    Screen.Ai,
    Screen.Tools,
    Screen.Assistant
)
