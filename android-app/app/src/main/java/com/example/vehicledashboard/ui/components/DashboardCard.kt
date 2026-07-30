package com.example.vehicledashboard.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The one card shape every panel uses.
 *
 * Having a single container keeps the grid visually calm: identical corner
 * radius, padding and section-label styling everywhere. On a driver display,
 * consistency is what lets the eye find a value without searching.
 */
@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    // Wide letter spacing marks these as quiet section labels,
                    // so they never compete with the values themselves.
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            content()
        }
    }
}
