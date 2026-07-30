package com.example.vehicledashboard.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Creates the single Retrofit/OkHttp instance the app uses.
 *
 * This is dependency injection done by hand. A framework such as Hilt would add
 * annotations, generated code and build time for exactly one dependency graph
 * with one edge in it - not a good trade at this size. An `object` with a lazy
 * property gives the same benefit (one shared client, created on first use).
 */
object NetworkModule {

    /**
     * 10.0.2.2 is the Android emulator's alias for "the host machine's
     * 127.0.0.1". A real device on the same Wi-Fi would use the PC's LAN IP.
     * The trailing slash matters: Retrofit resolves endpoint paths against it.
     */
    const val BASE_URL = "http://10.0.2.2:8000/"

    private val json = Json {
        // Tolerate fields the backend adds later - a new JSON key must never
        // crash a running dashboard.
        ignoreUnknownKeys = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        // Short timeouts: this is a 1 Hz poll against a local server. Waiting
        // 10 seconds for a dead backend would freeze the connection indicator
        // long after the driver can see something is wrong.
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    val dashboardApi: DashboardApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DashboardApi::class.java)
    }
}
