package com.schedulejs.data.repository

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.room.withTransaction
import com.schedulejs.data.local.ScheduleDatabase
import com.schedulejs.data.local.SeedData
import com.schedulejs.data.local.TemplateTaskEntity
import com.schedulejs.data.local.WeeklyReviewLogEntity
import com.schedulejs.domain.AppSettings
import com.schedulejs.domain.DayTemplateDraft
import com.schedulejs.domain.DayType
import com.schedulejs.domain.NotificationLeadTime
import com.schedulejs.domain.PermissionEducationState
import com.schedulejs.domain.ReviewEntryDraft
import com.schedulejs.domain.ReviewLog
import com.schedulejs.domain.ScheduleTask
import com.schedulejs.domain.StudyBlock
import com.schedulejs.domain.StudyBlockType
import com.schedulejs.domain.TaskCategory
import com.schedulejs.domain.TemplateTaskDraft
import com.schedulejs.domain.TodaySchedule
import com.schedulejs.domain.ValidationError
import com.schedulejs.domain.WeeklyReviewState
import com.schedulejs.domain.WorkoutPlan
import com.schedulejs.domain.WorkoutRoutineItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface ScheduleRepository {
    suspend fun getTodaySchedule(date: LocalDate): TodaySchedule
    suspend fun getTemplateSummaries(): List<Pair<String, String>>
    suspend fun getEditableTemplates(): List<DayTemplateDraft>
    suspend fun updateTemplates(templates: List<DayTemplateDraft>): List<ValidationError>
}

interface WorkoutRepository {
    suspend fun getWorkoutForDate(date: LocalDate): WorkoutPlan
}

interface StudyRepository {
    suspend fun getStudyBlocksForDate(date: LocalDate): List<StudyBlock>
}

interface ReviewRepository {
    suspend fun getReviewState(dateTime: LocalDateTime): WeeklyReviewState
    suspend fun saveReview(date: LocalDate, draft: ReviewEntryDraft): List<ValidationError>
}

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun updateSettings(notificationLeadTime: NotificationLeadTime, transitAlertsEnabled: Boolean)
    suspend fun dismissPermissionEducation(cardId: PermissionEducationCard)
}

interface InteractiveStateRepository {
    suspend fun isWorkoutComplete(date: LocalDate): Boolean
    suspend fun setWorkoutComplete(date: LocalDate, isComplete: Boolean)
}

