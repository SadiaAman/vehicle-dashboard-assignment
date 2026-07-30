package com.example.vehicledashboard.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vehicledashboard.domain.DrivingStatus
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MAX_SPEED = 250
private const val START_ANGLE = 135f  // 0 degrees is 3 o'clock; 135 is lower-left
private const val SWEEP_ANGLE = 270f  // three quarters of a circle

/**
 * The centrepiece: speed as an arc gauge, drawn with Compose Canvas.
 *
 * Two things make it readable while driving:
 * - the number is huge and pure white on near-black, so it is legible in a
 *   glance of well under a second
 * - the arc gives the same information *without reading*, by how full it is
 */
@Composable
fun SpeedGaugeCard(
    speedKmh: Int,
    drivingStatus: DrivingStatus,
    modifier: Modifier = Modifier,
) {
    // The backend is polled once per second. Animating between the readings
    // makes the needle sweep instead of jumping, which looks like a real
    // instrument. Linear easing over slightly less than the poll interval keeps
    // the motion continuous rather than "ease-in, wait, ease-in".
    val animatedSpeed by animateFloatAsState(
        targetValue = speedKmh.coerceIn(0, MAX_SPEED).toFloat(),
        animationSpec = tween(durationMillis = 950, easing = LinearEasing),
        label = "speed",
    )

    val textMeasurer = rememberTextMeasurer()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    DashboardCard(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 20.dp.toPx()
                val labelRoom = 44.dp.toPx()
                val radius = (minOf(size.width, size.height) / 2f) - stroke - labelRoom
                if (radius <= 0f) return@Canvas

                val center = Offset(size.width / 2f, size.height / 2f)
                val arcTopLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)

                drawRoadHint(center, radius, trackColor)

                // Full sweep in the muted colour: the scale that is always there.
                drawArc(
                    color = trackColor,
                    startAngle = START_ANGLE,
                    sweepAngle = SWEEP_ANGLE,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )

                // The filled part: current speed as a fraction of the scale.
                drawArc(
                    color = accentColor,
                    startAngle = START_ANGLE,
                    sweepAngle = SWEEP_ANGLE * (animatedSpeed / MAX_SPEED),
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )

                drawScaleTicksAndLabels(
                    center = center,
                    radius = radius,
                    stroke = stroke,
                    tickColor = labelColor,
                    labelColor = labelColor,
                    textMeasurer = textMeasurer,
                )
            }

            // The readable values sit on top of the canvas as normal text, so
            // they scale with the system font settings and stay crisp.
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = animatedSpeed.roundToInt().toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 116.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "km/h",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                )
                DrivingStatusPill(
                    drivingStatus = drivingStatus,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

/**
 * The coloured badge under the speed.
 *
 * Colour carries the meaning at a glance (green = moving, blue = charging,
 * grey = standing still) and the word confirms it.
 */
@Composable
private fun DrivingStatusPill(
    drivingStatus: DrivingStatus,
    modifier: Modifier = Modifier,
) {
    val color = when (drivingStatus) {
        DrivingStatus.DRIVING -> MaterialTheme.colorScheme.secondary
        DrivingStatus.CHARGING -> MaterialTheme.colorScheme.primary
        DrivingStatus.PARKED -> MaterialTheme.colorScheme.onSurfaceVariant
        DrivingStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .border(width = 2.dp, color = color, shape = RoundedCornerShape(50))
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text = drivingStatus.label,
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
    }
}

/** Major ticks with numbers every 50 km/h, minor ticks every 10. */
private fun DrawScope.drawScaleTicksAndLabels(
    center: Offset,
    radius: Float,
    stroke: Float,
    tickColor: Color,
    labelColor: Color,
    textMeasurer: TextMeasurer,
) {
    val outerEdge = radius + stroke / 2f
    val labelStyle = TextStyle(
        color = labelColor,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
    )

    var value = 0
    while (value <= MAX_SPEED) {
        val angleDegrees = START_ANGLE + SWEEP_ANGLE * (value / MAX_SPEED.toFloat())
        val angleRadians = Math.toRadians(angleDegrees.toDouble())
        val cosA = cos(angleRadians).toFloat()
        val sinA = sin(angleRadians).toFloat()

        val isMajor = value % 50 == 0
        val tickLength = if (isMajor) 14.dp.toPx() else 7.dp.toPx()
        val tickStart = outerEdge + 6.dp.toPx()

        drawLine(
            color = tickColor,
            start = Offset(center.x + cosA * tickStart, center.y + sinA * tickStart),
            end = Offset(
                center.x + cosA * (tickStart + tickLength),
                center.y + sinA * (tickStart + tickLength),
            ),
            strokeWidth = if (isMajor) 3.dp.toPx() else 1.5f.dp.toPx(),
            cap = StrokeCap.Round,
        )

        if (isMajor) {
            val labelRadius = tickStart + tickLength + 18.dp.toPx()
            val layout = textMeasurer.measure(text = value.toString(), style = labelStyle)
            // measure() gives the text box size, so it can be centred on the
            // point instead of hanging off its top-left corner.
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    center.x + cosA * labelRadius - layout.size.width / 2f,
                    center.y + sinA * labelRadius - layout.size.height / 2f,
                ),
            )
        }

        value += 10
    }
}

/**
 * A faint road perspective inside the gauge opening.
 *
 * Purely decorative and deliberately low-contrast: it must add depth without
 * drawing the eye away from the number.
 */
private fun DrawScope.drawRoadHint(center: Offset, radius: Float, color: Color) {
    val faint = color.copy(alpha = 0.55f)
    val top = center.y + radius * 0.45f
    val bottom = center.y + radius * 1.15f
    val spreadTop = radius * 0.16f
    val spreadBottom = radius * 0.85f

    drawLine(
        color = faint,
        start = Offset(center.x - spreadTop, top),
        end = Offset(center.x - spreadBottom, bottom),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = faint,
        start = Offset(center.x + spreadTop, top),
        end = Offset(center.x + spreadBottom, bottom),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Dashed centre line.
    drawLine(
        color = faint,
        start = Offset(center.x, top),
        end = Offset(center.x, bottom),
        strokeWidth = 4.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(12.dp.toPx(), 14.dp.toPx()),
        ),
    )
}
