package com.meteory.optimizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.meteory.optimizer.navigation.*
import com.meteory.optimizer.ui.components.MeteoryBottomNavBar
import com.meteory.optimizer.ui.theme.MeteoryTheme
import com.meteory.optimizer.utils.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkTheme by remember { mutableStateOf(true) }
            MeteoryTheme(darkTheme = darkTheme) {
                MeteoryApp(onThemeToggle = { darkTheme = !darkTheme })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeteoryApp(onThemeToggle: () -> Unit) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            MeteoryBottomNavBar(
                items = bottomNavItems,
                currentRoute = currentRoute,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            MeteoryNavGraph(navController = navController)
        }
    }
}
