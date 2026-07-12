package com.meteory.optimizer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.meteory.optimizer.screens.ai.AiScreen
import com.meteory.optimizer.screens.assistant.AssistantScreen
import com.meteory.optimizer.screens.battery.BatteryScreen
import com.meteory.optimizer.screens.cleaning.CleaningScreen
import com.meteory.optimizer.screens.gaming.GamingScreen
import com.meteory.optimizer.screens.home.HomeScreen
import com.meteory.optimizer.screens.network.NetworkScreen
import com.meteory.optimizer.screens.performance.PerformanceScreen
import com.meteory.optimizer.screens.privacy.PrivacyScreen
import com.meteory.optimizer.screens.tools.ToolsScreen

@Composable
fun MeteoryNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route)        { HomeScreen(navController) }
        composable(Screen.Gaming.route)      { GamingScreen() }
        composable(Screen.Cleaning.route)    { CleaningScreen() }
        composable(Screen.Performance.route) { PerformanceScreen() }
        composable(Screen.Battery.route)     { BatteryScreen() }
        composable(Screen.Privacy.route)     { PrivacyScreen() }
        composable(Screen.Network.route)     { NetworkScreen() }
        composable(Screen.Ai.route)          { AiScreen() }
        composable(Screen.Tools.route)       { ToolsScreen() }
        composable(Screen.Assistant.route)   { AssistantScreen() }
    }
}
