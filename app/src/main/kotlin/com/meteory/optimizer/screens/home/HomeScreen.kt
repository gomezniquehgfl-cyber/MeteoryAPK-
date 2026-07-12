package com.meteory.optimizer.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.meteory.optimizer.navigation.Screen
import com.meteory.optimizer.ui.components.*
import com.meteory.optimizer.ui.theme.*
import com.meteory.optimizer.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Neutral950)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text  = "Meteory",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text  = "Optimizer",
                    style = MaterialTheme.typography.labelLarge,
                    color = Primary400
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.shizukuAvailable) {
                    AssistChip(
                        onClick = {},
                        label   = { Text("Shizuku", fontSize = 10.sp, color = Accent500) },
                        colors  = AssistChipDefaults.assistChipColors(containerColor = Accent500.copy(0.1f)),
                        border  = BorderStroke(1.dp, Accent500.copy(0.3f))
                    )
                }
                IconButton(onClick = { navController.navigate(Screen.Assistant.route) }) {
                    Icon(Icons.Default.SmartToy, null, tint = Primary400)
                }
            }
        }

        // System Score
        SystemScoreCard(score = state.systemScore)

        // Quick metrics 2x2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressGauge(
                percent = state.cpuPercent,
                label   = "CPU",
                color   = Primary400,
                modifier = Modifier.weight(1f)
            )
            CircularProgressGauge(
                percent = state.ramPercent,
                label   = "RAM",
                color   = Warning400,
                modifier = Modifier.weight(1f)
            )
            CircularProgressGauge(
                percent = state.storageUsedPct,
                label   = "Almac.",
                color   = Accent500,
                modifier = Modifier.weight(1f)
            )
            CircularProgressGauge(
                percent = state.batteryLevel,
                label   = "Batería",
                color   = if (state.isCharging) Success400 else Success300,
                centerText = "${state.batteryLevel}%",
                modifier = Modifier.weight(1f)
            )
        }

        // Temperature card
        MetricCard(
            title      = "Temperatura CPU",
            value      = "${"%.1f".format(state.tempC)}°C",
            subtitle   = when {
                state.tempC < 40f -> "Frío — Excelente"
                state.tempC < 55f -> "Tibio — Normal"
                state.tempC < 70f -> "Caliente — Atención"
                else              -> "Crítico — Riesgo"
            },
            icon       = Icons.Default.Thermostat,
            accentColor = when {
                state.tempC < 40f -> TempCool
                state.tempC < 55f -> TempWarm
                state.tempC < 70f -> TempHot
                else              -> TempCritic
            }
        )

        // Network speed
        MetricCard(
            title      = "Red",
            value      = "${state.dlKbps} KB/s ↓ · ${state.ulKbps} KB/s ↑",
            icon       = Icons.Default.Wifi,
            accentColor = Primary300
        )

        // Quick actions grid
        SectionHeader("Acceso Rápido", modifier = Modifier.padding(top = 4.dp))
        QuickActionsGrid(navController)

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SystemScoreCard(score: Int) {
    val color = when {
        score >= 80 -> Success400
        score >= 60 -> Warning400
        score >= 40 -> Warning500
        else        -> Error400
    }
    val label = when {
        score >= 80 -> "Sistema Óptimo"
        score >= 60 -> "Rendimiento Normal"
        score >= 40 -> "Requiere Atención"
        else        -> "Rendimiento Bajo"
    }

    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(color.copy(0.08f), Color.Transparent)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text  = "Salud del Sistema",
                        style = MaterialTheme.typography.labelMedium,
                        color = Neutral400
                    )
                    Text(
                        text  = "$score / 100",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Neutral300
                    )
                }
                CircularProgressGauge(
                    percent = score,
                    label   = "",
                    color   = color,
                    size    = 80.dp,
                    strokeWidth = 6.dp
                )
            }
            Spacer(Modifier.height(12.dp))
            MeteoryLinearProgress(
                progress = score / 100f,
                color    = color,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun QuickActionsGrid(navController: NavController) {
    val items = listOf(
        Triple("Gaming Brutal", Icons.Default.SportsEsports, Screen.Gaming),
        Triple("Limpieza", Icons.Default.CleaningServices, Screen.Cleaning),
        Triple("CPU / RAM", Icons.Default.Speed, Screen.Performance),
        Triple("Batería", Icons.Default.BatteryFull, Screen.Battery),
        Triple("Privacidad", Icons.Default.Security, Screen.Privacy),
        Triple("Red", Icons.Default.Wifi, Screen.Network),
        Triple("IA", Icons.Default.AutoAwesome, Screen.Ai),
        Triple("Herramientas", Icons.Default.Build, Screen.Tools)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (label, icon, screen) ->
                    QuickActionTile(
                        label    = label,
                        icon     = icon,
                        modifier = Modifier.weight(1f),
                        onClick  = { navController.navigate(screen.route) }
                    )
                }
                if (row.size < 4) {
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        border = BorderStroke(1.dp, Neutral700)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement   = Arrangement.Center,
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = Primary400, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                text      = label,
                style     = MaterialTheme.typography.labelSmall,
                color     = Neutral200,
                maxLines  = 1
            )
        }
    }
}
