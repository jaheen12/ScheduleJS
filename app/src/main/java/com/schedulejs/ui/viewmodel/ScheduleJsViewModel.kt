package com.schedulejs.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.schedulejs.data.local.ScheduleDatabase
import com.schedulejs.data.local.SeedData
import com.schedulejs.data.repository.OfflineReviewRepository
import com.schedulejs.data.repository.OfflineScheduleRepository
import com.schedulejs.data.repository.OfflineSettingsRepository
import com.schedulejs.data.repository.OfflineStudyRepository
import com.schedulejs.data.repository.OfflineWorkoutRepository
import com.schedulejs.data.repository.ReviewRepository
import com.schedulejs.data.repository.ScheduleRepository
import com.schedulejs.data.repository.SettingsRepository
import com.schedulejs.data.repository.StudyRepository
import com.schedulejs.data.repository.WorkoutRepository
import com.schedulejs.domain.NotificationLeadTime
import com.schedulejs.domain.ScheduleTask
import com.schedulejs.domain.StudyBlockType
import com.schedulejs.ui.BellyRoutineState
import com.schedulejs.ui.DashboardUiState
import com.schedulejs.ui.FocusTimerState
import com.schedulejs.ui.ReviewHistoryItem
import com.schedulejs.ui.ReviewQuestion
import com.schedulejs.ui.ReviewUiState
import com.schedulejs.ui.RoutineItem
import com.schedulejs.ui.SettingsUiState
import com.schedulejs.ui.TaskSnapshot
import com.schedulejs.ui.TemplateSummary
import com.schedulejs.ui.TimelineItem
import com.schedulejs.ui.TimelineItemState
import com.schedulejs.ui.StudyUiState
import com.schedulejs.ui.WorkoutUiState
import java.time.Clock
import java.time.LocalDateTime
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScheduleJsViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val workoutRepository: WorkoutRepository,
    private val studyRepository: StudyRepository,
    private val reviewRepository: ReviewRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {
    private val _dashboardState = MutableStateFlow(loadingDashboardState())
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _workoutState = MutableStateFlow(loadingWorkoutState())
    val workoutState: StateFlow<WorkoutUiState> = _workoutState.asStateFlow()

    private val _studyState = MutableStateFlow(loadingStudyState())
    val studyState: StateFlow<StudyUiState> = _studyState.asStateFlow()

    private val _reviewState = MutableStateFlow(loadingReviewState())
    val reviewState: StateFlow<ReviewUiState> = _reviewState.asStateFlow()

    private val _settingsState = MutableStateFlow(loadingSettingsState())
    val settingsState: StateFlow<SettingsUiState> = _settingsState.asStateFlow()

    init {
        refresh()
        observeSettings()
    }

    fun refresh(now: LocalDateTime = LocalDateTime.now(clock)) {
        viewModelScope.launch {
            val schedule = scheduleRepository.getTodaySchedule(now.toLocalDate())
            val workout = workoutRepository.getWorkoutForDate(now.toLocalDate())
            val studyBlocks = studyRepository.getStudyBlocksForDate(now.toLocalDate())
            val review = reviewRepository.getReviewState(now)
            val templateSummaries = scheduleRepository.getTemplateSummaries()

            _dashboardState.value = schedule.toDashboardUiState(now)
            _workoutState.value = WorkoutUiState(
                dayLabel = workout.dayLabel,
                routineItems = workout.routineItems.map { RoutineItem(it.title, it.prescription, it.note) },
                bellyRoutineState = BellyRoutineState(
                    ctaLabel = "Start 5-Min Belly Routine",
                    steps = workout.bellyRoutineSteps
                ),
                isWorkoutComplete = false
            )
            _studyState.value = StudyUiState(
                morningSubject = studyBlocks.firstOrNull { it.blockType == StudyBlockType.MORNING }?.subject ?: "No morning block",
                eveningSubject = studyBlocks.firstOrNull { it.blockType == StudyBlockType.EVENING }?.subject ?: "No evening block",
                focusTimerState = FocusTimerState(
                    ctaLabel = "Enter Deep Work",
                    durationLabel = if (schedule.dayType.name == "OFFICE_DAY") "90 min" else "60 min",
                    statusLabel = "Phase 2 now resolves subjects by date; timers arrive in Phase 3."
                ),
                reminderText = studyBlocks.firstOrNull { it.blockType == StudyBlockType.MORNING }?.notes
                    ?: "Solve board questions first."
            )
            _reviewState.value = ReviewUiState(
                isUnlocked = review.isUnlocked,
                questions = reviewQuestions(),
                historySummaries = review.history.map {
                    ReviewHistoryItem(
                        weekLabel = it.completedAt.toString(),
                        summary = it.summary
                    )
                }
            )
            _settingsState.value = _settingsState.value.copy(
                templateSummaries = templateSummaries.map { TemplateSummary(it.first, it.second) }
            )
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _settingsState.value = _settingsState.value.copy(
                    notificationLeadTime = settings.notificationLeadTime.toUiLabel(),
                    transitAlertsEnabled = settings.transitAlertsEnabled
                )
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val database = ScheduleDatabase.getInstance(context)
            val seedData = SeedData(database)
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ScheduleJsViewModel(
                        scheduleRepository = OfflineScheduleRepository(database, seedData),
                        workoutRepository = OfflineWorkoutRepository(database, seedData),
                        studyRepository = OfflineStudyRepository(database, seedData),
                        reviewRepository = OfflineReviewRepository(database, seedData),
                        settingsRepository = OfflineSettingsRepository(database, seedData)
                    ) as T
                }
            }
        }

        private fun loadingDashboardState(): DashboardUiState {
            return DashboardUiState(
                currentTask = TaskSnapshot("Loading schedule", "--", "Resolving today's template."),
                nextTask = TaskSnapshot("Loading", "--", "Preparing next block."),
                progressPercent = 0f,
                timelineItems = emptyList()
            )
        }

        private fun loadingWorkoutState(): WorkoutUiState {
            return WorkoutUiState(
                dayLabel = "Loading workout",
                routineItems = emptyList(),
                bellyRoutineState = BellyRoutineState(
                    ctaLabel = "Start 5-Min Belly Routine",
                    steps = emptyList()
                ),
                isWorkoutComplete = false
            )
        }

        private fun loadingStudyState(): StudyUiState {
            return StudyUiState(
                morningSubject = "Loading",
                eveningSubject = "Loading",
                focusTimerState = FocusTimerState(
                    ctaLabel = "Enter Deep Work",
                    durationLabel = "--",
                    statusLabel = "Resolving study rotation."
                ),
                reminderText = "Loading study notes."
            )
        }

        private fun loadingReviewState(): ReviewUiState {
            return ReviewUiState(
                isUnlocked = false,
                questions = reviewQuestions(),
                historySummaries = emptyList()
            )
        }

        private fun loadingSettingsState(): SettingsUiState {
            return SettingsUiState(
                notificationLeadTime = NotificationLeadTime.FIVE_MINUTES.toUiLabel(),
                transitAlertsEnabled = true,
                templateSummaries = emptyList()
            )
        }

        private fun reviewQuestions(): List<ReviewQuestion> {
            return listOf(
                ReviewQuestion("What did I fully cover this week?", "Topics, chapters, repetitions"),
                ReviewQuestion("Where did I fall behind?", "Missed blocks, skipped transitions"),
                ReviewQuestion("How did tuition affect the day?", "Timing, energy, prep quality"),
                ReviewQuestion("What drained my energy?", "Sleep, commute, friction points"),
                ReviewQuestion("What should change next week?", "One concrete adjustment")
            )
        }
    }
}

