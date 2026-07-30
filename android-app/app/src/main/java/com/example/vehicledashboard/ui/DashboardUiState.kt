package com.example.vehicledashboard.ui

import com.example.vehicledashboard.domain.VehicleState

/**
 * Everything the screen can be, as three explicit cases.
 *
 * A sealed interface (instead of one class with `isLoading`, `error` and
 * `data` fields) makes impossible combinations unrepresentable: there is no way
 * to be loading and offline at the same time, and `when` forces every case to
 * be handled at compile time.
 */
sealed interface DashboardUiState {

    /** Before the first successful response - shown once, at startup. */
    data object Loading : DashboardUiState

    /** The normal case: fresh data from the backend. */
    data class Connected(val vehicle: VehicleState) : DashboardUiState

    /**
     * The backend could not be reached.
     *
     * `lastKnown` keeps the values from the final successful poll so the
     * dashboard can stay on screen, dimmed and clearly marked OFFLINE, instead
     * of blanking out. Note that these are real values that were once received -
     * the app never invents or simulates data (that is the backend's job).
     * On the very first poll there is nothing yet, so it is null.
     */
    data class Offline(
        val reason: String,
        val lastKnown: VehicleState?,
    ) : DashboardUiState
}
