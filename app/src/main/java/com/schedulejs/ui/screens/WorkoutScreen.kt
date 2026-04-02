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
fun WorkoutScreen(state: WorkoutUiState) {
    ScreenFrame(
        title = "Workout Module",
        subtitle = "Static routine content from the workout plan with the timer UX represented but not active yet."
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
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text(state.bellyRoutineState.ctaLabel)
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

            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (state.isWorkoutComplete) "Workout Completed" else "Mark Workout Complete"
                )
            }
        }
    }
}
