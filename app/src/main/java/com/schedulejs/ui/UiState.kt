package com.schedulejs.ui

data class DashboardUiState(
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
    val historySummaries: List<ReviewHistoryItem>
)

data class SettingsUiState(
    val notificationLeadTime: String,
    val transitAlertsEnabled: Boolean,
    val templateSummaries: List<TemplateSummary>
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
    val steps: List<String>
)

data class FocusTimerState(
    val ctaLabel: String,
    val durationLabel: String,
    val statusLabel: String
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
