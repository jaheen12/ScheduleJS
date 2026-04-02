package com.schedulejs.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedulejs.ui.ReviewUiState

@Composable
fun ReviewScreen(state: ReviewUiState) {
    ScreenFrame(
        title = "Friday Review",
        subtitle = "Locked/unlocked states are both represented; Phase 4 will connect the real Friday 15:30 rule."
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
                        text = "The form below is shown as a preview of the upcoming flow.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard("Weekly Questions") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.questions.forEachIndexed { index, question ->
                        EmptyStatePill("${index + 1}. ${question.prompt}")
                        Text(
                            text = question.placeholder,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
