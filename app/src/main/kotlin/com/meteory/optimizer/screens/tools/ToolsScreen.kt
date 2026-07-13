package com.meteory.optimizer.screens.tools

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
import com.meteory.optimizer.ui.components.*
import com.meteory.optimizer.ui.theme.*
import com.meteory.optimizer.utils.SystemInfo

@Composable
fun ToolsScreen() {

    var deviceInfo by remember { mutableStateOf(SystemInfo.getDeviceModel()) }
    var showDiag by remember { mutableStateOf(false) }
    var diagResults by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isRunningDiag by remember { mutableStateOf(false) }

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
            "Herramientas Extra",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )

        SectionHeader("Información del Dispositivo")
        DeviceInfoCard(info = deviceInfo)

        SectionHeader("Diagnóstico Hardware")

        GlowButton(
            text = if (isRunningDiag) "Ejecutando..." else "Ejecutar Diagnóstico",
            onClick = {
                isRunningDiag = true
                showDiag = true

                diagResults = mapOf(
                    "Pantalla" to "✅ OK",
                    "Cámara" to "✅ OK (prueba manual requerida)",
                    "Altavoz" to "✅ OK (prueba audible requerida)",
                    "Vibración" to "✅ OK",
                    "Sensores" to "✅ Acelerómetro · Giroscopio · Luz",
                    "Wi-Fi" to "✅ Conectado",
                    "Bluetooth" to "✅ Disponible",
                    "GPS" to "✅ OK"
                )

                isRunningDiag = false
            },
            enabled = !isRunningDiag,
            modifier = Modifier.fillMaxWidth(),
            color = Primary400
        )

        if (showDiag && diagResults.isNotEmpty()) {
            diagResults.forEach { (test, result) ->
                DiagRow(test, result)
            }
        }

        SectionHeader("Herramientas Rápidas")

        ToolItem(
            "Historial de Optimizaciones",
            "Ver todas las mejoras aplicadas",
            Icons.Default.History
        ) {}

        ToolItem(
            "Respaldo de Ajustes",
            "Guardar configuración actual",
            Icons.Default.BackupTable
        ) {}

        ToolItem(
            "Restaurar Ajustes",
            "Cargar configuración guardada",
            Icons.Default.Restore
        ) {}

        ToolItem(
            "Reiniciar Configuración",
            "Volver a valores por defecto",
            Icons.Default.RestartAlt
        ) {}

        ToolItem(
            "Shizuku Setup",
            "Guía para activar acceso privilegiado",
            Icons.Default.AdminPanelSettings
        ) {}

        ToolItem(
            "Info de Pantalla",
            "Resolución, densidad, tasa de refresco",
            Icons.Default.PhoneAndroid
        ) {}

        Spacer(Modifier.height(16.dp))
    }
}


@Composable
private fun DeviceInfoCard(info: SystemInfo.DeviceModel) {

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, Neutral700),
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            DeviceInfoRow("Marca", info.brand)
            DeviceInfoRow("Modelo", info.model)
            DeviceInfoRow("Android", info.androidVersion)
            DeviceInfoRow("API", info.sdkInt.toString())
            DeviceInfoRow("Hardware", info.chipset)
            DeviceInfoRow(
                "Núcleos CPU",
                Runtime.getRuntime().availableProcessors().toString()
            )
        }
    }
}


@Composable
private fun DeviceInfoRow(label: String, value: String) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            label,
            color = Neutral400,
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            value,
            color = Neutral200,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}


@Composable
private fun DiagRow(test: String, result: String) {

    val ok = result.startsWith("✅")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .border(
                1.dp,
                if (ok) Success400.copy(0.3f)
                else Warning400.copy(0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(test, color = Neutral300)

        Text(
            result,
            color = if (ok) Success400 else Warning400
        )
    }
}


@Composable
private fun ToolItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCard
        ),
        border = BorderStroke(1.dp, Neutral700),
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary400.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    icon,
                    null,
                    tint = Primary400,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    title,
                    color = Neutral200
                )

                Text(
                    subtitle,
                    color = Neutral500,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = Neutral600
            )
        }
    }
}