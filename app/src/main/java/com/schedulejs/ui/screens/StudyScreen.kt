package com.schedulejs.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedulejs.ui.StudyUiState

@Composable
fun StudyScreen(
    state: StudyUiState,
    onFocusTimerAction: () -> Unit,
    onCancelFocusTimer: () -> Unit
) {
    ScreenFrame(
        title = "Study Module",
        subtitle = "Phase 3 adds a persisted focus timer with start, pause, resume, and cancel."
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
