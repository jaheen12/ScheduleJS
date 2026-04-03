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
import com.schedulejs.domain.StudyBlock
import com.schedulejs.domain.StudyBlockType
import com.schedulejs.domain.TimerStatus
import com.schedulejs.domain.TodaySchedule
import com.schedulejs.domain.WorkoutPlan
import com.schedulejs.services.DefaultTimeEngine
import com.schedulejs.services.DashboardLiveContentFactory
import com.schedulejs.services.FocusModeController
import com.schedulejs.services.FocusTimerController
import com.schedulejs.services.NotificationPolicyFocusModeController
import com.schedulejs.services.RoomFocusTimerController
import com.schedulejs.services.RoomRoutineTimerController
import com.schedulejs.services.RoutineTimerController
import com.schedulejs.services.TimeEngine
import com.schedulejs.services.ToneRoutineCuePlayer
import com.schedulejs.services.toClockLabel
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
    private val focusModeController: FocusModeController,
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
    private var focusModeEnabled = false

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
            val shouldEnableDnd = focusModeEnabled && focusModeController.hasNotificationPolicyAccess()
            when (focusTimerController.getState().status) {
                TimerStatus.IDLE,
                TimerStatus.COMPLETED -> focusTimerController.start(durationMinutes, enableDnd = shouldEnableDnd)
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

    fun toggleFocusMode() {
        viewModelScope.launch {
            if (!focusModeController.hasNotificationPolicyAccess()) {
                focusModeEnabled = false
            } else {
                focusModeEnabled = !focusModeEnabled
            }
            syncUi()
        }
    }

    fun buildDndPermissionIntent() = focusModeController.buildPermissionIntent()

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
        val dndAccessGranted = focusModeController.hasNotificationPolicyAccess()
        if (focusSession.status == TimerStatus.RUNNING || focusSession.status == TimerStatus.PAUSED) {
            focusModeEnabled = focusSession.enableDnd
        } else if (!dndAccessGranted) {
            focusModeEnabled = false
        }

        _dashboardState.value = schedule.toDashboardUiState(dashboardSnapshot, now)
        _workoutState.value = workout.toWorkoutUiState(bellySession, workoutComplete)
        _studyState.value = loadedStudyBlocks.toStudyUiState(
            dayType = schedule.dayType,
            focusSession = focusSession,
            focusModeEnabled = focusModeEnabled,
            dndAccessGranted = dndAccessGranted
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
            val focusModeController = NotificationPolicyFocusModeController(context)
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
                        focusTimerController = RoomFocusTimerController(
                            database.focusTimerDao(),
                            focusModeController,
                            clock
                        ),
                        focusModeController = focusModeController,
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
                    statusLabel = "Loading timer state.",
                    dndStatusLabel = "Checking Do Not Disturb access."
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
    val liveContent = DashboardLiveContentFactory.create(this, snapshot, now)
    val nowMinute = now.hour * 60 + now.minute
    return DashboardUiState(
        currentTask = TaskSnapshot(
            title = liveContent.currentTitle,
            timeLabel = liveContent.currentTimeLabel,
            subtitle = liveContent.currentSubtitle
        ),
        nextTask = TaskSnapshot(
            title = liveContent.nextTitle,
            timeLabel = liveContent.nextTimeLabel,
            subtitle = liveContent.nextSubtitle
        ),
        progressPercent = liveContent.progressPercent,
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
    focusSession: FocusTimerSession,
    focusModeEnabled: Boolean,
    dndAccessGranted: Boolean
): StudyUiState {
    val morningBlock = firstOrNull { it.blockType == StudyBlockType.MORNING }
    val eveningBlock = firstOrNull { it.blockType == StudyBlockType.EVENING }
    val defaultMinutes = if (dayType == DayType.OFFICE_DAY) 90 else 60
    val displaySeconds = when (focusSession.status) {
        TimerStatus.IDLE -> defaultMinutes * 60
        else -> focusSession.remainingSeconds
    }
    val effectiveFocusMode = if (focusSession.status == TimerStatus.RUNNING || focusSession.status == TimerStatus.PAUSED) {
        focusSession.enableDnd
    } else {
        focusModeEnabled && dndAccessGranted
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
                TimerStatus.RUNNING -> "Focus timer is running${if (focusSession.enableDnd) " with Do Not Disturb active." else "."}"
                TimerStatus.PAUSED -> "Focus timer paused with ${focusSession.remainingSeconds.toTimerDurationLabel()} remaining."
                TimerStatus.COMPLETED -> "Focus session completed."
            },
            secondaryCtaLabel = when (focusSession.status) {
                TimerStatus.RUNNING,
                TimerStatus.PAUSED -> "Cancel Timer"
                else -> null
            },
            isDndEnabled = effectiveFocusMode,
            isDndPermissionGranted = dndAccessGranted,
            dndStatusLabel = when {
                !dndAccessGranted -> "Grant Do Not Disturb access to silence interruptions during focus sessions."
                focusSession.status == TimerStatus.RUNNING && focusSession.enableDnd -> "Do Not Disturb is active and will restore when the timer ends."
                focusSession.status == TimerStatus.PAUSED && focusSession.enableDnd -> "Do Not Disturb stays active while this focus session is paused."
                effectiveFocusMode -> "Focus mode will enable Do Not Disturb for the next session."
                else -> "Focus sessions will run without changing Do Not Disturb."
            },
            dndPermissionCtaLabel = if (dndAccessGranted) null else "Grant DND Access"
        ),
        reminderText = morningBlock?.notes ?: "Solve board questions first."
    )
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
