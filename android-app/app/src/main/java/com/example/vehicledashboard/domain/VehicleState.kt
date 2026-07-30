package com.example.vehicledashboard.domain

import java.time.Instant

/**
 * The vehicle data as the UI wants to use it.
 *
 * Why a separate model instead of showing the network DTOs directly?
 * The DTOs mirror the backend's JSON (snake_case names, a driving status that
 * is just a String, a timestamp that is just text). This layer converts that
 * into Kotlin types the UI can trust: a real enum, a real Instant. If the
 * backend ever renames a field, only the mapper changes - no composable does.
 */
data class VehicleState(
    val speedKmh: Int,
    val batteryPercent: Int,
    val temperatureC: Double,
    val drivingStatus: DrivingStatus,
    val media: MediaState,
    val navigation: NavigationState,
    /** When the backend produced these values (UTC). Used as a freshness clock. */
    val updatedAt: Instant,
)

/**
 * The three states the assignment defines.
 *
 * UNKNOWN exists so that an unexpected value from the backend degrades to a
 * neutral display instead of crashing the app.
 */
enum class DrivingStatus(val label: String) {
    PARKED("PARKED"),
    DRIVING("DRIVING"),
    CHARGING("CHARGING"),
    UNKNOWN("—");

    companion object {
        fun fromBackend(value: String): DrivingStatus = when (value) {
            "Parked" -> PARKED
            "Driving" -> DRIVING
            "Charging" -> CHARGING
            else -> UNKNOWN
        }
    }
}

data class MediaState(
    val isPlaying: Boolean,
    val trackName: String,
    /** 0..100. The backend does not send elapsed/total time, so percent is all we show. */
    val progressPercent: Int,
)

data class NavigationState(
    val destination: String,
    val remainingMinutes: Int,
    val distanceKm: Double,
)

/** What the driver just pressed, sent to the backend with the sync request. */
enum class MediaAction(val wireValue: String) {
    NONE("none"),
    PLAY("play"),
    PAUSE("pause"),
    TOGGLE("toggle"),
}
