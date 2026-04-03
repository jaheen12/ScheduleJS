package com.schedulejs.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    ScreenFrame(
        title = "Study Module",
        subtitle = "Focus timer state persists, and Phase 4 can drive Do Not Disturb during deep work."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard("Today's Blocks") {
                Text(
                    text = "Morning: ${state.morningSubject}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Evening: ${state.eveningSubject}",
                    style = MaterialTheme.typography.titleMedium
                )
                EmptyStatePill(state.reminderText)
            }

            SectionCard("Focus Engine") {
                Text(
                    text = state.focusTimerState.statusLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    Text("${state.focusTimerState.ctaLabel} • ${state.focusTimerState.durationLabel}")
                }
                state.focusTimerState.secondaryCtaLabel?.let { secondaryLabel ->
                    OutlinedButton(onClick = onCancelFocusTimer, modifier = Modifier.fillMaxWidth()) {
                        Text(secondaryLabel)
                    }
                }
            }
        }
    }
}
