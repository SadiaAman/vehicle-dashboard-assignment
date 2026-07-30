package com.example.vehicledashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Title, connection indicator and clock.
 *
 * The clock shows the timestamp of the data itself, converted from the
 * backend's UTC to local time. That is more useful than the device clock: if
 * the value stops advancing, the driver can see the data is stale.
 */
@Composable
fun DashboardTopBar(
    isConnected: Boolean,
    dataTimestamp: Instant?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "VEHICLE DASHBOARD",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            ConnectionIndicator(isConnected = isConnected)

            Text(
                text = dataTimestamp?.let { LOCAL_TIME_FORMAT.format(it) } ?: "--:--",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 24.dp),
            )
        }
    }
}

@Composable
private fun ConnectionIndicator(isConnected: Boolean) {
    // Green/red is reinforced by the word itself, so the state is still
    // readable for a colour-blind driver.
    val color = if (isConnected) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.error
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = if (isConnected) "CONNECTED" else "OFFLINE",
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

/** Formatter is created once: building it on every recomposition would be wasteful. */
private val LOCAL_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
