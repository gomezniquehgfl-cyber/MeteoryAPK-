package com.meteory.optimizer.screens.gaming

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meteory.optimizer.services.HudOverlayService
import com.meteory.optimizer.ui.components.*
import com.meteory.optimizer.ui.theme.*
import com.meteory.optimizer.utils.PermissionUtils
import com.meteory.optimizer.viewmodels.GamingViewModel

@Composable
fun GamingScreen(vm: GamingViewModel = hiltViewModel()) {
    val state   = vm.state.collectAsState().value
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.scanInstalledApps() }

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
            "Modo Gaming Brutal",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        // Live metrics strip
        LiveMetricsStrip(cpu = state.currentCpu, temp = state.currentTemp)

        // Main toggle
        GamingModeToggle(
            isOn    = state.isGamingModeOn,
            onToggle = vm::toggleGamingMode
        )

        // Renderer
        SectionHeader("Motor de Renderizado")
        RendererSelector(
            current    = state.renderer,
            isTesting  = state.isTestingRenderer,
            testResult = state.rendererTestResult,
            onSelect   = vm::setRenderer,
            onAuto     = vm::autoSelectRenderer
        )

        // Refresh rate
        SectionHeader("Tasa de Refresco")
        RefreshRateSelector(
            current = state.refreshHz,
            options = vm.availableHz,
            onSelect = vm::setRefreshHz
        )

        // Options
        SectionHeader("Opciones de Juego")
        ToggleRow("No Molestar (DND)", state.dndEnabled, Icons.Default.NotificationsOff, vm::toggleDnd)
        ToggleRow("HUD Superpuesto", state.hudEnabled, Icons.Default.Visibility, onToggle = { on ->
            vm.toggleHud(on)
            if (on && PermissionUtils.hasOverlayPermission(context)) {
                HudOverlayService.start(context)
            } else if (!on) {
                HudOverlayService.stop(context)
            } else {
                PermissionUtils.openOverlaySettings(context)
            }
        })
        ToggleRow("Matar Procesos BG", state.backgroundKill, Icons.Default.Close, vm::toggleBgKill)

        // Game profiles
        if (state.gameProfiles.isNotEmpty()) {
            SectionHeader("Perfiles Guardados")
            state.gameProfiles.forEach { profile ->
                GameProfileRow(
                    name      = profile.appName,
                    renderer  = profile.renderer,
                    hz        = profile.refreshHz,
                    onDelete  = { vm.deleteProfile(profile) }
                )
            }
        }

        // App list
        SectionHeader(
            "Apps Instaladas",
            action = {
                if (state.isScanningApps) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Primary400)
                }
            }
        )
        state.installedApps.take(12).forEach { app ->
            AppRow(
                name         = app.name,
                hasProfile   = app.hasGameProfile,
                isSelected   = state.selectedApp?.packageName == app.packageName,
                onSelect     = { vm.selectApp(app) },
                onSaveProfile = { vm.saveGameProfile(app.packageName, app.name) }
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LiveMetricsStrip(cpu: Int, temp: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, Neutral700, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        MetricPill("CPU", "$cpu%", Primary400)
        MetricPill("TEMP", "${"%.1f".format(temp)}°C", when {
            temp < 40f -> TempCool; temp < 55f -> TempWarm; temp < 70f -> TempHot; else -> TempCritic
        })
    }
}

@Composable
private fun MetricPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Neutral400)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
private fun GamingModeToggle(isOn: Boolean, onToggle: () -> Unit) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOn) Accent500.copy(0.12f) else SurfaceCard
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isOn) Accent500.copy(0.5f) else Neutral700
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Modo Gaming Brutal",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isOn) Accent500 else Color.White
                )
                Text(
                    if (isOn) "CPU/GPU al máximo — Activo" else "Toca para activar recursos totales",
                    style = MaterialTheme.typography.bodySmall,
                    color = Neutral400
                )
            }
            Switch(
                checked         = isOn,
                onCheckedChange = { onToggle() },
                colors          = SwitchDefaults.colors(
                    checkedThumbColor  = Neutral950,
                    checkedTrackColor  = Accent500,
                    uncheckedTrackColor = Neutral700
                )
            )
        }
    }
}

@Composable
private fun RendererSelector(
    current: String,
    isTesting: Boolean,
    testResult: String,
    onSelect: (String) -> Unit,
    onAuto: () -> Unit
) {
    val options = listOf("skiavk" to "Vulkan", "opengles" to "OpenGL ES", "auto" to "Auto")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = current == key,
                onClick  = { onSelect(key) },
                label    = { Text(label, fontSize = 13.sp) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary400.copy(0.2f),
                    selectedLabelColor     = Primary400
                )
            )
        }
    }
    if (testResult.isNotBlank()) {
        Text(testResult, style = MaterialTheme.typography.bodySmall, color = Neutral300)
    }
    if (isTesting) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Primary400)
    } else {
        OutlinedButton(
            onClick = onAuto,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(12.dp),
            border   = BorderStroke(1.dp, Primary400.copy(0.5f))
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = Primary400, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Selección Automática (30s test)", color = Primary400)
        }
    }
}

@Composable
private fun RefreshRateSelector(current: Int, options: List<Int>, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { hz ->
            FilterChip(
                selected = current == hz,
                onClick  = { onSelect(hz) },
                label    = { Text("${hz}Hz", fontSize = 13.sp) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Warning400.copy(0.2f),
                    selectedLabelColor     = Warning400
                )
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, Neutral700, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = Primary400, modifier = Modifier.size(20.dp))
            Text(label, color = Neutral200, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedTrackColor = Primary400,
                uncheckedTrackColor = Neutral700
            )
        )
    }
}

@Composable
private fun GameProfileRow(name: String, renderer: String, hz: Int, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, Neutral700, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(name, color = Neutral100, style = MaterialTheme.typography.bodyMedium)
            Text("$renderer · ${hz}Hz", color = Neutral400, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, null, tint = Error400, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AppRow(
    name: String,
    hasProfile: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onSaveProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Primary400.copy(0.08f) else SurfaceCard)
            .border(1.dp, if (isSelected) Primary400.copy(0.4f) else Neutral700, RoundedCornerShape(10.dp))
            .clickable { onSelect() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.SportsEsports, null, tint = if (hasProfile) Accent500 else Neutral600, modifier = Modifier.size(18.dp))
            Text(name, color = Neutral200, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        if (isSelected) {
            TextButton(onClick = onSaveProfile) {
                Text("Guardar perfil", color = Primary400, fontSize = 12.sp)
            }
        }
    }
}
