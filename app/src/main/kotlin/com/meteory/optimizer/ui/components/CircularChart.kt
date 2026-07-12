package com.meteory.optimizer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meteory.optimizer.ui.theme.*

@Composable
fun CircularProgressGauge(
    percent: Int,
    label: String,
    color: Color = Primary400,
    trackColor: Color = Neutral700,
    size: Dp = 100.dp,
    strokeWidth: Dp = 8.dp,
    centerText: String = "$percent%"
) {
    val animatedSweep by animateFloatAsState(
        targetValue   = percent / 100f * 360f,
        animationSpec = tween(700, easing = EaseInOutCubic),
        label         = "sweep"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier        = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke     = strokeWidth.toPx()
                val inset      = stroke / 2f
                val arcSize    = Size(this.size.width - stroke, this.size.height - stroke)
                val topLeft    = Offset(inset, inset)

                drawArc(
                    color      = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color      = color,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            Text(
                text  = centerText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                fontSize = (size.value / 5).sp
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = Neutral400
        )
    }
}

@Composable
fun StoragePieChart(
    segments: List<Pair<String, Float>>,
    colors: List<Color> = listOf(Primary400, Accent500, Warning400, Error400, Secondary300),
    size: Dp = 160.dp
) {
    val total = segments.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)
    var startAngle = -90f

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = (size / 4).toPx()
            val inset  = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)

            segments.forEachIndexed { i, (_, value) ->
                val sweep = value / total * 360f
                drawArc(
                    color      = colors[i % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke((size / 4).toPx(), cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
    }
}

@Composable
fun SparkLineChart(
    values: List<Int>,
    color: Color = Primary400,
    modifier: Modifier = Modifier
) {
    if (values.size < 2) return

    val max = values.max().toFloat().coerceAtLeast(1f)
    val min = values.min().toFloat()

    Canvas(modifier = modifier.height(48.dp)) {
        val step   = this.size.width / (values.size - 1)
        val points = values.mapIndexed { i, v ->
            Offset(
                x = i * step,
                y = this.size.height * (1f - (v - min) / (max - min).coerceAtLeast(1f))
            )
        }
        for (i in 0 until points.size - 1) {
            drawLine(
                color       = color,
                start       = points[i],
                end         = points[i + 1],
                strokeWidth = 2.dp.toPx(),
                cap         = StrokeCap.Round
            )
        }
        drawCircle(color = color, radius = 4.dp.toPx(), center = points.last())
    }
}
