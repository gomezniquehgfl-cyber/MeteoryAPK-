package com.meteory.optimizer.screens.performance

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meteory.optimizer.ui.components.*
import com.meteory.optimizer.ui.theme.*
import com.meteory.optimizer.viewmodels.PerformanceViewModel

@Composable
fun PerformanceScreen(vm: PerformanceViewModel = hiltViewModel()) {
    val state = vm.state.collectAsState().value

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
            "CPU / RAM / Sistema",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        // CPU + RAM gauges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            CircularProgressGauge(
                percent = state.cpuPercent,
                label   = "CPU ${state.cpuFreqMhz}MHz",
                color   = Primary400,
                size    = 100.dp
            )
            CircularProgressGauge(
                percent = state.ramInfo.usedPercent,
                label   = "RAM ${state.ramInfo.usedMb}/${state.ramInfo.totalMb}MB",
                color   = Warning400,
                size    = 100.dp
            )
            CircularProgressGauge(
                percent = state.tempC.toInt().coerceAtMost(100),
                label   = "Temp ${"%.1f".format(state.tempC)}°C",
                color   = when {
                    state.tempC < 40f -> TempCool
                    state.tempC < 55f -> TempWarm
                    state.tempC < 70f -> TempHot
                    else              -> TempCritic
                },
                centerText = "${state.tempC.toInt()}°C",
                size    = 100.dp
            )
        }

        // CPU Sparkline
        SectionHeader("Historial CPU")
        SparkLineChart(
            values   = state.cpuHistory,
            color    = Primary400,
            modifier = Modifier.fillMaxWidth()
        )

        // RAM Sparkline
        SectionHeader("Historial RAM")
        SparkLineChart(
            values   = state.ramHistory,
            color    = Warning400,
            modifier = Modifier.fillMaxWidth()
        )

        // Profiles
        SectionHeader("Perfil de Rendimiento")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.profiles.forEach { (key, label) ->
                FilterChip(
                    selected = state.profile == key,
                    onClick  = { vm.applyProfile(key) },
                    label    = { Text(label) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary400.copy(0.2f),
                        selectedLabelColor     = Primary400
                    )
                )
            }
        }

        // Actions
        SectionHeader("Acciones")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlowButton(
                text     = if (state.isKillingBg) "Limpiando..." else "Matar BG",
                onClick  = vm::killBackgroundApps,
                enabled  = !state.isKillingBg,
                modifier = Modifier.weight(1f),
                color    = Error400
            )
            GlowButton(
                text     = "Liberar RAM",
                onClick  = vm::freeRam,
                modifier = Modifier.weight(1f),
                color    = Primary400
            )
        }

        // System Health
        SectionHeader("Salud del Sistema")
        HealthScoreCard(score = state.systemScore)

        if (state.healthActions.isNotEmpty()) {
            state.healthActions.forEach { action ->
                HealthActionRow(action)
            }
        }

        // CPU Info
        SectionHeader("Info del CPU")
        CpuInfoCard(
            cores  = state.cpuCores,
            curMhz = state.cpuFreqMhz,
            maxMhz = state.cpuMaxMhz
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HealthScoreCard(score: Int) {
    val color = when {
        score >= 80 -> Success400
        score >= 60 -> Warning400
        else        -> Error400
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text("Puntuación Sistema", color = Neutral400, style = MaterialTheme.typography.labelMedium)
            Text("$score / 100", color = color, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        }
        MeteoryLinearProgress(
            progress = score / 100f,
            color    = color,
            modifier = Modifier.width(100.dp)
        )
    }
}

@Composable
private fun HealthActionRow(action: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .border(1.dp, Neutral700, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (action.startsWith("✅")) Icons.Default.CheckCircle else Icons.Default.Warning,
            null,
            tint     = if (action.startsWith("✅")) Success400 else Warning400,
            modifier = Modifier.size(16.dp)
        )
        Text(action, color = Neutral300, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CpuInfoCard(cores: Int, curMhz: Int, maxMhz: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, Neutral700, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CpuInfoRow("Núcleos", "$cores")
        CpuInfoRow("Frecuencia actual", "${curMhz} MHz")
        CpuInfoRow("Frecuencia máxima", "${maxMhz} MHz")
    }
}

@Composable
private fun CpuInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Neutral400, style = MaterialTheme.typography.bodySmall)
        Text(value, color = Neutral200, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
    }
}
