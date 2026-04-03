package com.schedulejs.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    val snackbarHostState = remember { SnackbarHostState() }
    var summaryExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.saveStatus) {
        state.saveStatus?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        ScreenFrame(
            title = "Settings",
            subtitle = "Customize notifications, alerts, and your daily schedule templates."
        ) {
            Column(
                modifier = Modifier.padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.permissionEducationCards.isNotEmpty()) {
                    SectionCard("First-Run Permissions") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.permissionEducationCards.forEach { card ->
                                PermissionCard(
                                    title = card.title,
                                    description = card.description,
                                    actionLabel = card.actionLabel,
                                    dismissLabel = card.dismissLabel,
                                    onAction = { onPermissionAction(card.id) },
                                    onDismiss = { onPermissionDismiss(card.id) }
                                )
                            }
                        }
                    }
                }

                SectionCard("Notification Lead Time") {
                    Text(
                        text = "Alert me before each scheduled task:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("On time", "5 minutes before", "10 minutes before").forEach { option ->
                            FilterChip(
                                selected = option == state.notificationLeadTime,
                                onClick = { onLeadTimeSelected(option) },
                                label = { Text(option) },
                                leadingIcon = if (option == state.notificationLeadTime) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }

                SectionCard("Transit Alerts") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Bicycle departure alerts",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Loud alarm for tight commute windows.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.transitAlertsEnabled,
                            onCheckedChange = onTransitAlertsChanged
                        )
                    }
                }

                if (state.templateSummaries.isNotEmpty()) {
                    SectionCard("Template Overview") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { summaryExpanded = !summaryExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${state.templateSummaries.size} templates configured")
                            Icon(
                                imageVector = if (summaryExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null
                            )
                        }
                        AnimatedVisibility(visible = summaryExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.templateSummaries.forEach { item ->
                                    EmptyStatePill("${item.title} • ${item.summary}")
                                }
                            }
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
                                label = { Text("Wake-up time") },
                                placeholder = { Text("HH:mm") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null
                                    )
                                },
                                singleLine = true
                            )
                            template.tasks.forEachIndexed { index, task ->
                                if (index > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                TaskEditRow(
                                    templateId = template.templateId,
                                    task = task,
                                    onTaskFieldChanged = onTaskFieldChanged
                                )
                            }
                        }
                    }
                }

                if (state.validationMessages.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = "Please fix the following:",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            state.validationMessages.forEach { message ->
                                Text(
                                    text = "\u2022 $message",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving && state.validationMessages.isEmpty()
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(if (state.isSaving) "Saving..." else "Save Settings")
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    actionLabel: String,
    dismissLabel: String,
    onAction: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(dismissLabel)
                }
                Spacer(Modifier.size(8.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun TaskEditRow(
    templateId: Long,
    task: com.schedulejs.ui.EditableTaskUiState,
    onTaskFieldChanged: (Long, Long, TemplateField, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = task.title.ifBlank { "Task" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = task.title,
            onValueChange = {
                onTaskFieldChanged(templateId, task.taskId, TemplateField.TITLE, it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Task title") },
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = task.startTime,
                onValueChange = {
                    onTaskFieldChanged(templateId, task.taskId, TemplateField.START_TIME, it)
                },
                modifier = Modifier.weight(1f),
                label = { Text("Start") },
                placeholder = { Text("HH:mm") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null
                    )
                },
                singleLine = true
            )
            OutlinedTextField(
                value = task.endTime,
                onValueChange = {
                    onTaskFieldChanged(templateId, task.taskId, TemplateField.END_TIME, it)
                },
                modifier = Modifier.weight(1f),
                label = { Text("End") },
                placeholder = { Text("HH:mm") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null
                    )
                },
                singleLine = true
            )
        }
        OutlinedTextField(
            value = task.details,
            onValueChange = {
                onTaskFieldChanged(templateId, task.taskId, TemplateField.DETAILS, it)
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            label = { Text("Task details") }
        )
    }
}
