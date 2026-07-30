package com.example.vehicledashboard.data

import com.example.vehicledashboard.data.remote.DashboardApi
import com.example.vehicledashboard.data.remote.NetworkModule
import com.example.vehicledashboard.data.remote.toDomain
import com.example.vehicledashboard.data.remote.toSyncRequest
import com.example.vehicledashboard.domain.MediaAction
import com.example.vehicledashboard.domain.VehicleState

/**
 * The single place the app talks to the backend.
 *
 * Its job is to hide *where* data comes from. The ViewModel asks for vehicle
 * data and gets either a value or a failure; it never sees Retrofit, HTTP codes
 * or JSON. If the data source were swapped later (a real vehicle bus, a local
 * cache), only this class would change.
 *
 * Errors are returned as Kotlin's `Result` instead of thrown, because a
 * dashboard losing its connection is an expected everyday situation, not an
 * exceptional one - it should end up as a UI state, not a crash.
 */
class DashboardRepository(
    private val api: DashboardApi = NetworkModule.dashboardApi,
) {

    /** One poll of the simulated vehicle data. */
    suspend fun fetchDashboard(): Result<VehicleState> =
        runCatching { api.getDashboard().toDomain() }

    /**
     * Reports the currently displayed values and the driver's media action.
     * Returns the backend's status string ("accepted") on success.
     */
    suspend fun syncDisplayedState(
        state: VehicleState,
        action: MediaAction,
    ): Result<String> = runCatching {
        api.syncDashboard(state.toSyncRequest(action)).status
    }
}
