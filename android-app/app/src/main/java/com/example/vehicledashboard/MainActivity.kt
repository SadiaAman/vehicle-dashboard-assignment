package com.example.vehicledashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.vehicledashboard.ui.DashboardScreen
import com.example.vehicledashboard.ui.theme.VehicleDashboardTheme

/**
 * The app's single screen.
 *
 * There is deliberately nothing here but wiring: no state, no networking, no
 * business logic. Everything the dashboard does lives in the ViewModel and the
 * composables, which is what makes those parts testable without an Activity.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draws behind the system bars for a full-screen instrument look;
        // DashboardScreen adds the necessary insets padding itself.
        enableEdgeToEdge()
        setContent {
            VehicleDashboardTheme {
                DashboardScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
