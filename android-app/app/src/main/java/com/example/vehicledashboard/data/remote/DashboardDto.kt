package com.example.vehicledashboard.data.remote

import com.example.vehicledashboard.domain.DrivingStatus
import com.example.vehicledashboard.domain.MediaAction
import com.example.vehicledashboard.domain.MediaState
import com.example.vehicledashboard.domain.NavigationState
import com.example.vehicledashboard.domain.VehicleState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Data Transfer Objects: an exact Kotlin mirror of the backend's Pydantic models.
 *
 * The backend sends snake_case, Kotlin uses camelCase, so every field carries an
 * explicit @SerialName. Keeping the JSON names in exactly one place means a
 * backend rename breaks compilation here and nowhere else.
 */

@Serializable
data class DashboardStateDto(
    @SerialName("timestamp") val timestamp: String,
    @SerialName("speed_kmh") val speedKmh: Int,
    @SerialName("battery_percent") val batteryPercent: Int,
    @SerialName("outside_temperature_c") val outsideTemperatureC: Double,
    @SerialName("driving_status") val drivingStatus: String,
    @SerialName("media") val media: MediaStateDto,
    @SerialName("navigation") val navigation: NavigationStateDto,
)

@Serializable
data class MediaStateDto(
    @SerialName("is_playing") val isPlaying: Boolean,
    @SerialName("track_name") val trackName: String,
    @SerialName("progress_percent") val progressPercent: Int,
)

@Serializable
data class NavigationStateDto(
    @SerialName("destination") val destination: String,
    @SerialName("remaining_minutes") val remainingMinutes: Int,
    @SerialName("distance_km") val distanceKm: Double,
)

/**
 * The body of POST /api/dashboard/sync.
 *
 * The `displayed_` prefix is the backend's wording: these are the values this
 * app is currently showing, which the backend validates and writes to its log.
 */
@Serializable
data class DashboardSyncRequestDto(
    @SerialName("displayed_speed_kmh") val displayedSpeedKmh: Int,
    @SerialName("displayed_battery_percent") val displayedBatteryPercent: Int,
    @SerialName("displayed_temperature_c") val displayedTemperatureC: Double,
    @SerialName("displayed_driving_status") val displayedDrivingStatus: String,
    @SerialName("media") val media: MediaStateDto,
    @SerialName("navigation") val navigation: NavigationStateDto,
    @SerialName("media_action") val mediaAction: String,
)

@Serializable
data class SyncResponseDto(
    @SerialName("status") val status: String,
    @SerialName("received_at") val receivedAt: String,
    @SerialName("logged_to") val loggedTo: String,
)

@Serializable
data class HealthResponseDto(
    @SerialName("status") val status: String,
    @SerialName("service") val service: String,
    @SerialName("uptime_seconds") val uptimeSeconds: Double,
)

// --- Mapping DTO <-> domain -------------------------------------------------

fun DashboardStateDto.toDomain(): VehicleState = VehicleState(
    speedKmh = speedKmh,
    batteryPercent = batteryPercent,
    temperatureC = outsideTemperatureC,
    drivingStatus = DrivingStatus.fromBackend(drivingStatus),
    media = MediaState(
        isPlaying = media.isPlaying,
        trackName = media.trackName,
        progressPercent = media.progressPercent,
    ),
    navigation = NavigationState(
        destination = navigation.destination,
        remainingMinutes = navigation.remainingMinutes,
        distanceKm = navigation.distanceKm,
    ),
    // The backend sends ISO-8601 UTC, e.g. "2026-07-30T18:50:06.721560Z".
    // Instant.parse handles the microseconds; if it ever fails we fall back to
    // "now" rather than losing an otherwise valid payload.
    updatedAt = runCatching { Instant.parse(timestamp) }.getOrElse { Instant.now() },
)

/** Builds the sync body from what is currently on screen plus the driver's action. */
fun VehicleState.toSyncRequest(action: MediaAction): DashboardSyncRequestDto =
    DashboardSyncRequestDto(
        displayedSpeedKmh = speedKmh,
        displayedBatteryPercent = batteryPercent,
        displayedTemperatureC = temperatureC,
        // The backend validates against "Parked"/"Driving"/"Charging", so the
        // enum is converted back to the backend's own spelling.
        displayedDrivingStatus = when (drivingStatus) {
            DrivingStatus.PARKED -> "Parked"
            DrivingStatus.DRIVING -> "Driving"
            DrivingStatus.CHARGING -> "Charging"
            DrivingStatus.UNKNOWN -> "Parked"
        },
        media = MediaStateDto(
            isPlaying = media.isPlaying,
            trackName = media.trackName,
            progressPercent = media.progressPercent,
        ),
        navigation = NavigationStateDto(
            destination = navigation.destination,
            remainingMinutes = navigation.remainingMinutes,
            distanceKm = navigation.distanceKm,
        ),
        mediaAction = action.wireValue,
    )
