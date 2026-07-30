package com.example.vehicledashboard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Destination, remaining time and remaining distance, plus a schematic route.
 *
 * The graphic is drawn with Canvas rather than a map SDK: the backend sends no
 * geodata, so a real map would be decoration pretending to be information. The
 * abstract route is honest about what is actually known - a start, a
 * destination, and how much of the way is left.
 */
@Composable
fun NavigationCard(
    destination: String,
    remainingMinutes: Int,
    distanceKm: Double,
    modifier: Modifier = Modifier,
) {
    // The backend sends the distance still to go, but not the trip's total
    // length, so "how far along are we" has to be inferred. The largest
    // distance seen since the app started is treated as the full route. It is
    // exact from the first reading of a parked vehicle onwards. A
    // `total_distance_km` field in the API would remove the guess entirely.
    val longestSeen = remember { mutableFloatStateOf(0f) }
    if (distanceKm.toFloat() > longestSeen.floatValue) {
        longestSeen.floatValue = distanceKm.toFloat()
    }
    val travelledFraction = if (longestSeen.floatValue > 0f) {
        (1f - (distanceKm.toFloat() / longestSeen.floatValue)).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = travelledFraction,
        animationSpec = tween(durationMillis = 950),
        label = "routeProgress",
    )

    val routeColor = MaterialTheme.colorScheme.primary
    val travelledColor = MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.error

    DashboardCard(modifier = modifier, title = "NAVIGATION") {
        Row(modifier = Modifier.fillMaxSize()) {

            // Proportional instead of a fixed 300.dp: a hard-coded width only
            // fits one screen size, and a two-line destination overflowed it.
            // The weights also guarantee the route canvas keeps the larger half.
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .padding(end = 16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text(
                    text = destination,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 30.sp,
                    // Without an explicit lineHeight the two lines of a long
                    // destination collide; 34.sp gives them room to breathe.
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(top = 24.dp),
                ) {
                    Text(
                        text = remainingMinutes.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "min",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                    )
                }

                Text(
                    // One decimal: distance changes slowly enough to be worth it.
                    text = "${(distanceKm * 10).roundToInt() / 10.0} km",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Box(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawSchematicRoute(
                        progress = animatedProgress,
                        routeColor = routeColor,
                        travelledColor = travelledColor,
                        gridColor = gridColor,
                        markerColor = markerColor,
                    )
                }
            }
        }
    }
}

/**
 * A stylised route: faint grid, a zig-zag line, the part already driven dimmed,
 * a vehicle dot at the current position and a pin at the destination.
 *
 * All coordinates are fractions of the canvas, so it scales to any card size.
 */
private fun DrawScope.drawSchematicRoute(
    progress: Float,
    routeColor: Color,
    travelledColor: Color,
    gridColor: Color,
    markerColor: Color,
) {
    val w = size.width
    val h = size.height

    // Background grid, suggesting streets without claiming to be a map.
    val gridStroke = 1.5f.dp.toPx()
    for (i in 1..5) {
        val x = w * i / 6f
        drawLine(gridColor, Offset(x, 0f), Offset(x, h), gridStroke)
    }
    for (i in 1..4) {
        val y = h * i / 5f
        drawLine(gridColor, Offset(0f, y), Offset(w, y), gridStroke)
    }

    // Route waypoints as fractions of the canvas, start (bottom-left) to
    // destination (top-right).
    val points = listOf(
        Offset(0.12f * w, 0.88f * h),
        Offset(0.28f * w, 0.72f * h),
        Offset(0.34f * w, 0.52f * h),
        Offset(0.52f * w, 0.46f * h),
        Offset(0.62f * w, 0.30f * h),
        Offset(0.80f * w, 0.24f * h),
        Offset(0.88f * w, 0.12f * h),
    )

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path = path,
        color = routeColor,
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Walk the polyline to find the point at `progress` along its total length.
    val segmentLengths = points.zipWithNext { a, b ->
        val dx = b.x - a.x
        val dy = b.y - a.y
        kotlin.math.sqrt(dx * dx + dy * dy)
    }
    val totalLength = segmentLengths.sum()
    var remaining = totalLength * progress.coerceIn(0f, 1f)

    var vehiclePosition = points.first()
    val travelled = Path().apply { moveTo(points.first().x, points.first().y) }

    for ((index, length) in segmentLengths.withIndex()) {
        val from = points[index]
        val to = points[index + 1]
        if (remaining >= length) {
            travelled.lineTo(to.x, to.y)
            vehiclePosition = to
            remaining -= length
        } else {
            val t = if (length == 0f) 0f else remaining / length
            vehiclePosition = Offset(
                from.x + (to.x - from.x) * t,
                from.y + (to.y - from.y) * t,
            )
            travelled.lineTo(vehiclePosition.x, vehiclePosition.y)
            remaining = 0f
            break
        }
    }

    // The already-driven part is dimmed, so "what is left" stands out.
    drawPath(
        path = travelled,
        color = travelledColor,
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Destination pin: a circle with a tail.
    val destination = points.last()
    drawCircle(color = markerColor, radius = 13.dp.toPx(), center = destination)
    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = destination)

    // Current position: a blue dot with a soft halo.
    drawCircle(color = routeColor.copy(alpha = 0.25f), radius = 24.dp.toPx(), center = vehiclePosition)
    drawCircle(color = routeColor, radius = 12.dp.toPx(), center = vehiclePosition)
    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = vehiclePosition)
}
