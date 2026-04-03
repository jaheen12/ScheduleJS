package com.schedulejs.ui

data class DashboardUiState(
    val dateLabel: String,
    val currentTask: TaskSnapshot,
    val nextTask: TaskSnapshot,
    val progressPercent: Float,
    val timelineItems: List<TimelineItem>
)

data class WorkoutUiState(
    val dayLabel: String,
    val routineItems: List<RoutineItem>,
    val bellyRoutineState: BellyRoutineState,
    val isWorkoutComplete: Boolean
)

data class StudyUiState(
    val morningSubject: String,
    val eveningSubject: String,
    val focusTimerState: FocusTimerState,
    val reminderText: String
)

data class ReviewUiState(
    val isUnlocked: Boolean,
    val questions: List<ReviewQuestion>,
    val historySummaries: List<ReviewHistoryItem>,
    val answerDraft: ReviewAnswerDraft,
    val validationMessages: List<String>,
    val saveStatus: String?
)

data class SettingsUiState(
    val notificationLeadTime: String,
    val transitAlertsEnabled: Boolean,
    val templateSummaries: List<TemplateSummary>,
    val editableTemplates: List<EditableTemplateUiState>,
    val permissionEducationCards: List<PermissionEducationCardUiState>,
    val validationMessages: List<String>,
    val saveStatus: String?
)

data class TaskSnapshot(
    val title: String,
    val timeLabel: String,
    val subtitle: String
)

data class TimelineItem(
    val timeLabel: String,
    val title: String,
    val detail: String,
    val state: TimelineItemState
)

enum class TimelineItemState {
    PAST,
    CURRENT,
    UPCOMING
}

data class RoutineItem(
    val title: String,
    val prescription: String,
    val note: String = ""
)

data class BellyRoutineState(
    val ctaLabel: String,
    val steps: List<String>,
    val statusLabel: String = "",
    val secondaryCtaLabel: String? = null
)

data class FocusTimerState(
    val ctaLabel: String,
    val durationLabel: String,
    val statusLabel: String,
    val secondaryCtaLabel: String? = null,
    val isDndEnabled: Boolean = false,
    val isDndPermissionGranted: Boolean = false,
    val dndStatusLabel: String = "",
    val dndPermissionCtaLabel: String? = null
)

data class ReviewQuestion(
    val prompt: String,
    val placeholder: String
)

data class ReviewHistoryItem(
    val weekLabel: String,
    val summary: String
)

data class TemplateSummary(
    val title: String,
    val summary: String
)

data class EditableTemplateUiState(
    val templateId: Long,
    val title: String,
    val dayTypeLabel: String,
    val wakeUpTime: String,
    val tasks: List<EditableTaskUiState>
)

data class EditableTaskUiState(
    val taskId: Long,
    val title: String,
    val startTime: String,
    val endTime: String,
    val details: String
)

data class PermissionEducationCardUiState(
    val id: String,
    val title: String,
    val description: String,
    val actionLabel: String,
    val dismissLabel: String = "Dismiss"
)

data class ReviewAnswerDraft(
    val covered: String,
    val behind: String,
    val tuition: String,
    val energy: String,
    val adjustment: String
)
