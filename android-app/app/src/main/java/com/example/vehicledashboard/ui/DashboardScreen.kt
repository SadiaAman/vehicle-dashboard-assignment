package com.example.vehicledashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vehicledashboard.data.remote.NetworkModule
import com.example.vehicledashboard.domain.VehicleState
import com.example.vehicledashboard.ui.components.BatteryCard
import com.example.vehicledashboard.ui.components.DashboardTopBar
import com.example.vehicledashboard.ui.components.MediaCard
import com.example.vehicledashboard.ui.components.NavigationCard
import com.example.vehicledashboard.ui.components.SpeedGaugeCard
import com.example.vehicledashboard.ui.components.TemperatureCard

/**
 * The whole dashboard.
 *
 * This composable only *reads* state and *reports* clicks - it holds no logic
 * of its own. That is the point of MVVM: the screen can be reasoned about as
 * "given this state, this is what is drawn".
 */
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(),
) {
    // collectAsStateWithLifecycle stops collecting while the app is in the
    // background, so a backgrounded dashboard does not keep recomposing.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Keeps content clear of the status/navigation bars and any notch.
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        val vehicle = when (uiState) {
            is DashboardUiState.Connected -> (uiState as DashboardUiState.Connected).vehicle
            is DashboardUiState.Offline -> (uiState as DashboardUiState.Offline).lastKnown
            DashboardUiState.Loading -> null
        }

        DashboardTopBar(
            isConnected = uiState is DashboardUiState.Connected,
            dataTimestamp = vehicle?.updatedAt,
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            // Values are on screen: show the dashboard. If the connection has
            // dropped, these are the last values actually received - the top bar
            // already says OFFLINE, so nothing is presented as live that is not.
            vehicle != null -> DashboardGrid(
                vehicle = vehicle,
                onPlayPauseClick = viewModel::onPlayPauseClicked,
                modifier = Modifier.weight(1f),
            )

            uiState is DashboardUiState.Loading -> StatusPanel(
                headline = "Connecting to vehicle backend…",
                detail = NetworkModule.BASE_URL,
                showSpinner = true,
                modifier = Modifier.weight(1f),
            )

            else -> StatusPanel(
                headline = "Backend unreachable",
                detail = "No data has been received from ${NetworkModule.BASE_URL}\n" +
                    "Start the backend with:  uvicorn app.main:app --host 0.0.0.0 --port 8000\n\n" +
                    ((uiState as? DashboardUiState.Offline)?.reason ?: ""),
                showSpinner = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * The two-column tablet layout.
 *
 * Left: what the driver checks most often (speed, then battery and outside
 * temperature). Right: the two panels that are read, not glanced at
 * (navigation and media). The split follows how attention is actually spent,
 * not how the data is grouped in the API.
 */
@Composable
private fun DashboardGrid(
    vehicle: VehicleState,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {

        Column(modifier = Modifier.weight(0.44f)) {
            SpeedGaugeCard(
                speedKmh = vehicle.speedKmh,
                drivingStatus = vehicle.drivingStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                BatteryCard(
                    batteryPercent = vehicle.batteryPercent,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(16.dp))
                TemperatureCard(
                    temperatureC = vehicle.temperatureC,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(0.56f)) {
            NavigationCard(
                destination = vehicle.navigation.destination,
                remainingMinutes = vehicle.navigation.remainingMinutes,
                distanceKm = vehicle.navigation.distanceKm,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            MediaCard(
                trackName = vehicle.media.trackName,
                isPlaying = vehicle.media.isPlaying,
                progressPercent = vehicle.media.progressPercent,
                onPlayPauseClick = onPlayPauseClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Full-screen message for the two cases where there is nothing to display:
 * the first load, and a backend that has never answered.
 *
 * The offline text names the exact command to start the backend - during a
 * demo that is the difference between a puzzled pause and a five-second fix.
 */
@Composable
private fun StatusPanel(
    headline: String,
    detail: String,
    showSpinner: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (showSpinner) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 32.dp),
                )
            }
            Text(
                text = headline,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
