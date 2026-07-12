package com.meteory.optimizer.screens.privacy

import android.content.pm.PackageManager
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

data class PermissionAlert(val appName: String, val permission: String, val risk: String)

@Composable
fun PrivacyScreen() {
    val context = LocalContext.current
    var alerts by remember { mutableStateOf<List<PermissionAlert>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isScanning = true
        val pm = context.packageManager
        val found = mutableListOf<PermissionAlert>()
        try {
            val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            packages.forEach { pkg ->
                if (pkg.requestedPermissions == null) return@forEach
                if (pkg.applicationInfo?.flags?.and(android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) return@forEach
                val appName = pm.getApplicationLabel(pkg.applicationInfo!!).toString()
                pkg.requestedPermissions?.forEach { perm ->
                    when {
                        perm.contains("FINE_LOCATION") ->
                            found += PermissionAlert(appName, "Ubicación precisa", "Alto")
                        perm.contains("RECORD_AUDIO") ->
                            found += PermissionAlert(appName, "Micrófono", "Alto")
                        perm.contains("CAMERA") ->
                            found += PermissionAlert(appName, "Cámara", "Medio")
                        perm.contains("READ_CONTACTS") ->
                            found += PermissionAlert(appName, "Contactos", "Medio")
                    }
                }
            }
        } catch (e: Exception) { /* non-fatal */ }
        alerts = found.take(30)
        isScanning = false
        scanned = true
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
            "Privacidad & Seguridad",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        if (isScanning) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Primary400)
                    Spacer(Modifier.height(8.dp))
                    Text("Escaneando permisos...", color = Neutral400)
                }
            }
        }

        if (scanned) {
            PrivacySummaryCard(alertCount = alerts.size)
            if (alerts.isEmpty()) {
                Card(
                    shape  = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Success500.copy(0.12f)),
                    border = BorderStroke(1.dp, Success500.copy(0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Success400)
                        Spacer(Modifier.width(12.dp))
                        Text("No se encontraron permisos sospechosos", color = Success400)
                    }
                }
            } else {
                SectionHeader("Permisos Detectados (${alerts.size})")
                alerts.forEach { alert ->
                    PermissionAlertRow(alert)
                }
            }
        }

        SectionHeader("Herramientas de Privacidad")
        PrivacyTool("Borrar portapapeles", Icons.Default.ContentPaste, "Elimina datos copiados") {}
        PrivacyTool("Limpiar historial reciente", Icons.Default.History, "Búsquedas y accesos") {}
        PrivacyTool("Bóveda privada", Icons.Default.Lock, "Cifrado AES-256, protección biométrica") {}
        PrivacyTool("Borrar temporales", Icons.Default.DeleteSweep, "Archivos temp del sistema") {}

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PrivacySummaryCard(alertCount: Int) {
    val (color, label) = when {
        alertCount == 0  -> Success400 to "Sin riesgos detectados"
        alertCount < 10  -> Warning400 to "$alertCount permisos a revisar"
        else             -> Error400   to "$alertCount permisos de riesgo"
    }
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, color.copy(0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Escáner de Permisos", color = Neutral400, style = MaterialTheme.typography.labelMedium)
                Text(label, color = color, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }
            Icon(if (alertCount == 0) Icons.Default.Shield else Icons.Default.Warning, null, tint = color, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun PermissionAlertRow(alert: PermissionAlert) {
    val riskColor = if (alert.risk == "Alto") Error400 else Warning400
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, riskColor.copy(0.3f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(alert.appName, color = Neutral100, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(alert.permission, color = riskColor, style = MaterialTheme.typography.bodySmall)
        }
        Badge(containerColor = riskColor.copy(0.2f)) {
            Text(alert.risk, color = riskColor, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PrivacyTool(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape   = RoundedCornerShape(12.dp),
        colors  = CardDefaults.cardColors(containerColor = SurfaceCard),
        border  = BorderStroke(1.dp, Neutral700),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = Primary400, modifier = Modifier.size(22.dp))
            Column {
                Text(title, color = Neutral200, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, color = Neutral500, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Neutral600)
        }
    }
}
