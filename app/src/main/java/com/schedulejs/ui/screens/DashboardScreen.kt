package com.schedulejs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedulejs.ui.DashboardUiState
import com.schedulejs.ui.TimelineItemState

@Composable
fun DashboardScreen(state: DashboardUiState) {
    ScreenFrame(
        title = "Dashboard",
        subtitle = "Phase 3 keeps the HUD live against device time and active timers."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = state.dateLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            SectionCard("Current Block") {
                Text(
                    text = state.currentTask.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.currentTask.timeLabel,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = state.currentTask.subtitle,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
                ProgressTrack(progress = state.progressPercent)
                Text(
                    text = "Next: ${state.nextTask.title} at ${state.nextTask.timeLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard("Timeline") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.timelineItems.forEach { item ->
                        val (accent, container) = when (item.state) {
                            TimelineItemState.PAST -> MaterialTheme.colorScheme.outline to MaterialTheme.colorScheme.surfaceVariant
                            TimelineItemState.CURRENT -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
                            TimelineItemState.UPCOMING -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.secondaryContainer
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(container, RoundedCornerShape(18.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = item.timeLabel,
                                color = accent,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = item.detail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressTrack(progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(999.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(14.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(999.dp)
                    )
            )
        }
        Text(
            text = "${(progress * 100).toInt()}% of current block complete",
            style = MaterialTheme.typography.labelLarge,
            color = Color.Unspecified
        )
    }
}
