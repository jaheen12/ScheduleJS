package com.schedulejs.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedulejs.ui.StudyUiState

@Composable
fun StudyScreen(state: StudyUiState) {
    ScreenFrame(
        title = "Study Module",
        subtitle = "The focus engine shell is wired to fixed subjects now so the UI contract stays stable later."
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
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("${state.focusTimerState.ctaLabel} • ${state.focusTimerState.durationLabel}")
                }
            }
        }
    }
}
