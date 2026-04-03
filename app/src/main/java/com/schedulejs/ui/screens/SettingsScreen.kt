package com.schedulejs.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedulejs.ui.SettingsUiState

@Composable
fun SettingsScreen(state: SettingsUiState) {
    ScreenFrame(
        title = "Settings & Automation",
        subtitle = "Lead time, transit alerts, and template summaries now come from local persistence."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard("Notification Lead Time") {
                Text(
                    text = state.notificationLeadTime,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                listOf("On time", "5 minutes before", "10 minutes before").forEach { option ->
                    FilterChip(
                        selected = option == state.notificationLeadTime,
                        onClick = {},
                        label = { Text(option) }
                    )
                }
            }

            SectionCard("Transit Alerts") {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Loud bicycle departure alerts",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Dedicated alarm path for the tight commute windows.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.transitAlertsEnabled,
                        onCheckedChange = {}
                    )
                }
            }

            SectionCard("Templates") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.templateSummaries.forEach { item ->
                        EmptyStatePill("${item.title} • ${item.summary}")
                    }
                }
            }
        }
    }
}
