package com.example.vehicledashboard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Media controller: track name, play/pause and a progress indicator.
 *
 * The button does not toggle a local flag. It reports the action to the
 * backend, which owns the media state, and the change comes back with the next
 * poll. That keeps a single source of truth (see DashboardViewModel).
 *
 * The backend sends only a progress percentage - no elapsed time, no duration,
 * no artist - so none of those are displayed. Showing invented values would
 * look better and be wrong.
 */
@Composable
fun MediaCard(
    trackName: String,
    isPlaying: Boolean,
    progressPercent: Int,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeProgress = progressPercent.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = safeProgress / 100f,
        animationSpec = tween(durationMillis = 950),
        label = "mediaProgress",
    )

    DashboardCard(modifier = modifier, title = "NOW PLAYING") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtwork(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp),
            ) {
                Text(
                    text = trackName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isPlaying) "Playing" else "Paused",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            PlayPauseButton(isPlaying = isPlaying, onClick = onPlayPauseClick)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text(
                text = "$safeProgress %",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

/**
 * The one interactive control on the dashboard.
 *
 * 88 dp is far above the 48 dp minimum touch target: in a moving vehicle the
 * driver aims badly, so the button has to be forgiving.
 */
@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val glyphColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(34.dp)) {
            if (isPlaying) drawPauseGlyph(glyphColor) else drawPlayGlyph(glyphColor)
        }
    }
}

/** Two rounded bars. */
private fun DrawScope.drawPauseGlyph(color: Color) {
    val barWidth = size.width * 0.28f
    val gap = size.width * 0.16f
    val left = (size.width - (barWidth * 2 + gap)) / 2f

    drawRoundRect(
        color = color,
        topLeft = Offset(left, 0f),
        size = Size(barWidth, size.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 3f, barWidth / 3f),
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(left + barWidth + gap, 0f),
        size = Size(barWidth, size.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 3f, barWidth / 3f),
    )
}

/** A triangle pointing right. */
private fun DrawScope.drawPlayGlyph(color: Color) {
    val path = Path().apply {
        moveTo(size.width * 0.15f, 0f)
        lineTo(size.width * 0.95f, size.height / 2f)
        lineTo(size.width * 0.15f, size.height)
        close()
    }
    drawPath(path = path, color = color)
}

/**
 * Stand-in cover art, drawn rather than bundled.
 *
 * The backend sends no artwork, and shipping a picture would imply data the
 * system does not have. A generated gradient scene keeps the layout honest and
 * adds no asset to the APK.
 */
@Composable
private fun AlbumArtwork(modifier: Modifier = Modifier) {
    val skyTop = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val skyBottom = MaterialTheme.colorScheme.surfaceVariant
    val sun = MaterialTheme.colorScheme.tertiary
    val road = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Canvas(modifier = modifier) {
        drawRect(brush = Brush.verticalGradient(listOf(skyTop, skyBottom)))

        // Low sun on the horizon.
        drawCircle(
            color = sun,
            radius = size.minDimension * 0.22f,
            center = Offset(size.width / 2f, size.height * 0.42f),
        )

        // Horizon and a road running to it.
        val horizon = size.height * 0.58f
        drawLine(
            color = road,
            start = Offset(0f, horizon),
            end = Offset(size.width, horizon),
            strokeWidth = 2.dp.toPx(),
        )
        drawLine(
            color = road,
            start = Offset(size.width * 0.38f, horizon),
            end = Offset(size.width * 0.05f, size.height),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = road,
            start = Offset(size.width * 0.62f, horizon),
            end = Offset(size.width * 0.95f, size.height),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