private fun NotificationLeadTime.toUiLabel(): String = displayLabel.lowercase().replaceFirstChar { it.uppercase() }

private fun com.schedulejs.domain.TodaySchedule.toDashboardUiState(now: LocalDateTime): DashboardUiState {
    val nowMinute = now.hour * 60 + now.minute
    val currentTask = tasks.firstOrNull { nowMinute in it.startMinuteOfDay until it.endMinuteOfDay }
    val nextTask = tasks.firstOrNull { it.startMinuteOfDay > nowMinute }
    val activeTask = when {
        currentTask != null -> currentTask
        tasks.isEmpty() -> null
        nowMinute < tasks.first().startMinuteOfDay -> null
        else -> tasks.last()
    }
    val progress = if (currentTask == null) {
        if (tasks.isNotEmpty() && nowMinute >= tasks.last().endMinuteOfDay) 1f else 0f
    } else {
        val duration = (currentTask.endMinuteOfDay - currentTask.startMinuteOfDay).coerceAtLeast(1)
        ((nowMinute - currentTask.startMinuteOfDay).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    return DashboardUiState(
        currentTask = activeTask.toCurrentSnapshot(nowMinute, currentTask != null),
        nextTask = if (nextTask == null) {
            TaskSnapshot(
                title = "No further blocks today",
                timeLabel = "--",
                subtitle = "End of schedule."
            )
        } else {
            TaskSnapshot(
                title = nextTask.title,
                timeLabel = nextTask.startMinuteOfDay.toClockLabel(),
                subtitle = nextTask.details
            )
        },
        progressPercent = progress,
        timelineItems = tasks.map { task ->
            val state = when {
                nowMinute >= task.endMinuteOfDay -> TimelineItemState.PAST
                nowMinute in task.startMinuteOfDay until task.endMinuteOfDay -> TimelineItemState.CURRENT
                else -> TimelineItemState.UPCOMING
            }
            TimelineItem(
                timeLabel = "${task.startMinuteOfDay.toClockLabel()} - ${task.endMinuteOfDay.toClockLabel()}",
                title = task.title,
                detail = task.details,
                state = state
            )
        }
    )
}

private fun ScheduleTask?.toCurrentSnapshot(nowMinute: Int, isLive: Boolean): TaskSnapshot {
    if (this == null) {
        return TaskSnapshot(
            title = "No active block",
            timeLabel = "--",
            subtitle = "Waiting for the first task of the day."
        )
    }
    val remainingMinutes = (endMinuteOfDay - nowMinute).coerceAtLeast(0)
    return TaskSnapshot(
        title = title,
        timeLabel = "${startMinuteOfDay.toClockLabel()} - ${endMinuteOfDay.toClockLabel()}",
        subtitle = if (isLive) {
            "Remaining: ${remainingMinutes.toDurationLabel()}"
        } else {
            "Current block resolved from today's template."
        }
    )
}

private fun Int.toClockLabel(): String {
    val hours = this / 60
    val minutes = this % 60
    return "%02d:%02d".format(hours, minutes)
}

private fun Int.toDurationLabel(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes.roundToInt()}m"
    }
}
