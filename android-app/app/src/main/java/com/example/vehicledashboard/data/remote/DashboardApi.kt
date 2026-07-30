package com.example.vehicledashboard.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * The backend contract, expressed as Kotlin functions.
 *
 * Retrofit generates the implementation at runtime: the annotations say which
 * HTTP method and path to use, the return type says how to parse the answer.
 * `suspend` means each call is a coroutine - it never blocks the UI thread and
 * needs no callbacks.
 */
interface DashboardApi {

    /** Liveness check used to distinguish "backend down" from "backend slow". */
    @GET("health")
    suspend fun getHealth(): HealthResponseDto

    /** The simulated vehicle data. Polled roughly once per second. */
    @GET("api/dashboard")
    suspend fun getDashboard(): DashboardStateDto

    /**
     * Sends the values currently displayed plus the driver's media action.
     * The backend validates them, writes them to its log file and acknowledges.
     */
    @POST("api/dashboard/sync")
    suspend fun syncDashboard(@Body body: DashboardSyncRequestDto): SyncResponseDto
}
