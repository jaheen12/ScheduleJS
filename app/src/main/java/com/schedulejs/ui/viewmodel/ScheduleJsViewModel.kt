package com.schedulejs.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.schedulejs.data.local.ScheduleDatabase
import com.schedulejs.data.local.SeedData
import com.schedulejs.data.repository.InteractiveStateRepository
import com.schedulejs.data.repository.OfflineInteractiveStateRepository
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
import com.schedulejs.domain.BellyRoutineSession
import com.schedulejs.domain.DashboardSnapshot
import com.schedulejs.domain.DayType
import com.schedulejs.domain.FocusTimerSession
import com.schedulejs.domain.NotificationLeadTime
import com.schedulejs.domain.ScheduleTask
import com.schedulejs.domain.StudyBlock
import com.schedulejs.domain.StudyBlockType
import com.schedulejs.domain.TimerStatus
import com.schedulejs.domain.TodaySchedule
import com.schedulejs.domain.WorkoutPlan
import com.schedulejs.services.DefaultTimeEngine
import com.schedulejs.services.FocusTimerController
import com.schedulejs.services.RoomFocusTimerController
import com.schedulejs.services.RoomRoutineTimerController
import com.schedulejs.services.RoutineTimerController
import com.schedulejs.services.TimeEngine
import com.schedulejs.services.ToneRoutineCuePlayer
import com.schedulejs.ui.BellyRoutineState
import com.schedulejs.ui.DashboardUiState
import com.schedulejs.ui.FocusTimerState
import com.schedulejs.ui.ReviewHistoryItem
import com.schedulejs.ui.ReviewQuestion
import com.schedulejs.ui.ReviewUiState
import com.schedulejs.ui.RoutineItem
import com.schedulejs.ui.SettingsUiState
import com.schedulejs.ui.StudyUiState
import com.schedulejs.ui.TaskSnapshot
import com.schedulejs.ui.TemplateSummary
import com.schedulejs.ui.TimelineItem
import com.schedulejs.ui.TimelineItemState
import com.schedulejs.ui.WorkoutUiState
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScheduleJsViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val workoutRepository: WorkoutRepository,
    private val studyRepository: StudyRepository,
    private val reviewRepository: ReviewRepository,
    private val settingsRepository: SettingsRepository,
    private val interactiveStateRepository: InteractiveStateRepository,
    private val timeEngine: TimeEngine,
    private val focusTimerController: FocusTimerController,
    private val routineTimerController: RoutineTimerController,
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

    private var loadedDate: LocalDate? = null
    private var loadedSchedule: TodaySchedule? = null
    private var loadedWorkout: WorkoutPlan? = null
    private var loadedStudyBlocks: List<StudyBlock> = emptyList()
    private var loadedTemplateSummaries: List<TemplateSummary> = emptyList()

    init {
        observeSettings()
        startTicker()
    }

    fun onBellyRoutineAction() {
        viewModelScope.launch {
            when (routineTimerController.getState().status) {
                TimerStatus.IDLE,
                TimerStatus.COMPLETED -> routineTimerController.startBellyRoutine()
                TimerStatus.RUNNING -> routineTimerController.pause()
                TimerStatus.PAUSED -> routineTimerController.resume()
            }
            syncUi()
        }
    }

    fun cancelBellyRoutine() {
        viewModelScope.launch {
            routineTimerController.cancel()
            syncUi()
        }
    }

    fun onFocusTimerAction() {
        viewModelScope.launch {
            val durationMinutes = defaultFocusDurationMinutes(loadedSchedule?.dayType ?: DayType.CLASS_DAY)
            when (focusTimerController.getState().status) {
                TimerStatus.IDLE,
                TimerStatus.COMPLETED -> focusTimerController.start(durationMinutes, enableDnd = false)
                TimerStatus.RUNNING -> focusTimerController.pause()
                TimerStatus.PAUSED -> focusTimerController.resume()
            }
            syncUi()
        }
    }

    fun cancelFocusTimer() {
        viewModelScope.launch {
            focusTimerController.cancel()
            syncUi()
        }
    }

    fun toggleWorkoutComplete() {
        viewModelScope.launch {
            val date = LocalDate.now(clock)
            val isComplete = interactiveStateRepository.isWorkoutComplete(date)
            interactiveStateRepository.setWorkoutComplete(date, !isComplete)
            syncUi()
        }
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (isActive) {
                syncUi()
                delay(1_000)
            }
        }
    }

    private suspend fun syncUi() {
        val now = LocalDateTime.now(clock)
        ensureStaticDataLoaded(now.toLocalDate())

        val schedule = loadedSchedule ?: return
        val workout = loadedWorkout ?: return
        val focusSession = focusTimerController.getState()
        val bellySession = routineTimerController.getState()
        val review = reviewRepository.getReviewState(now)
        val workoutComplete = interactiveStateRepository.isWorkoutComplete(now.toLocalDate())
        val dashboardSnapshot = timeEngine.getDashboardSnapshot(schedule, now)

        _dashboardState.value = schedule.toDashboardUiState(dashboardSnapshot, now)
        _workoutState.value = workout.toWorkoutUiState(bellySession, workoutComplete)
        _studyState.value = loadedStudyBlocks.toStudyUiState(schedule.dayType, focusSession)
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
            templateSummaries = loadedTemplateSummaries
        )
    }

    private suspend fun ensureStaticDataLoaded(date: LocalDate) {
        if (loadedDate == date && loadedSchedule != null && loadedWorkout != null) {
            return
        }

        loadedDate = date
        loadedSchedule = scheduleRepository.getTodaySchedule(date)
        loadedWorkout = workoutRepository.getWorkoutForDate(date)
        loadedStudyBlocks = studyRepository.getStudyBlocksForDate(date)
        loadedTemplateSummaries = scheduleRepository.getTemplateSummaries().map {
            TemplateSummary(it.first, it.second)
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.observeSettings().collectLatest { settings ->
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
            val clock = Clock.systemDefaultZone()
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ScheduleJsViewModel(
                        scheduleRepository = OfflineScheduleRepository(database, seedData),
                        workoutRepository = OfflineWorkoutRepository(database, seedData),
                        studyRepository = OfflineStudyRepository(database, seedData),
                        reviewRepository = OfflineReviewRepository(database, seedData),
                        settingsRepository = OfflineSettingsRepository(database, seedData),
                        interactiveStateRepository = OfflineInteractiveStateRepository(database, seedData),
                        timeEngine = DefaultTimeEngine(),
                        focusTimerController = RoomFocusTimerController(database.focusTimerDao(), clock),
                        routineTimerController = RoomRoutineTimerController(
                            database.bellyRoutineDao(),
                            ToneRoutineCuePlayer(),
                            clock
                        ),
                        clock = clock
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
                    ctaLabel = "Start Belly Routine",
                    steps = emptyList(),
                    statusLabel = "Loading timer state."
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
                    statusLabel = "Loading timer state."
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

        private fun defaultFocusDurationMinutes(dayType: DayType): Int {
            return if (dayType == DayType.OFFICE_DAY) 90 else 60
        }
    }
}

private fun NotificationLeadTime.toUiLabel(): String = displayLabel.lowercase().replaceFirstChar { it.uppercase() }

private fun TodaySchedule.toDashboardUiState(
    snapshot: DashboardSnapshot,
    now: LocalDateTime
): DashboardUiState {
    val nowMinute = now.hour * 60 + now.minute
    return DashboardUiState(
        currentTask = snapshot.currentTask.toCurrentSnapshot(nowMinute, snapshot.currentTask != null && nowMinute < (snapshot.currentTask?.endMinuteOfDay ?: 0)),
        nextTask = snapshot.nextTask.toNextSnapshot(),
        progressPercent = snapshot.progressPercent,
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

private fun WorkoutPlan.toWorkoutUiState(
    bellySession: BellyRoutineSession,
    isWorkoutComplete: Boolean
): WorkoutUiState {
    val formattedSteps = bellyRoutineSteps.mapIndexed { index, step ->
        when {
            bellySession.status == TimerStatus.RUNNING && index == bellySession.currentStepIndex -> "Now: $step"
            bellySession.status == TimerStatus.PAUSED && index == bellySession.currentStepIndex -> "Paused: $step"
            bellySession.status == TimerStatus.COMPLETED && index == bellyRoutineSteps.lastIndex -> "Complete: $step"
            else -> step
        }
    }

    return WorkoutUiState(
        dayLabel = dayLabel,
        routineItems = routineItems.map { RoutineItem(it.title, it.prescription, it.note) },
        bellyRoutineState = BellyRoutineState(
            ctaLabel = when (bellySession.status) {
                TimerStatus.IDLE,
                TimerStatus.COMPLETED -> "Start Belly Routine"
                TimerStatus.RUNNING -> "Pause Belly Routine"
                TimerStatus.PAUSED -> "Resume Belly Routine"
            },
            steps = formattedSteps,
            statusLabel = when (bellySession.status) {
                TimerStatus.IDLE -> "Ready for the first step."
                TimerStatus.RUNNING -> "Step ${bellySession.currentStepIndex + 1} live • ${bellySession.stepRemainingSeconds.toTimerDurationLabel()} left in this step."
                TimerStatus.PAUSED -> "Paused on step ${bellySession.currentStepIndex + 1} • ${bellySession.remainingSeconds.toTimerDurationLabel()} left."
                TimerStatus.COMPLETED -> "Belly routine complete for this session."
            },
            secondaryCtaLabel = when (bellySession.status) {
                TimerStatus.RUNNING,
                TimerStatus.PAUSED -> "Cancel Routine"
                else -> null
            }
        ),
        isWorkoutComplete = isWorkoutComplete
    )
}

private fun List<StudyBlock>.toStudyUiState(
    dayType: DayType,
    focusSession: FocusTimerSession
): StudyUiState {
    val morningBlock = firstOrNull { it.blockType == StudyBlockType.MORNING }
    val eveningBlock = firstOrNull { it.blockType == StudyBlockType.EVENING }
    val defaultMinutes = if (dayType == DayType.OFFICE_DAY) 90 else 60
    val displaySeconds = when (focusSession.status) {
        TimerStatus.IDLE -> defaultMinutes * 60
        else -> focusSession.remainingSeconds
    }
    return StudyUiState(
        morningSubject = morningBlock?.subject ?: "No morning block",
        eveningSubject = eveningBlock?.subject ?: "No evening block",
        focusTimerState = FocusTimerState(
            ctaLabel = when (focusSession.status) {
                TimerStatus.IDLE,
                TimerStatus.COMPLETED -> "Enter Deep Work"
                TimerStatus.RUNNING -> "Pause Focus Timer"
                TimerStatus.PAUSED -> "Resume Focus Timer"
            },
            durationLabel = displaySeconds.toTimerDurationLabel(),
            statusLabel = when (focusSession.status) {
                TimerStatus.IDLE -> "Ready to start ${defaultMinutes} minutes of focus work."
                TimerStatus.RUNNING -> "Focus timer is running${if (focusSession.enableDnd) " with DND planned for Phase 4." else "."}"
                TimerStatus.PAUSED -> "Focus timer paused with ${focusSession.remainingSeconds.toTimerDurationLabel()} remaining."
                TimerStatus.COMPLETED -> "Focus session completed."
            },
            secondaryCtaLabel = when (focusSession.status) {
                TimerStatus.RUNNING,
                TimerStatus.PAUSED -> "Cancel Timer"
                else -> null
            }
        ),
        reminderText = morningBlock?.notes ?: "Solve board questions first."
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
            "Remaining: ${remainingMinutes.toMinuteDurationLabel()}"
        } else {
            "Current block resolved from today's template."
        }
    )
}

private fun ScheduleTask?.toNextSnapshot(): TaskSnapshot {
    return if (this == null) {
        TaskSnapshot(
            title = "No further blocks today",
            timeLabel = "--",
            subtitle = "End of schedule."
        )
    } else {
        TaskSnapshot(
            title = title,
            timeLabel = startMinuteOfDay.toClockLabel(),
            subtitle = details
        )
    }
}

private fun Int.toClockLabel(): String {
    val hours = this / 60
    val minutes = this % 60
    return "%02d:%02d".format(hours, minutes)
}

private fun Int.toMinuteDurationLabel(): String {
    val totalMinutes = this.coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

private fun Int.toTimerDurationLabel(): String {
    val totalSeconds = this.coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}