class OfflineScheduleRepository(
    private val database: ScheduleDatabase,
    private val seedData: SeedData
) : ScheduleRepository {
    override suspend fun getTodaySchedule(date: LocalDate): TodaySchedule {
        seedData.seedIfNeeded()
        val dayType = date.toDayType()
        val template = requireNotNull(database.dayTemplateDao().getByDayType(dayType.name))
        val tasks = database.templateTaskDao().getForTemplate(template.id).map { entity ->
            ScheduleTask(
                id = entity.id,
                title = entity.title,
                startMinuteOfDay = entity.startTime.toMinuteOfDay(),
                endMinuteOfDay = entity.endTime.toMinuteOfDay(),
                category = TaskCategory.valueOf(entity.category),
                details = entity.details,
                dayType = dayType
            )
        }
        return TodaySchedule(
            date = date,
            dayType = dayType,
            tasks = tasks
        )
    }

    override suspend fun getTemplateSummaries(): List<Pair<String, String>> {
        seedData.seedIfNeeded()
        return database.dayTemplateDao().getAll().map { template ->
            val tasks = database.templateTaskDao().getForTemplate(template.id)
            val summary = when (DayType.valueOf(template.dayType)) {
                DayType.CLASS_DAY -> "College -> 35-minute sprint -> office -> tuition"
                DayType.OFFICE_DAY -> "Morning study -> office -> tuition -> evening reset"
                DayType.FRIDAY -> "Office flow with review unlock at 15:30"
            }
            template.title to "$summary (${tasks.size} blocks)"
        }
    }

    override suspend fun getEditableTemplates(): List<DayTemplateDraft> {
        seedData.seedIfNeeded()
        return database.dayTemplateDao().getAll().map { template ->
            DayTemplateDraft(
                id = template.id,
                title = template.title,
                dayType = DayType.valueOf(template.dayType),
                wakeUpTime = template.wakeUpTime,
                tasks = database.templateTaskDao().getForTemplate(template.id).map { task ->
                    TemplateTaskDraft(
                        id = task.id,
                        title = task.title,
                        startTime = task.startTime,
                        endTime = task.endTime,
                        category = TaskCategory.valueOf(task.category),
                        details = task.details,
                        sortOrder = task.sortOrder
                    )
                }
            )
        }
    }

    override suspend fun updateTemplates(templates: List<DayTemplateDraft>): List<ValidationError> {
        seedData.seedIfNeeded()
        val errors = templates.flatMap(::validateTemplate)
        if (errors.isNotEmpty()) return errors

        database.withTransaction {
            templates.forEach { template ->
                database.dayTemplateDao().update(
                    com.schedulejs.data.local.DayTemplateEntity(
                        id = template.id,
                        title = template.title.trim(),
                        dayType = template.dayType.name,
                        wakeUpTime = template.wakeUpTime
                    )
                )
                database.templateTaskDao().deleteForTemplate(template.id)
                database.templateTaskDao().insertAll(
                    template.tasks.sortedBy { it.sortOrder }.mapIndexed { index, task ->
                        TemplateTaskEntity(
                            templateId = template.id,
                            title = task.title.trim(),
                            startTime = task.startTime,
                            endTime = task.endTime,
                            category = task.category.name,
                            details = task.details.trim(),
                            sortOrder = index
                        )
                    }
                )
            }
        }
        return emptyList()
    }

    private fun validateTemplate(template: DayTemplateDraft): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        if (template.title.isBlank()) {
            errors += ValidationError("template:${template.id}:title", "Template title is required.")
        }
        if (!template.wakeUpTime.isValidTime()) {
            errors += ValidationError("template:${template.id}:wakeUpTime", "Wake-up time must use HH:mm.")
        }

        val normalizedTasks = template.tasks.sortedBy { it.sortOrder }
        normalizedTasks.forEachIndexed { index, task ->
            if (task.title.isBlank()) {
                errors += ValidationError("task:${task.id}:title", "Task ${index + 1} needs a title.")
            }
            if (!task.startTime.isValidTime() || !task.endTime.isValidTime()) {
                errors += ValidationError("task:${task.id}:time", "Task ${index + 1} must use HH:mm times.")
            } else {
                val start = task.startTime.toMinuteOfDay()
                val end = task.endTime.toMinuteOfDay()
                if (start >= end) {
                    errors += ValidationError("task:${task.id}:order", "Task ${index + 1} must start before it ends.")
                }
            }
        }

        normalizedTasks.zipWithNext().forEachIndexed { index, pair ->
            val current = pair.first
            val next = pair.second
            if (current.startTime.isValidTime() && current.endTime.isValidTime() && next.startTime.isValidTime()) {
                if (current.endTime.toMinuteOfDay() > next.startTime.toMinuteOfDay()) {
                    errors += ValidationError(
                        "template:${template.id}:overlap:$index",
                        "Tasks in ${template.title} overlap around ${current.title} and ${next.title}."
                    )
                }
            }
        }
        return errors
    }
}

class OfflineWorkoutRepository(
    private val database: ScheduleDatabase,
    private val seedData: SeedData
) : WorkoutRepository {
    override suspend fun getWorkoutForDate(date: LocalDate): WorkoutPlan {
        seedData.seedIfNeeded()
        val rows = database.workoutRotationDao().getForDay(date.dayOfWeek.value)
        val first = rows.first()
        return WorkoutPlan(
            dayOfWeek = date.dayOfWeek,
            dayLabel = first.dayLabel,
            isRestDay = first.isRestDay,
            routineItems = rows.map {
                WorkoutRoutineItem(
                    title = it.routineTitle,
                    prescription = it.prescription,
                    note = it.note
                )
            },
            bellyRoutineSteps = rows.mapNotNull { step -> step.bellyRoutineStep.takeIf { it.isNotBlank() } }.distinct()
        )
    }
}

