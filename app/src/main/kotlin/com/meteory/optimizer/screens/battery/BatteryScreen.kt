package com.meteory.optimizer.screens.battery

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
import com.meteory.optimizer.viewmodels.BatteryViewModel

@Composable
fun BatteryScreen(vm: BatteryViewModel = hiltViewModel()) {
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
            "Batería & Energía",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        // Big battery indicator
        BatteryHeroCard(
            level      = state.level,
            isCharging = state.isCharging,
            chargeType = state.chargeType,
            health     = state.health,
            estMinutes = state.estimatedMinutes
        )

        // Details grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoTile("Temperatura", "${"%.1f".format(state.tempC)}°C",
                when { state.tempC < 35f -> TempCool; state.tempC < 45f -> TempWarm; else -> TempCritic },
                Modifier.weight(1f))
            InfoTile("Voltaje", "${"%.2f".format(state.voltageV)}V", Primary300, Modifier.weight(1f))
            InfoTile("Capacidad", "${state.capacityMah}mAh", Accent500, Modifier.weight(1f))
        }

        // Modes
        SectionHeader("Modo de Energía")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            vm.batteryModes.forEach { (key, label) ->
                FilterChip(
                    selected = state.batteryMode == key,
                    onClick  = { vm.setBatteryMode(key) },
                    label    = { Text(label) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Success400.copy(0.2f),
                        selectedLabelColor     = Success400
                    )
                )
            }
        }

        // Battery protection
        SectionHeader("Protección de Carga")
        BatteryProtectionCard(
            enabled  = state.protectEnabled,
            pct      = state.protectPct,
            onToggle = vm::setBatteryProtect,
            onPctChange = vm::setBatteryProtectPct
        )

        // History chart
        if (state.levelHistory.isNotEmpty()) {
            SectionHeader("Historial de Nivel")
            SparkLineChart(
                values   = state.levelHistory.map { it.level },
                color    = Success400,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BatteryHeroCard(
    level: Int,
    isCharging: Boolean,
    chargeType: String,
    health: String,
    estMinutes: Int
) {
    val batteryColor = when {
        level > 60 -> Success400
        level > 30 -> Warning400
        else       -> Error400
    }
    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, batteryColor.copy(0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Nivel de Batería", color = Neutral400, style = MaterialTheme.typography.labelMedium)
                Text("$level%", color = batteryColor,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    if (isCharging) "⚡ $chargeType" else "🔋 Descargando",
                    color = Neutral300,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    if (isCharging) "Completa en ~${estMinutes}min" else "Restante ~${estMinutes}min",
                    color = Neutral400,
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Salud: $health", color = when(health) {
                    "Buena" -> Success400; "Sobrecalentada" -> TempHot; else -> Warning400
                }, style = MaterialTheme.typography.labelSmall)
            }
            CircularProgressGauge(
                percent    = level,
                label      = if (isCharging) "Cargando" else "Batería",
                color      = batteryColor,
                size       = 90.dp
            )
        }
        MeteoryLinearProgress(
            progress = level / 100f,
            color    = batteryColor,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun InfoTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = Neutral400, style = MaterialTheme.typography.labelSmall)
        Text(value, color = color, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun BatteryProtectionCard(
    enabled: Boolean,
    pct: Int,
    onToggle: (Boolean) -> Unit,
    onPctChange: (Int) -> Unit
) {
    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, if (enabled) Success400.copy(0.4f) else Neutral700),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Limitar carga al $pct%", color = Neutral200, fontWeight = FontWeight.SemiBold)
                    Text("Protege los ciclos de batería", color = Neutral400, style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked         = enabled,
                    onCheckedChange = onToggle,
                    colors          = SwitchDefaults.colors(
                        checkedTrackColor = Success400,
                        uncheckedTrackColor = Neutral700
                    )
                )
            }
            if (enabled) {
                Slider(
                    value         = pct.toFloat(),
                    onValueChange = { onPctChange(it.toInt()) },
                    valueRange    = 60f..100f,
                    colors        = SliderDefaults.colors(
                        thumbColor = Success400,
                        activeTrackColor = Success400,
                        inactiveTrackColor = Neutral700
                    )
                )
            }
        }
    }
}
