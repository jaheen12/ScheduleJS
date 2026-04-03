package com.schedulejs.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedulejs.ui.SettingsUiState
import com.schedulejs.ui.viewmodel.TemplateField

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onLeadTimeSelected: (String) -> Unit,
    onTransitAlertsChanged: (Boolean) -> Unit,
    onWakeUpTimeChanged: (Long, String) -> Unit,
    onTaskFieldChanged: (Long, Long, TemplateField, String) -> Unit,
    onSave: () -> Unit,
    onPermissionAction: (String) -> Unit,
    onPermissionDismiss: (String) -> Unit
) {
    ScreenFrame(
        title = "Settings & Automation",
        subtitle = "Phase 5 adds settings-backed edits, validation, permission education, and restart-safe automation."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.permissionEducationCards.isNotEmpty()) {
                SectionCard("First-Run Permissions") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.permissionEducationCards.forEach { card ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = card.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = card.description,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { onPermissionAction(card.id) }) {
                                        Text(card.actionLabel)
                                    }
                                    Button(onClick = { onPermissionDismiss(card.id) }) {
                                        Text(card.dismissLabel)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SectionCard("Notification Lead Time") {
                Text(
                    text = state.notificationLeadTime,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                listOf("On time", "5 minutes before", "10 minutes before").forEach { option ->
                    FilterChip(
                        selected = option == state.notificationLeadTime,
                        onClick = { onLeadTimeSelected(option) },
                        label = { Text(option) }
                    )
                }
            }

            SectionCard("Transit Alerts") {
                Row(
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
                        onCheckedChange = onTransitAlertsChanged
                    )
                }
            }

            SectionCard("Template Summaries") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.templateSummaries.forEach { item ->
                        EmptyStatePill("${item.title} • ${item.summary}")
                    }
                }
            }

            state.editableTemplates.forEach { template ->
                SectionCard("${template.title} Template") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = template.dayTypeLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = template.wakeUpTime,
                            onValueChange = { onWakeUpTimeChanged(template.templateId, it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Wake-up time (HH:mm)") }
                        )
                        template.tasks.forEach { task ->
                            SectionCard(task.title.ifBlank { "Task" }) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = task.title,
                                        onValueChange = {
                                            onTaskFieldChanged(template.templateId, task.taskId, TemplateField.TITLE, it)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Task title") }
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = task.startTime,
                                            onValueChange = {
                                                onTaskFieldChanged(template.templateId, task.taskId, TemplateField.START_TIME, it)
                                            },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("Start") }
                                        )
                                        OutlinedTextField(
                                            value = task.endTime,
                                            onValueChange = {
                                                onTaskFieldChanged(template.templateId, task.taskId, TemplateField.END_TIME, it)
                                            },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("End") }
                                        )
                                    }
                                    OutlinedTextField(
                                        value = task.details,
                                        onValueChange = {
                                            onTaskFieldChanged(template.templateId, task.taskId, TemplateField.DETAILS, it)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        label = { Text("Task details") }
                                    )
                                }
                            }
                        }
                    }
                }
            }

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

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}
