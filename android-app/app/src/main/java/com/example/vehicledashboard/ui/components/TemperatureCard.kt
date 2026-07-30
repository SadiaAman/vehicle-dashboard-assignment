package com.example.vehicledashboard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private const val MIN_TEMPERATURE = -20.0
private const val MAX_TEMPERATURE = 50.0

/**
 * Outside temperature, −20 °C to +50 °C.
 *
 * The drawn thermometer fills in proportion to where the reading sits in that
 * range, and its colour shifts from cold blue to warm amber, so the rough value
 * is readable before the digits are.
 */
@Composable
fun TemperatureCard(
    temperatureC: Double,
    modifier: Modifier = Modifier,
) {
    val clamped = temperatureC.coerceIn(MIN_TEMPERATURE, MAX_TEMPERATURE)
    val level = ((clamped - MIN_TEMPERATURE) / (MAX_TEMPERATURE - MIN_TEMPERATURE)).toFloat()

    val cold = MaterialTheme.colorScheme.primary
    val warm = MaterialTheme.colorScheme.tertiary
    // Below 10 °C reads as cold, above 25 °C as warm; in between it blends.
    val temperatureColor = when {
        clamped <= 10.0 -> cold
        clamped >= 25.0 -> warm
        else -> lerpColor(cold, warm, ((clamped - 10.0) / 15.0).toFloat())
    }
    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant

    DashboardCard(modifier = modifier, title = "OUTSIDE") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    // One decimal would add noise; the driver needs the rough value.
                    text = clamped.roundToInt().toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "°C",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                )
            }

            Canvas(modifier = Modifier.size(width = 44.dp, height = 84.dp)) {
                drawThermometer(level = level, fillColor = temperatureColor, outlineColor = outlineColor)
            }
        }

        Text(
            text = "Range −20 °C to 50 °C",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/** A classic thermometer: bulb at the bottom, tube above, fill rising from the bulb. */
private fun DrawScope.drawThermometer(
    level: Float,
    fillColor: Color,
    outlineColor: Color,
) {
    val stroke = 3.dp.toPx()
    val bulbRadius = size.width * 0.36f
    val bulbCenter = Offset(size.width / 2f, size.height - bulbRadius)
    val tubeWidth = size.width * 0.34f
    val tubeTop = stroke
    val tubeBottom = bulbCenter.y

    drawRoundRect(
        color = outlineColor,
        topLeft = Offset(size.width / 2f - tubeWidth / 2f, tubeTop),
        size = Size(tubeWidth, tubeBottom - tubeTop),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(tubeWidth, tubeWidth),
        style = Stroke(width = stroke),
    )
    drawCircle(color = outlineColor, radius = bulbRadius, center = bulbCenter, style = Stroke(width = stroke))

    // The bulb is always full - it is the reservoir.
    drawCircle(color = fillColor, radius = bulbRadius - stroke * 1.6f, center = bulbCenter)

    val innerWidth = tubeWidth - stroke * 2.4f
    val innerTop = tubeTop + stroke
    val fillHeight = (tubeBottom - innerTop) * level.coerceIn(0f, 1f)
    if (fillHeight > 0f) {
        drawLine(
            color = fillColor,
            start = Offset(size.width / 2f, tubeBottom),
            end = Offset(size.width / 2f, tubeBottom - fillHeight),
            strokeWidth = innerWidth,
            cap = StrokeCap.Round,
        )
    }
}

/** Simple channel-wise blend; avoids pulling in an animation dependency for one colour. */
private fun lerpColor(from: Color, to: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * f,
        green = from.green + (to.green - from.green) * f,
        blue = from.blue + (to.blue - from.blue) * f,
        alpha = 1f,
    )
}
