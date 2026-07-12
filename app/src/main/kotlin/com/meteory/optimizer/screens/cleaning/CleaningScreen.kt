package com.meteory.optimizer.screens.cleaning

import androidx.compose.animation.*
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meteory.optimizer.ui.components.*
import com.meteory.optimizer.ui.theme.*
import com.meteory.optimizer.viewmodels.CleaningViewModel

@Composable
fun CleaningScreen(vm: CleaningViewModel = hiltViewModel()) {
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
            "Limpieza & Almacenamiento",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        // Storage overview
        StorageOverviewCard(
            totalGb  = state.storageTotalGb,
            freeGb   = state.storageFreeGb,
            usedPct  = state.storageUsedPct
        )

        // Success banner
        AnimatedVisibility(visible = state.showSuccess) {
            Card(
                shape  = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Success500.copy(0.15f)),
                border = BorderStroke(1.dp, Success500.copy(0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("✅ Limpieza completada", color = Success400, fontWeight = FontWeight.SemiBold)
                        Text("${state.lastFreedMb} MB liberados", color = Neutral300, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = vm::dismissSuccess) {
                        Text("OK", color = Success400)
                    }
                }
            }
        }

        // Scan button
        if (state.categories.isEmpty() && !state.isScanning && !state.showSuccess) {
            GlowButton(
                text     = "Escanear Basura",
                onClick  = vm::scan,
                modifier = Modifier.fillMaxWidth(),
                color    = Primary400
            )
        }

        if (state.isScanning) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(color = Primary400)
                Spacer(Modifier.height(8.dp))
                Text("Analizando sistema...", color = Neutral400)
            }
        }

        // Categories
        if (state.categories.isNotEmpty()) {
            SectionHeader("Archivos Encontrados")
            Text(
                "Total: ${state.totalJunkMb} MB",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Accent500
            )

            state.categories.forEachIndexed { idx, cat ->
                CleanCategoryRow(
                    name     = cat.name,
                    sizeMb   = cat.sizeMb,
                    selected = cat.isSelected,
                    onToggle = { vm.toggleCategory(idx) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick  = vm::scan,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.dp, Neutral600)
                ) {
                    Text("Re-escanear", color = Neutral300)
                }
                GlowButton(
                    text     = if (state.isCleaning) "Limpiando..." else "Limpiar Ahora",
                    onClick  = vm::clean,
                    enabled  = !state.isCleaning,
                    modifier = Modifier.weight(1f),
                    color    = Accent500
                )
            }

            if (state.isCleaning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color    = Accent500,
                    trackColor = Neutral700
                )
            }
        }

        // Auto clean threshold
        SectionHeader("Limpieza Automática")
        AutoCleanConfig(
            threshold = state.autoThreshold,
            onChange  = vm::setAutoThreshold
        )

        // Total freed stats
        if (state.totalFreedAllTime > 0L) {
            StatBanner(
                label = "Total liberado (histórico)",
                value = "${state.totalFreedAllTime} MB"
            )
        }

        // History
        if (state.history.isNotEmpty()) {
            SectionHeader("Historial de Limpiezas")
            state.history.take(5).forEach { entry ->
                HistoryRow(
                    freedMb   = entry.freedMb,
                    type      = entry.type,
                    timestamp = entry.timestamp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StorageOverviewCard(totalGb: Float, freeGb: Float, usedPct: Int) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, Neutral700),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Almacenamiento", style = MaterialTheme.typography.labelMedium, color = Neutral400)
                    Text("${"%.1f".format(freeGb)} GB libres", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text("de ${"%.1f".format(totalGb)} GB totales", style = MaterialTheme.typography.bodySmall, color = Neutral400)
                }
                CircularProgressGauge(
                    percent = usedPct,
                    label   = "Usado",
                    color   = when {
                        usedPct < 70 -> Success400
                        usedPct < 85 -> Warning400
                        else         -> Error400
                    },
                    size    = 80.dp
                )
            }
            MeteoryLinearProgress(
                progress = usedPct / 100f,
                color    = when {
                    usedPct < 70 -> Success400
                    usedPct < 85 -> Warning400
                    else         -> Error400
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CleanCategoryRow(name: String, sizeMb: Long, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, if (selected) Primary400.copy(0.3f) else Neutral700, RoundedCornerShape(10.dp))
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Checkbox(
                checked         = selected,
                onCheckedChange = { onToggle() },
                colors          = CheckboxDefaults.colors(
                    checkedColor        = Primary400,
                    uncheckedColor      = Neutral600,
                    checkmarkColor      = Neutral950
                )
            )
            Text(name, color = Neutral200, style = MaterialTheme.typography.bodyMedium)
        }
        Text("$sizeMb MB", color = Accent500, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun AutoCleanConfig(threshold: Int, onChange: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, Neutral700, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Limpiar cuando >", color = Neutral300, style = MaterialTheme.typography.bodyMedium)
            Text("$threshold%", color = Primary400, fontWeight = FontWeight.Bold)
        }
        Slider(
            value         = threshold.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange    = 70f..95f,
            steps         = 4,
            colors        = SliderDefaults.colors(
                thumbColor       = Primary400,
                activeTrackColor = Primary400,
                inactiveTrackColor = Neutral700
            )
        )
        Text("Umbral actual: $threshold%", color = Neutral400, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StatBanner(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Accent500.copy(0.08f))
            .border(1.dp, Accent500.copy(0.3f), RoundedCornerShape(10.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Neutral300, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Accent500, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun HistoryRow(freedMb: Long, type: String, timestamp: Long) {
    val date = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(timestamp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .border(1.dp, Neutral700, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$freedMb MB · $type", color = Neutral200, style = MaterialTheme.typography.bodySmall)
        Text(date, color = Neutral500, style = MaterialTheme.typography.bodySmall)
    }
}
