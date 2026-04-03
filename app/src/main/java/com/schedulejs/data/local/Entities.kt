package com.schedulejs.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "day_templates")
data class DayTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dayType: String,
    val wakeUpTime: String
)

@Entity(
    tableName = "template_tasks",
    foreignKeys = [
        ForeignKey(
            entity = DayTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId")]
)
data class TemplateTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val title: String,
    val startTime: String,
    val endTime: String,
    val category: String,
    val details: String,
    val sortOrder: Int
)

@Entity(tableName = "study_rotations")
data class StudyRotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,
    val blockType: String,
    val subject: String,
    val notes: String
)

@Entity(tableName = "workout_rotations")
data class WorkoutRotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,
    val dayLabel: String,
    val isRestDay: Boolean,
    val routineTitle: String,
    val prescription: String,
    val note: String,
    val bellyRoutineStep: String,
    val sortOrder: Int
)

@Entity(tableName = "weekly_review_logs")
data class WeeklyReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reviewDate: String,
    val q1Covered: String,
    val q2Behind: String,
    val q3Tuition: String,
    val q4Energy: String,
    val q5Adjustment: String
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val notificationLeadTime: String,
    val transitAlertsEnabled: Boolean,
    val notificationsEducationDismissed: Boolean,
    val exactAlarmEducationDismissed: Boolean,
    val dndEducationDismissed: Boolean,
    val batteryOptimizationEducationDismissed: Boolean
)

@Entity(tableName = "daily_progress")
data class DailyProgressEntity(
    @PrimaryKey val date: String,
    val isWorkoutComplete: Boolean
)

@Entity(tableName = "focus_timer_state")
data class FocusTimerStateEntity(
    @PrimaryKey val id: Int = 1,
    val totalDurationSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val isCompleted: Boolean,
    val startedAtEpochMillis: Long?,
    val enableDnd: Boolean
)

@Entity(tableName = "belly_routine_state")
data class BellyRoutineStateEntity(
    @PrimaryKey val id: Int = 1,
    val accumulatedElapsedSeconds: Int,
    val isRunning: Boolean,
    val isCompleted: Boolean,
    val startedAtEpochMillis: Long?,
    val lastCueStepIndex: Int
)

@Entity(tableName = "nightly_checklists")
data class NightlyChecklistEntity(
    @PrimaryKey val date: String,
    val clothesLaidOut: Boolean,
    val lunchPrepped: Boolean,
    val bagPacked: Boolean,
    val completedAtEpochMillis: Long?
)
