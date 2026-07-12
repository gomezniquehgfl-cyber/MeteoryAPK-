package com.meteory.optimizer.screens.ai

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.unit.dp
import com.meteory.optimizer.ui.components.*
import com.meteory.optimizer.ui.theme.*
import com.meteory.optimizer.utils.SystemInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class AiRecommendation(val title: String, val detail: String, val priority: String)

@Composable
fun AiScreen() {
    val context = LocalContext.current
    var recommendations by remember { mutableStateOf<List<AiRecommendation>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(true) }
    var systemScore by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (isActive) {
            val cpu    = SystemInfo.getCpuUsagePercent()
            val ram    = SystemInfo.getRamInfo(context)
            val temp   = SystemInfo.getCpuTemperature()
            val battery = SystemInfo.getBatteryInfo(context)
            val score  = SystemInfo.computeSystemScore(context)
            systemScore = score

            val recs = mutableListOf<AiRecommendation>()

            if (cpu > 70)
                recs += AiRecommendation("CPU sobrecargado", "Usa el perfil 'Equilibrado' y cierra apps en segundo plano.", "Alta")
            if (ram.usedPercent > 80)
                recs += AiRecommendation("RAM al ${ram.usedPercent}%", "Libera RAM ahora. Solo ${ram.availableMb}MB disponible.", "Alta")
            if (temp > 55f)
                recs += AiRecommendation("Temperatura crítica ${temp.toInt()}°C", "Detén juegos 5 min. Activa control térmico.", "Alta")
            if (battery.level < 20 && !battery.isCharging)
                recs += AiRecommendation("Batería baja ${battery.level}%", "Activa modo Ultra Ahorro de energía.", "Media")
            if (cpu < 30 && ram.usedPercent < 50)
                recs += AiRecommendation("Sistema optimizado", "Todo en orden. Rendimiento ${score}/100.", "Info")
            if (battery.temperatureC > 40f)
                recs += AiRecommendation("Batería caliente ${battery.temperatureC.toInt()}°C", "Evita carga rápida. Desconecta cargador si > 43°C.", "Media")

            if (recs.isEmpty())
                recs += AiRecommendation("Rendimiento óptimo", "La IA no detecta problemas. Puntuación: ${score}/100.", "Info")

            recommendations = recs
            isAnalyzing = false
            delay(15_000)
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AutoAwesome, null, tint = Accent500, modifier = Modifier.size(28.dp))
            Text(
                "Inteligencia Artificial",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        if (isAnalyzing) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Accent500)
                    Spacer(Modifier.height(8.dp))
                    Text("Analizando sistema con IA...", color = Neutral400)
                }
            }
        } else {
            // Score
            Card(
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, Accent500.copy(0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Análisis IA", color = Neutral400, style = MaterialTheme.typography.labelMedium)
                        Text("$systemScore / 100", color = Accent500,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Actualiza cada 15 segundos", color = Neutral500, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.Psychology, null, tint = Accent500, modifier = Modifier.size(40.dp))
                }
            }

            SectionHeader("Recomendaciones (${recommendations.size})")

            recommendations.forEach { rec ->
                AiRecommendationCard(rec)
            }

            // AI habits summary
            SectionHeader("Hábitos Detectados")
            HabitRow("Horario de uso", "09:00 – 22:00 (estimado)", Icons.Default.Schedule)
            HabitRow("Uso promedio CPU", "~35% en reposo", Icons.Default.Memory)
            HabitRow("Pico de temperatura", "Gaming / mediodía", Icons.Default.WbSunny)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AiRecommendationCard(rec: AiRecommendation) {
    val (border, icon, textColor) = when (rec.priority) {
        "Alta"  -> Triple(Error400, Icons.Default.Error, Error400)
        "Media" -> Triple(Warning400, Icons.Default.Warning, Warning400)
        else    -> Triple(Success400, Icons.Default.CheckCircle, Success400)
    }
    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, border.copy(0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(20.dp).padding(top = 2.dp))
            Column {
                Text(rec.title, color = Neutral100, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(rec.detail, color = Neutral400, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun HabitRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
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
        Icon(icon, null, tint = Accent500.copy(0.8f), modifier = Modifier.size(18.dp))
        Column {
            Text(label, color = Neutral400, style = MaterialTheme.typography.labelSmall)
            Text(value, color = Neutral200, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
