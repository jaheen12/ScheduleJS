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
import com.schedulejs.ui.WorkoutUiState

@Composable
fun WorkoutScreen(
    state: WorkoutUiState,
    onBellyRoutineAction: () -> Unit,
    onCancelBellyRoutine: () -> Unit,
    onToggleWorkoutComplete: () -> Unit
) {
    ScreenFrame(
        title = "Workout Module",
        subtitle = "Phase 3 adds a live belly routine timer and persists workout completion for the day."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard("Today's Focus") {
                Text(
                    text = state.dayLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Purpose-built bodyweight plan for the current day.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = state.bellyRoutineState.statusLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onBellyRoutineAction, modifier = Modifier.fillMaxWidth()) {
                    Text(state.bellyRoutineState.ctaLabel)
                }
                state.bellyRoutineState.secondaryCtaLabel?.let { secondaryLabel ->
                    OutlinedButton(onClick = onCancelBellyRoutine, modifier = Modifier.fillMaxWidth()) {
                        Text(secondaryLabel)
                    }
                }
                state.bellyRoutineState.steps.forEach { step ->
                    EmptyStatePill(step)
                }
            }

            SectionCard("Routine") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.routineItems.forEach { item ->
                        EmptyStatePill("${item.title} • ${item.prescription}")
                        if (item.note.isNotBlank()) {
                            Text(
                                text = item.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            OutlinedButton(onClick = onToggleWorkoutComplete, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (state.isWorkoutComplete) "Workout Completed" else "Mark Workout Complete"
                )
            }
        }
    }
}