class OfflineStudyRepository(
    private val database: ScheduleDatabase,
    private val seedData: SeedData
) : StudyRepository {
    override suspend fun getStudyBlocksForDate(date: LocalDate): List<StudyBlock> {
        seedData.seedIfNeeded()
        return database.studyRotationDao().getForDay(date.dayOfWeek.value).map { entity ->
            StudyBlock(
                dayOfWeek = DayOfWeek.of(entity.dayOfWeek),
                blockType = StudyBlockType.valueOf(entity.blockType),
                subject = entity.subject,
                notes = entity.notes
            )
        }
    }
}

class OfflineReviewRepository(
    private val database: ScheduleDatabase,
    private val seedData: SeedData
) : ReviewRepository {
    override suspend fun getReviewState(dateTime: LocalDateTime): WeeklyReviewState {
        seedData.seedIfNeeded()
        val isUnlocked = dateTime.dayOfWeek == DayOfWeek.FRIDAY &&
            !dateTime.toLocalTime().isBefore(LocalTime.of(15, 30))
        val history = database.weeklyReviewLogDao().getAll().map { log ->
            ReviewLog(
                completedAt = LocalDate.parse(log.reviewDate),
                covered = log.q1Covered,
                behind = log.q2Behind,
                tuition = log.q3Tuition,
                energy = log.q4Energy,
                adjustment = log.q5Adjustment,
                summary = listOf(log.q1Covered, log.q2Behind, log.q5Adjustment).joinToString(" ")
            )
        }
        return WeeklyReviewState(
            requestedAt = dateTime,
            isUnlocked = isUnlocked,
            history = history
        )
    }

    override suspend fun saveReview(date: LocalDate, draft: ReviewEntryDraft): List<ValidationError> {
        seedData.seedIfNeeded()
        val errors = buildList {
            if (draft.covered.isBlank()) add(ValidationError("review:covered", "Covered this week is required."))
            if (draft.behind.isBlank()) add(ValidationError("review:behind", "Behind this week is required."))
            if (draft.tuition.isBlank()) add(ValidationError("review:tuition", "Tuition reflection is required."))
            if (draft.energy.isBlank()) add(ValidationError("review:energy", "Energy reflection is required."))
            if (draft.adjustment.isBlank()) add(ValidationError("review:adjustment", "Adjustment is required."))
        }
        if (errors.isNotEmpty()) return errors

        database.weeklyReviewLogDao().insert(
            WeeklyReviewLogEntity(
                reviewDate = date.toString(),
                q1Covered = draft.covered.trim(),
                q2Behind = draft.behind.trim(),
                q3Tuition = draft.tuition.trim(),
                q4Energy = draft.energy.trim(),
                q5Adjustment = draft.adjustment.trim()
            )
        )
        return emptyList()
    }
}

