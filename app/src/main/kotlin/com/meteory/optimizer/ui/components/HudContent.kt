package com.meteory.optimizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meteory.optimizer.ui.theme.*

@Composable
fun HudContent(
    fps: String,
    cpu: String,
    ram: String,
    temp: String,
    ping: String,
    opacity: Float,
    visible: Boolean,
    onToggle: () -> Unit
) {
    if (!visible) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(HudBackground)
        ) {
            Icon(
                imageVector        = Icons.Default.VisibilityOff,
                contentDescription = "Mostrar HUD",
                tint               = Primary400,
                modifier           = Modifier.size(16.dp)
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(HudBackground.copy(alpha = opacity))
            .border(1.dp, HudBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Text(
                    text     = "METEORY",
                    fontSize = 8.sp,
                    color    = Primary400,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                IconButton(
                    onClick  = onToggle,
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.VisibilityOff,
                        contentDescription = "Ocultar",
                        tint               = Neutral400,
                        modifier           = Modifier.size(10.dp)
                    )
                }
            }
            HudRow("FPS",  fps,  Accent500)
            HudRow("CPU",  cpu,  Primary400)
            HudRow("RAM",  ram,  Warning400)
            HudRow("TEMP", temp, tempColor(temp))
            HudRow("PING", ping, Success400)
        }
    }
}

@Composable
private fun HudRow(label: String, value: String, color: Color) {
    Row(
        modifier              = Modifier.width(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text       = label,
            fontSize   = 9.sp,
            color      = Neutral400,
            fontFamily = FontFamily.Monospace,
            modifier   = Modifier.width(32.dp)
        )
        Text(
            text       = value,
            fontSize   = 10.sp,
            color      = color,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun tempColor(temp: String): Color {
    val v = temp.replace("°C", "").trim().toFloatOrNull() ?: 0f
    return when {
        v < 40f -> TempCool
        v < 55f -> TempWarm
        v < 70f -> TempHot
        else    -> TempCritic
    }
}
