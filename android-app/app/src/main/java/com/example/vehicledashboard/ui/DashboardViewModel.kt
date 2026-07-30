package com.example.vehicledashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vehicledashboard.data.DashboardRepository
import com.example.vehicledashboard.domain.MediaAction
import com.example.vehicledashboard.domain.VehicleState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Holds the dashboard state and keeps it up to date.
 *
 * Why a ViewModel: it survives configuration changes (a tablet rotating, or the
 * activity being recreated), so the polling loop is not restarted and the last
 * values are not lost. Why StateFlow: the UI observes one always-available
 * "current value" and recomposes when it changes - which is exactly how Compose
 * wants to be driven.
 *
 * All simulation lives in the backend. This class only fetches and forwards.
 */
class DashboardViewModel(
    private val repository: DashboardRepository = DashboardRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)

    /** Read-only view for the UI: only this class may change the state. */
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /** The last values actually received, kept so an offline screen can stay useful. */
    private var lastKnown: VehicleState? = null

    init {
        startPolling()
    }

    /**
     * Polls the backend about once per second.
     *
     * The loop lives in `viewModelScope`, so it is cancelled automatically when
     * the ViewModel is cleared - no manual teardown, no leaked coroutine.
     *
     * One second is a deliberate compromise: fast enough that the speed and
     * media progress look live, slow enough to stay cheap. The values are
     * animated between polls so the gauge still moves smoothly (see
     * SpeedGaugeCard), rather than jumping once per second.
     */
    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun refresh() {
        repository.fetchDashboard()
            .onSuccess { vehicle ->
                lastKnown = vehicle
                _uiState.value = DashboardUiState.Connected(vehicle)
            }
            .onFailure { error ->
                _uiState.value = DashboardUiState.Offline(
                    reason = error.message ?: "Backend unreachable",
                    lastKnown = lastKnown,
                )
            }
    }

    /**
     * Called when the driver taps play/pause.
     *
     * The app does not flip its own flag. It sends the action to the backend,
     * which owns the media state; the next poll (max one second later) brings
     * the new state back. That keeps exactly one source of truth and is why the
     * button works even though the app runs no simulation of its own.
     */
    fun onPlayPauseClicked() {
        val current = lastKnown ?: return
        viewModelScope.launch {
            repository.syncDisplayedState(current, MediaAction.TOGGLE)
                .onSuccess { refresh() } // reflect the change immediately
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 1_000L
    }
}