class OfflineSettingsRepository(
    private val database: ScheduleDatabase,
    private val seedData: SeedData,
    private val context: Context
) : SettingsRepository {
    override fun observeSettings(): Flow<AppSettings> {
        return database.appSettingsDao().observeSettings().map { entity ->
            if (entity == null) {
                seedData.seedIfNeeded()
                AppSettings(
                    notificationLeadTime = NotificationLeadTime.FIVE_MINUTES,
                    transitAlertsEnabled = true,
                    permissionEducation = permissionEducationState(
                        notificationsEducationDismissed = false,
                        exactAlarmEducationDismissed = false,
                        dndEducationDismissed = false,
                        batteryOptimizationEducationDismissed = false
                    )
                )
            } else {
                AppSettings(
                    notificationLeadTime = NotificationLeadTime.fromStorage(entity.notificationLeadTime),
                    transitAlertsEnabled = entity.transitAlertsEnabled,
                    permissionEducation = permissionEducationState(
                        notificationsEducationDismissed = entity.notificationsEducationDismissed,
                        exactAlarmEducationDismissed = entity.exactAlarmEducationDismissed,
                        dndEducationDismissed = entity.dndEducationDismissed,
                        batteryOptimizationEducationDismissed = entity.batteryOptimizationEducationDismissed
                    )
                )
            }
        }
    }

    override suspend fun updateSettings(
        notificationLeadTime: NotificationLeadTime,
        transitAlertsEnabled: Boolean
    ) {
        seedData.seedIfNeeded()
        val current = database.appSettingsDao().observeSettings().map { entity ->
            entity ?: com.schedulejs.data.local.AppSettingsEntity(
                notificationLeadTime = NotificationLeadTime.FIVE_MINUTES.name,
                transitAlertsEnabled = true,
                notificationsEducationDismissed = false,
                exactAlarmEducationDismissed = false,
                dndEducationDismissed = false,
                batteryOptimizationEducationDismissed = false
            )
        }.first()
        database.appSettingsDao().upsert(
            current.copy(
                notificationLeadTime = notificationLeadTime.name,
                transitAlertsEnabled = transitAlertsEnabled
            )
        )
    }

    override suspend fun dismissPermissionEducation(cardId: PermissionEducationCard) {
        seedData.seedIfNeeded()
        val current = database.appSettingsDao().observeSettings().map { entity ->
            entity ?: com.schedulejs.data.local.AppSettingsEntity(
                notificationLeadTime = NotificationLeadTime.FIVE_MINUTES.name,
                transitAlertsEnabled = true,
                notificationsEducationDismissed = false,
                exactAlarmEducationDismissed = false,
                dndEducationDismissed = false,
                batteryOptimizationEducationDismissed = false
            )
        }.first()
        val updated = when (cardId) {
            PermissionEducationCard.NOTIFICATIONS -> current.copy(notificationsEducationDismissed = true)
            PermissionEducationCard.EXACT_ALARMS -> current.copy(exactAlarmEducationDismissed = true)
            PermissionEducationCard.DND -> current.copy(dndEducationDismissed = true)
            PermissionEducationCard.BATTERY_OPTIMIZATION -> current.copy(batteryOptimizationEducationDismissed = true)
        }
        database.appSettingsDao().upsert(updated)
    }

    private fun permissionEducationState(
        notificationsEducationDismissed: Boolean,
        exactAlarmEducationDismissed: Boolean,
        dndEducationDismissed: Boolean,
        batteryOptimizationEducationDismissed: Boolean
    ): PermissionEducationState {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val exactAlarmsNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()
        val batteryOptimizationNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !powerManager.isIgnoringBatteryOptimizations(context.packageName)
        return PermissionEducationState(
            shouldShowNotificationsCard = !notificationsEducationDismissed,
            shouldShowExactAlarmsCard = exactAlarmsNeeded && !exactAlarmEducationDismissed,
            shouldShowDndCard = !dndEducationDismissed,
            shouldShowBatteryOptimizationCard = batteryOptimizationNeeded && !batteryOptimizationEducationDismissed
        )
    }
}

class OfflineInteractiveStateRepository(
    private val database: ScheduleDatabase,
    private val seedData: SeedData
) : InteractiveStateRepository {
    override suspend fun isWorkoutComplete(date: LocalDate): Boolean {
        seedData.seedIfNeeded()
        return database.dailyProgressDao().getForDate(date.toString())?.isWorkoutComplete ?: false
    }

    override suspend fun setWorkoutComplete(date: LocalDate, isComplete: Boolean) {
        seedData.seedIfNeeded()
        database.dailyProgressDao().upsert(
            com.schedulejs.data.local.DailyProgressEntity(
                date = date.toString(),
                isWorkoutComplete = isComplete
            )
        )
    }
}

private fun LocalDate.toDayType(): DayType {
    return when (dayOfWeek) {
        DayOfWeek.SUNDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.THURSDAY -> DayType.CLASS_DAY
        DayOfWeek.FRIDAY -> DayType.FRIDAY
        else -> DayType.OFFICE_DAY
    }
}

private fun String.toMinuteOfDay(): Int {
    val time = LocalTime.parse(this)
    return time.hour * 60 + time.minute
}

private fun String.isValidTime(): Boolean {
    return runCatching { LocalTime.parse(this) }.isSuccess
}

enum class PermissionEducationCard {
    NOTIFICATIONS,
    EXACT_ALARMS,
    DND,
    BATTERY_OPTIMIZATION
}
