package com.schedulejs.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedulejs.ui.ReviewUiState
import com.schedulejs.ui.viewmodel.ReviewField

@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onAnswerChange: (ReviewField, String) -> Unit,
    onSave: () -> Unit
) {
    ScreenFrame(
        title = "Friday Review",
        subtitle = "Weekly review answers now persist locally and require all fields before saving."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard("Status") {
                if (state.isUnlocked) {
                    Text(
                        text = "Review unlocked. Capture the week while it's still fresh.",
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = "Review unlocks Friday at 15:30.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "You can draft answers now, but the intended completion window starts after unlock.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard("Weekly Questions") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReviewInput(
                        label = state.questions[0].prompt,
                        value = state.answerDraft.covered,
                        onValueChange = { onAnswerChange(ReviewField.COVERED, it) }
                    )
                    ReviewInput(
                        label = state.questions[1].prompt,
                        value = state.answerDraft.behind,
                        onValueChange = { onAnswerChange(ReviewField.BEHIND, it) }
                    )
                    ReviewInput(
                        label = state.questions[2].prompt,
                        value = state.answerDraft.tuition,
                        onValueChange = { onAnswerChange(ReviewField.TUITION, it) }
                    )
                    ReviewInput(
                        label = state.questions[3].prompt,
                        value = state.answerDraft.energy,
                        onValueChange = { onAnswerChange(ReviewField.ENERGY, it) }
                    )
                    ReviewInput(
                        label = state.questions[4].prompt,
                        value = state.answerDraft.adjustment,
                        onValueChange = { onAnswerChange(ReviewField.ADJUSTMENT, it) }
                    )
                    state.validationMessages.forEach { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    state.saveStatus?.let { status ->
                        Text(
                            text = status,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Button(onClick = onSave) {
                        Text("Save Review")
                    }
                }
            }

            SectionCard("Recent History") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.historySummaries.forEach { item ->
                        EmptyStatePill("${item.weekLabel} • ${item.summary}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )
}
