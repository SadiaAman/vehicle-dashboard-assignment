package com.example.vehicledashboard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Battery level, 0-100 %.
 *
 * The assignment asks for a progress indicator, so the value is shown three
 * ways for redundancy: the number, a drawn battery that fills up, and a bar.
 * Colour changes with the level - a driver should notice "low" without reading.
 */
@Composable
fun BatteryCard(
    batteryPercent: Int,
    modifier: Modifier = Modifier,
) {
    val safePercent = batteryPercent.coerceIn(0, 100)
    val animatedPercent by animateFloatAsState(
        targetValue = safePercent.toFloat(),
        animationSpec = tween(durationMillis = 950),
        label = "battery",
    )

    val level = animatedPercent / 100f
    val batteryColor = when {
        safePercent <= 15 -> MaterialTheme.colorScheme.error
        safePercent <= 35 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant

    DashboardCard(modifier = modifier, title = "BATTERY") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = safePercent.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )
            }

            Canvas(modifier = Modifier.size(width = 96.dp, height = 48.dp)) {
                drawBattery(level = level, fillColor = batteryColor, outlineColor = outlineColor)
            }
        }

        LinearProgressIndicator(
            progress = { level },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(12.dp),
            color = batteryColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

/** Battery outline with a terminal on the right and a fill that follows the level. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBattery(
    level: Float,
    fillColor: Color,
    outlineColor: Color,
) {
    val terminalWidth = size.width * 0.07f
    val bodyWidth = size.width - terminalWidth - 4.dp.toPx()
    val outline = 3.dp.toPx()
    val corner = CornerRadius(8.dp.toPx(), 8.dp.toPx())

    drawRoundRect(
        color = outlineColor,
        topLeft = Offset(0f, 0f),
        size = Size(bodyWidth, size.height),
        cornerRadius = corner,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = outline),
    )

    // Positive terminal.
    drawRoundRect(
        color = outlineColor,
        topLeft = Offset(bodyWidth + 4.dp.toPx(), size.height * 0.3f),
        size = Size(terminalWidth, size.height * 0.4f),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
    )

    // Inner fill, inset so it never overlaps the outline.
    val inset = outline + 3.dp.toPx()
    val fillWidth = (bodyWidth - inset * 2) * level.coerceIn(0f, 1f)
    if (fillWidth > 0f) {
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(inset, inset),
            size = Size(fillWidth, size.height - inset * 2),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
    }
}
