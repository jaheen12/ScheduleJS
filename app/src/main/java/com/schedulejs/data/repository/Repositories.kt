package com.schedulejs.data.repository

import com.schedulejs.data.local.ScheduleDatabase
import com.schedulejs.data.local.SeedData
import com.schedulejs.domain.AppSettings
import com.schedulejs.domain.DayType
import com.schedulejs.domain.NotificationLeadTime
import com.schedulejs.domain.ReviewLog
import com.schedulejs.domain.ScheduleTask
import com.schedulejs.domain.StudyBlock
import com.schedulejs.domain.StudyBlockType
import com.schedulejs.domain.TaskCategory
import com.schedulejs.domain.TodaySchedule
import com.schedulejs.domain.WeeklyReviewState
import com.schedulejs.domain.WorkoutPlan
import com.schedulejs.domain.WorkoutRoutineItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ScheduleRepository {
    suspend fun getTodaySchedule(date: LocalDate): TodaySchedule
    suspend fun getTemplateSummaries(): List<Pair<String, String>>
}

interface WorkoutRepository {
    suspend fun getWorkoutForDate(date: LocalDate): WorkoutPlan
}

interface StudyRepository {
    suspend fun getStudyBlocksForDate(date: LocalDate): List<StudyBlock>
}

interface ReviewRepository {
    suspend fun getReviewState(dateTime: LocalDateTime): WeeklyReviewState
}

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
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
                summary = listOf(log.q1Covered, log.q2Behind, log.q5Adjustment).joinToString(" ")
            )
        }
        return WeeklyReviewState(
            requestedAt = dateTime,
            isUnlocked = isUnlocked,
            history = history
        )
    }
}

class OfflineSettingsRepository(
    private val database: ScheduleDatabase,
    private val seedData: SeedData
) : SettingsRepository {
    override fun observeSettings(): Flow<AppSettings> {
        return database.appSettingsDao().observeSettings().map { entity ->
            if (entity == null) {
                seedData.seedIfNeeded()
                AppSettings(
                    notificationLeadTime = NotificationLeadTime.FIVE_MINUTES,
                    transitAlertsEnabled = true
                )
            } else {
                AppSettings(
                    notificationLeadTime = NotificationLeadTime.fromStorage(entity.notificationLeadTime),
                    transitAlertsEnabled = entity.transitAlertsEnabled
                )
            }
        }
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
