package com.meteory.optimizer.screens.network

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meteory.optimizer.ui.components.*
import com.meteory.optimizer.ui.theme.*
import com.meteory.optimizer.utils.AdbCommands
import com.meteory.optimizer.utils.ShizukuUtils
import com.meteory.optimizer.utils.SystemInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun NetworkScreen() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var dlKbps by remember { mutableLongStateOf(0L) }
    var ulKbps by remember { mutableLongStateOf(0L) }
    var signal by remember { mutableIntStateOf(0) }
    var dlHistory by remember { mutableStateOf(listOf<Int>()) }
    var isOptimizing by remember { mutableStateOf(false) }
    var optimizeResult by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (isActive) {
            val speed = SystemInfo.getNetworkSpeed()
            dlKbps = speed.downloadKbps
            ulKbps = speed.uploadKbps
            signal = SystemInfo.getWifiSignalStrength(context)
            dlHistory = (dlHistory + dlKbps.toInt()).takeLast(30)
            delay(1500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Neutral950)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Conectividad & Red",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        // Speed overview
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, Primary400.copy(0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                    SpeedIndicator("↓ Descarga", dlKbps, Primary400)
                    SpeedIndicator("↑ Subida", ulKbps, Accent500)
                    SignalIndicator(signal)
                }
                SectionHeader("Historial Descarga")
                SparkLineChart(values = dlHistory, color = Primary400, modifier = Modifier.fillMaxWidth())
            }
        }

        // Optimization
        SectionHeader("Optimización de Red")
        GlowButton(
            text     = if (isOptimizing) "Optimizando..." else "Optimizar Red para Gaming",
            onClick  = {
                scope.launch {
                    isOptimizing = true
                    ShizukuUtils.execBestEffort(AdbCommands.TCP_NO_DELAY)
                    ShizukuUtils.execBestEffort(AdbCommands.TCP_FAST_OPEN)
                    ShizukuUtils.execBestEffort(AdbCommands.WIFI_SCAN_ALWAYS)
                    ShizukuUtils.execBestEffort(AdbCommands.WIFI_SLEEP_OFF)
                    delay(2000)
                    isOptimizing = false
                    optimizeResult = "✅ TCP NoDelay activo · Fast Open habilitado · Wi-Fi optimizado"
                }
            },
            enabled  = !isOptimizing,
            modifier = Modifier.fillMaxWidth(),
            color    = Primary400
        )

        if (optimizeResult.isNotBlank()) {
            Text(optimizeResult, color = Success400, style = MaterialTheme.typography.bodySmall)
        }

        // Network tips
        SectionHeader("Ajustes de Red")
        listOf(
            Triple("TCP Sin Retraso", "Reduce latencia en juegos", Icons.Default.Speed),
            Triple("Wi-Fi sin suspensión", "Mantiene conexión activa", Icons.Default.WifiLock),
            Triple("Bloquear escaneo BG", "Evita interrupciones", Icons.Default.WifiOff),
            Triple("DNS optimizado", "Resolución más rápida", Icons.Default.Dns)
        ).forEach { (title, subtitle, icon) ->
            NetworkTipRow(title, subtitle, icon)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SpeedIndicator(label: String, kbps: Long, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Neutral400)
        val (value, unit) = if (kbps >= 1024) "${kbps / 1024}" to "MB/s" else "$kbps" to "KB/s"
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = color)
        Text(unit, style = MaterialTheme.typography.labelSmall, color = Neutral400)
    }
}

@Composable
private fun SignalIndicator(level: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Señal", style = MaterialTheme.typography.labelSmall, color = Neutral400)
        Icon(
            when (level) {
                0    -> Icons.Default.SignalWifiOff
                1, 2 -> Icons.Default.NetworkWifi1Bar
                3    -> Icons.Default.NetworkWifi3Bar
                else -> Icons.Default.NetworkWifi
            },
            contentDescription = null,
            tint     = when (level) {
                0, 1 -> Error400; 2, 3 -> Warning400; else -> Success400
            },
            modifier = Modifier.size(28.dp)
        )
        Text("$level/5", style = MaterialTheme.typography.labelSmall, color = Neutral400)
    }
}

@Composable
private fun NetworkTipRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, Neutral700, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = Primary300, modifier = Modifier.size(20.dp))
        Column {
            Text(title, color = Neutral200, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = Neutral500, style = MaterialTheme.typography.bodySmall)
        }
    }
}
