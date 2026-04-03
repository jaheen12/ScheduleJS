package com.schedulejs.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

enum class DayType {
    CLASS_DAY,
    OFFICE_DAY,
    FRIDAY
}

enum class TaskCategory {
    ROUTINE,
    WORKOUT,
    STUDY,
    TRANSIT,
    COLLEGE,
    OFFICE,
    TUITION,
    MEAL,
    PREP,
    REVIEW,
    REST,
    PERSONAL,
    SLEEP
}

enum class StudyBlockType {
    MORNING,
    EVENING
}

enum class NotificationLeadTime(val displayLabel: String) {
    ON_TIME("On time"),
    FIVE_MINUTES("5 minutes before"),
    TEN_MINUTES("10 minutes before");

    companion object {
        fun fromStorage(value: String): NotificationLeadTime {
            return entries.firstOrNull { it.name == value } ?: FIVE_MINUTES
        }
    }
}

data class ScheduleTask(
    val id: Long,
    val title: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val category: TaskCategory,
    val details: String,
    val dayType: DayType
)

data class StudyBlock(
    val dayOfWeek: DayOfWeek,
    val blockType: StudyBlockType,
    val subject: String,
    val notes: String
)

data class WorkoutRoutineItem(
    val title: String,
    val prescription: String,
    val note: String
)

data class WorkoutPlan(
    val dayOfWeek: DayOfWeek,
    val dayLabel: String,
    val isRestDay: Boolean,
    val routineItems: List<WorkoutRoutineItem>,
    val bellyRoutineSteps: List<String>
)

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class DashboardSnapshot(
    val currentTask: ScheduleTask?,
    val nextTask: ScheduleTask?,
    val progressPercent: Float
)

data class FocusTimerSession(
    val status: TimerStatus,
    val totalDurationSeconds: Int,
    val remainingSeconds: Int,
    val enableDnd: Boolean
)

data class BellyRoutineSession(
    val status: TimerStatus,
    val totalDurationSeconds: Int,
    val remainingSeconds: Int,
    val currentStepIndex: Int,
    val stepRemainingSeconds: Int
)

data class ReviewLog(
    val completedAt: LocalDate,
    val summary: String
)

data class WeeklyReviewState(
    val requestedAt: LocalDateTime,
    val isUnlocked: Boolean,
    val history: List<ReviewLog>
)

data class AppSettings(
    val notificationLeadTime: NotificationLeadTime,
    val transitAlertsEnabled: Boolean
)

data class TodaySchedule(
    val date: LocalDate,
    val dayType: DayType,
    val tasks: List<ScheduleTask>
)
