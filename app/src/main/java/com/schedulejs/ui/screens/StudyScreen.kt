package com.schedulejs.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schedulejs.ui.StudyUiState

@Composable
fun StudyScreen(
    state: StudyUiState,
    onFocusTimerAction: () -> Unit,
    onCancelFocusTimer: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onRequestDndPermission: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StudyHeaderCard(
                dayLabel = state.dayLabel,
                templateLabel = state.templateLabel,
                summaryLine = buildString {
                    if (state.morningBlock != null) append("Major: ${state.morningBlock.subject}")
                    if (state.eveningBlock != null) {
                        if (isNotEmpty()) append(" - ")
                        append("Light: ${state.eveningBlock.subject}")
                    }
                    if (isEmpty()) append("Recovery and reset day.")
                },
                isFocusRunning = state.focusTimerState.ctaLabel == "Pause Deep Work",
                isFreeDay = state.isFreeDay
            )
        }

        if (state.isFreeDay) {
            item {
                RestDayCard()
            }
        } else {
            state.morningBlock?.let { morning ->
                item { StudyBlockCard(block = morning) }
            }
            state.eveningBlock?.let { evening ->
                item { StudyBlockCard(block = evening) }
            }

            item {
                StrategyReminderCard(state.reminderText)
            }

            item {
                SectionCard("Focus Engine") {
                    Text(
                        text = state.focusTimerState.statusLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FocusTimerRing(
                            remainingSeconds = durationToSeconds(state.focusTimerState.durationLabel),
                            totalSeconds = state.focusTimerState.totalSeconds
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Focus mode DND",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = state.focusTimerState.dndStatusLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.focusTimerState.isDndEnabled,
                            onCheckedChange = { onToggleFocusMode() },
                            enabled = state.focusTimerState.isDndPermissionGranted
                        )
                    }
                    state.focusTimerState.dndPermissionCtaLabel?.let { permissionLabel ->
                        OutlinedButton(
                            onClick = onRequestDndPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(permissionLabel)
                        }
                    }
                    Button(onClick = onFocusTimerAction, modifier = Modifier.fillMaxWidth()) {
                        Text("${state.focusTimerState.ctaLabel} - ${state.focusTimerState.durationLabel}")
                    }
                    state.focusTimerState.secondaryCtaLabel?.let { secondaryLabel ->
                        OutlinedButton(onClick = onCancelFocusTimer, modifier = Modifier.fillMaxWidth()) {
                            Text(secondaryLabel)
                        }
                    }
                    state.focusSessionHistory?.let { FocusHistoryStrip(it) }
                }
            }
        }

        state.tomorrowBlock?.let { tomorrow ->
            item { TomorrowPreviewCard(tomorrow) }
        }
    }
}

private fun durationToSeconds(label: String): Int {
    val parts = label.split(":")
    if (parts.size != 2) return 0
    val minutes = parts[0].toIntOrNull() ?: return 0
    val seconds = parts[1].toIntOrNull() ?: return 0
    return (minutes * 60 + seconds).coerceAtLeast(0)
}
