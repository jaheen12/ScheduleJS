package com.schedulejs.ui

data class DashboardUiState(
    val dateLabel: String,
    val dayType: String = "",
    val currentTask: TaskSnapshot,
    val nextTask: TaskSnapshot,
    val progressPercent: Float,
    val timelineItems: List<TimelineItem>
)

data class WorkoutUiState(
    val dayLabel: String,
    val muscleGroup: String,
    val purposeNote: String,
    val dayOfWeek: String,
    val weeklyStreak: Int,
    val weekDays: List<WeeklyWorkoutDay>,
    val routineItems: List<RoutineItem>,
    val bellyRoutineState: BellyRoutineState,
    val isWorkoutComplete: Boolean
)

data class StudyUiState(
    val morningBlock: StudyBlockUiState?,
    val eveningBlock: StudyBlockUiState?,
    val focusTimerState: FocusTimerState,
    val reminderText: String,
    val dayLabel: String,
    val templateLabel: String,
    val isFreeDay: Boolean,
    val tomorrowBlock: TomorrowStudyPreview?,
    val focusSessionHistory: FocusSessionHistory?
)

data class ReviewUiState(
    val isUnlocked: Boolean,
    val isPendingToday: Boolean,
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
    val saveStatus: String?,
    val isSaving: Boolean = false
)

data class TaskSnapshot(
    val title: String,
    val timeLabel: String,
    val subtitle: String,
    val category: String = ""
)

data class TimelineItem(
    val timeLabel: String,
    val title: String,
    val detail: String,
    val state: TimelineItemState,
    val category: String = ""
)

enum class TimelineItemState {
    PAST,
    CURRENT,
    UPCOMING
}

data class RoutineItem(
    val id: String,
    val title: String,
    val prescription: String,
    val totalSets: Int,
    val repsOrDuration: String,
    val setsCompleted: Int,
    val restSecondsLeft: Int = 0,
    val note: String = ""
)

data class BellyRoutineState(
    val ctaLabel: String,
    val steps: List<BellyRoutineStep>,
    val currentStepIndex: Int,
    val secondsRemaining: Int,
    val repsCompleted: Int,
    val statusLabel: String = "",
    val isTimerVisible: Boolean = false,
    val secondaryCtaLabel: String? = null
)

data class BellyRoutineStep(
    val name: String,
    val type: StepType,
    val durationSeconds: Int = 0,
    val targetReps: Int = 0
)

enum class StepType {
    TIMED,
    REPS
}

data class WeeklyWorkoutDay(
    val dayLabel: String,
    val muscleGroupEmoji: String,
    val isRestDay: Boolean,
    val isCompleted: Boolean,
    val isCurrent: Boolean
)

data class FocusTimerState(
    val ctaLabel: String,
    val durationLabel: String,
    val statusLabel: String,
    val totalSeconds: Int,
    val secondaryCtaLabel: String? = null,
    val isDndEnabled: Boolean = false,
    val isDndPermissionGranted: Boolean = false,
    val dndStatusLabel: String = "",
    val dndPermissionCtaLabel: String? = null
)

data class StudyBlockUiState(
    val timeLabel: String,
    val subject: String,
    val category: String,
    val difficultyLabel: String,
    val durationMinutes: Int,
    val emoji: String,
    val isActive: Boolean
)

data class TomorrowStudyPreview(
    val dayLabel: String,
    val morningSubject: String,
    val morningDuration: Int,
    val eveningSubject: String?,
    val eveningDuration: Int?
)

data class FocusSessionHistory(
    val sessionsToday: Int,
    val totalMinutesToday: Int
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
