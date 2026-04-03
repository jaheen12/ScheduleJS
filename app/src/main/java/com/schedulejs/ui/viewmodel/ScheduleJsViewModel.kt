package com.schedulejs.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import com.schedulejs.data.repository.PermissionEducationCard
import com.schedulejs.data.repository.ReviewRepository
import com.schedulejs.data.repository.ScheduleRepository
import com.schedulejs.data.repository.SettingsRepository
import com.schedulejs.data.repository.StudyRepository
import com.schedulejs.data.repository.WorkoutRepository
import com.schedulejs.domain.BellyRoutineSession
import com.schedulejs.domain.DashboardSnapshot
import com.schedulejs.domain.DayTemplateDraft
import com.schedulejs.domain.DayType
import com.schedulejs.domain.FocusTimerSession
import com.schedulejs.domain.NotificationLeadTime
import com.schedulejs.domain.ReviewEntryDraft
import com.schedulejs.domain.StudyBlock
import com.schedulejs.domain.StudyBlockType
import com.schedulejs.domain.TaskCategory
import com.schedulejs.domain.TimerStatus
import com.schedulejs.domain.TodaySchedule
import com.schedulejs.domain.WorkoutPlan
import com.schedulejs.services.DefaultTimeEngine
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
import com.schedulejs.ui.BellyRoutineStep
import com.schedulejs.ui.DashboardUiState
import com.schedulejs.ui.EditableTaskUiState
import com.schedulejs.ui.EditableTemplateUiState
import com.schedulejs.ui.FocusTimerState
import com.schedulejs.ui.PermissionEducationCardUiState
import com.schedulejs.ui.ReviewAnswerDraft
import com.schedulejs.ui.ReviewHistoryItem
import com.schedulejs.ui.ReviewQuestion
import com.schedulejs.ui.ReviewUiState
import com.schedulejs.ui.RoutineItem
import com.schedulejs.ui.SettingsUiState
import com.schedulejs.ui.StepType
import com.schedulejs.ui.StudyUiState
import com.schedulejs.ui.TaskSnapshot
import com.schedulejs.ui.TemplateSummary
import com.schedulejs.ui.TimelineItem
import com.schedulejs.ui.TimelineItemState
import com.schedulejs.ui.WeeklyWorkoutDay
import com.schedulejs.ui.WorkoutUiState
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
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
    private var editableTemplates: List<DayTemplateDraft> = emptyList()
    private var focusModeEnabled = false
    private var notificationLeadTimeSelection = NotificationLeadTime.FIVE_MINUTES
    private var transitAlertsEnabledSelection = true
    private var permissionCards: List<PermissionEducationCardUiState> = emptyList()
    private var reviewDraft = ReviewEntryDraft()
    private var reviewSaveStatus: String? = null
    private var settingsSaveStatus: String? = null
    private var settingsValidationMessages: List<String> = emptyList()
    private var reviewValidationMessages: List<String> = emptyList()
    private var routineSetProgress: Map<String, Int> = emptyMap()
    private var routineRestTimers: Map<String, Int> = emptyMap()
    private var bellyStepRepProgress: Int = 0
    private var weeklyWorkoutDaysCache: List<WeeklyWorkoutDay> = emptyList()
    private var weeklyWorkoutStreakCache: Int = 0
    private var isWorkoutWeekDirty: Boolean = true

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

    fun buildDndPermissionIntent(): Intent = focusModeController.buildPermissionIntent()

    fun buildNotificationPermissionIntent(): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, "com.schedulejs")
    }

    fun buildExactAlarmPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:com.schedulejs")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:com.schedulejs")
            }
        }
    }

    fun buildBatteryOptimizationIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:com.schedulejs")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:com.schedulejs")
            }
        }
    }

    fun dismissPermissionEducation(cardId: String) {
        val card = when (cardId) {
            "notifications" -> PermissionEducationCard.NOTIFICATIONS
            "exact_alarms" -> PermissionEducationCard.EXACT_ALARMS
            "dnd" -> PermissionEducationCard.DND
            "battery_optimization" -> PermissionEducationCard.BATTERY_OPTIMIZATION
            else -> return
        }
        viewModelScope.launch {
            settingsRepository.dismissPermissionEducation(card)
            syncUi()
        }
    }

    fun updateNotificationLeadTime(label: String) {
        notificationLeadTimeSelection = NotificationLeadTime.entries.firstOrNull { it.displayLabel == label }
            ?: notificationLeadTimeSelection
        settingsSaveStatus = null
        syncSettingsStateOnly()
    }

    fun updateTransitAlerts(enabled: Boolean) {
        transitAlertsEnabledSelection = enabled
        settingsSaveStatus = null
        syncSettingsStateOnly()
    }

    fun updateTemplateWakeUpTime(templateId: Long, value: String) {
        editableTemplates = editableTemplates.map { template ->
            if (template.id == templateId) template.copy(wakeUpTime = value) else template
        }
        settingsSaveStatus = null
        syncSettingsStateOnly()
    }

    fun updateTaskField(templateId: Long, taskId: Long, field: TemplateField, value: String) {
        editableTemplates = editableTemplates.map { template ->
            if (template.id != templateId) return@map template
            template.copy(
                tasks = template.tasks.map { task ->
                    if (task.id != taskId) return@map task
                    when (field) {
                        TemplateField.TITLE -> task.copy(title = value)
                        TemplateField.START_TIME -> task.copy(startTime = value)
                        TemplateField.END_TIME -> task.copy(endTime = value)
                        TemplateField.DETAILS -> task.copy(details = value)
                    }
                }
            )
        }
        settingsSaveStatus = null
        syncSettingsStateOnly()
    }

    fun saveSettings() {
        viewModelScope.launch {
            val templateErrors = scheduleRepository.updateTemplates(editableTemplates)
            if (templateErrors.isNotEmpty()) {
                settingsValidationMessages = templateErrors.map { it.message }.distinct()
                settingsSaveStatus = null
                syncSettingsStateOnly()
                return@launch
            }
            settingsRepository.updateSettings(notificationLeadTimeSelection, transitAlertsEnabledSelection)
            loadedDate = null
            loadedTemplateSummaries = scheduleRepository.getTemplateSummaries().map { TemplateSummary(it.first, it.second) }
            settingsValidationMessages = emptyList()
            settingsSaveStatus = "Settings saved."
            syncUi()
        }
    }

    fun toggleWorkoutComplete() {
        viewModelScope.launch {
            val date = LocalDate.now(clock)
            val isComplete = interactiveStateRepository.isWorkoutComplete(date)
            interactiveStateRepository.setWorkoutComplete(date, !isComplete)
            isWorkoutWeekDirty = true
            syncUi()
        }
    }

    fun checkWorkoutSet(itemId: String, setNumber: Int) {
        viewModelScope.launch {
            val workout = loadedWorkout ?: return@launch
            val item = workout.routineItems.firstOrNull { it.idForUi() == itemId } ?: return@launch
            val totalSets = parsePrescription(item.prescription).first
            if (totalSets <= 0) return@launch

            val normalizedSet = setNumber.coerceIn(0, totalSets)
            val previous = routineSetProgress[itemId] ?: 0
            routineSetProgress = routineSetProgress + (itemId to normalizedSet)
            if (normalizedSet > previous && normalizedSet in 1 until totalSets) {
                routineRestTimers = routineRestTimers + (itemId to DEFAULT_REST_SECONDS)
            } else if (normalizedSet >= totalSets) {
                routineRestTimers = routineRestTimers - itemId
            }

            val allDone = workout.routineItems.all { routine ->
                val key = routine.idForUi()
                val sets = parsePrescription(routine.prescription).first
                (routineSetProgress[key] ?: 0) >= sets
            }
            if (allDone) {
                interactiveStateRepository.setWorkoutComplete(LocalDate.now(clock), true)
                isWorkoutWeekDirty = true
            }
            syncUi()
        }
    }

    fun onBellyRoutineRepTap() {
        val state = _workoutState.value.bellyRoutineState
        val step = state.steps.getOrNull(state.currentStepIndex) ?: return
        if (step.type != StepType.REPS || step.targetReps <= 0) return
        bellyStepRepProgress = (bellyStepRepProgress + 1).coerceAtMost(step.targetReps)
        _workoutState.value = _workoutState.value.copy(
            bellyRoutineState = state.copy(repsCompleted = bellyStepRepProgress)
        )
    }

    fun updateReviewAnswer(field: ReviewField, value: String) {
        reviewDraft = when (field) {
            ReviewField.COVERED -> reviewDraft.copy(covered = value)
            ReviewField.BEHIND -> reviewDraft.copy(behind = value)
            ReviewField.TUITION -> reviewDraft.copy(tuition = value)
            ReviewField.ENERGY -> reviewDraft.copy(energy = value)
            ReviewField.ADJUSTMENT -> reviewDraft.copy(adjustment = value)
        }
        reviewSaveStatus = null
        syncReviewStateOnly()
    }

    fun saveReview() {
        viewModelScope.launch {
            val today = LocalDate.now(clock)
            val errors = reviewRepository.saveReview(today, reviewDraft)
            if (errors.isNotEmpty()) {
                reviewValidationMessages = errors.map { it.message }.distinct()
                reviewSaveStatus = null
                syncReviewStateOnly()
                return@launch
            }
            reviewDraft = ReviewEntryDraft()
            reviewValidationMessages = emptyList()
            reviewSaveStatus = "Review saved."
            syncUi()
        }
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (isActive) {
                tickWorkoutRestTimers()
                syncUi(clock.millis())
                delay(1_000)
            }
        }
    }

    private fun tickWorkoutRestTimers() {
        if (routineRestTimers.isEmpty()) return
        routineRestTimers = routineRestTimers
            .mapValues { (_, value) -> (value - 1).coerceAtLeast(0) }
            .filterValues { it > 0 }
    }

    private suspend fun refreshWorkoutWeekIfNeeded(today: LocalDate) {
        if (!isWorkoutWeekDirty && weeklyWorkoutDaysCache.isNotEmpty()) return
        val start = startOfWorkoutWeek(today)
        val weekData = buildList {
            repeat(7) { offset ->
                val date = start.plusDays(offset.toLong())
                val plan = workoutRepository.getWorkoutForDate(date)
                val completed = interactiveStateRepository.isWorkoutComplete(date)
                add(
                    WeeklyWorkoutDay(
                        dayLabel = date.dayOfWeek.shortLabel(),
                        muscleGroupEmoji = muscleEmoji(plan.dayLabel),
                        isRestDay = plan.isRestDay,
                        isCompleted = completed,
                        isCurrent = date == today
                    )
                )
            }
        }
        weeklyWorkoutDaysCache = weekData
        weeklyWorkoutStreakCache = computeWeeklyStreak(weekData)
        isWorkoutWeekDirty = false
    }

    private suspend fun syncUi(nowEpochMillis: Long = clock.millis()) {
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowEpochMillis), clock.zone)
        ensureStaticDataLoaded(now.toLocalDate())

        val schedule = loadedSchedule ?: return
        val workout = loadedWorkout ?: return
        val focusSession = focusTimerController.getState()
        val bellySession = routineTimerController.getState()
        val bellySteps = workout.bellyRoutineSteps.map(::parseBellyRoutineStep)
        val activeStep = bellySteps.getOrNull(bellySession.currentStepIndex)
        if (activeStep?.type != StepType.REPS) {
            bellyStepRepProgress = 0
        } else if (bellyStepRepProgress > activeStep.targetReps) {
            bellyStepRepProgress = activeStep.targetReps
        }
        val review = reviewRepository.getReviewState(now)
        val workoutComplete = interactiveStateRepository.isWorkoutComplete(now.toLocalDate())
        if (workoutComplete && routineSetProgress.isEmpty()) {
            routineSetProgress = workout.routineItems.associate { item ->
                val sets = parsePrescription(item.prescription).first
                item.idForUi() to sets
            }
        }
        val dashboardSnapshot = timeEngine.getDashboardSnapshot(schedule, now)
        val liveContent = com.schedulejs.services.DashboardLiveContentFactory.create(schedule, dashboardSnapshot, now)
        val dndAccessGranted = focusModeController.hasNotificationPolicyAccess()
        refreshWorkoutWeekIfNeeded(now.toLocalDate())
        if (focusSession.status == TimerStatus.RUNNING || focusSession.status == TimerStatus.PAUSED) {
            focusModeEnabled = focusSession.enableDnd
        } else if (!dndAccessGranted) {
            focusModeEnabled = false
        }

        _dashboardState.value = schedule.toDashboardUiState(liveContent, now)
        _workoutState.value = workout.toWorkoutUiState(
            session = bellySession,
            isWorkoutComplete = workoutComplete,
            setProgress = routineSetProgress,
            restTimers = routineRestTimers,
            weekDays = weeklyWorkoutDaysCache,
            weeklyStreak = weeklyWorkoutStreakCache,
            repsCompleted = bellyStepRepProgress
        )
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
            },
            answerDraft = reviewDraft.toUi(),
            validationMessages = reviewValidationMessages,
            saveStatus = reviewSaveStatus
        )
        syncSettingsStateOnly()
    }

    private suspend fun ensureStaticDataLoaded(date: LocalDate) {
        if (loadedDate == date && loadedSchedule != null && loadedWorkout != null && editableTemplates.isNotEmpty()) {
            return
        }

        loadedDate = date
        loadedSchedule = scheduleRepository.getTodaySchedule(date)
        loadedWorkout = workoutRepository.getWorkoutForDate(date)
        routineSetProgress = emptyMap()
        routineRestTimers = emptyMap()
        bellyStepRepProgress = 0
        isWorkoutWeekDirty = true
        loadedStudyBlocks = studyRepository.getStudyBlocksForDate(date)
        loadedTemplateSummaries = scheduleRepository.getTemplateSummaries().map {
            TemplateSummary(it.first, it.second)
        }
        editableTemplates = scheduleRepository.getEditableTemplates()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.observeSettings().collectLatest { settings ->
                notificationLeadTimeSelection = settings.notificationLeadTime
                transitAlertsEnabledSelection = settings.transitAlertsEnabled
                permissionCards = buildPermissionCards(settings.permissionEducation)
                syncSettingsStateOnly()
            }
        }
    }

    private fun syncReviewStateOnly() {
        _reviewState.value = _reviewState.value.copy(
            answerDraft = reviewDraft.toUi(),
            validationMessages = reviewValidationMessages,
            saveStatus = reviewSaveStatus
        )
    }

    private fun syncSettingsStateOnly() {
        _settingsState.value = _settingsState.value.copy(
            notificationLeadTime = notificationLeadTimeSelection.displayLabel,
            transitAlertsEnabled = transitAlertsEnabledSelection,
            templateSummaries = loadedTemplateSummaries,
            editableTemplates = editableTemplates.map { template ->
                EditableTemplateUiState(
                    templateId = template.id,
                    title = template.title,
                    dayTypeLabel = template.dayType.name.replace('_', ' '),
                    wakeUpTime = template.wakeUpTime,
                    tasks = template.tasks.map { task ->
                        EditableTaskUiState(
                            taskId = task.id,
                            title = task.title,
                            startTime = task.startTime,
                            endTime = task.endTime,
                            details = task.details
                        )
                    }
                )
            },
            permissionEducationCards = permissionCards,
            validationMessages = settingsValidationMessages,
            saveStatus = settingsSaveStatus
        )
    }

    private fun buildPermissionCards(
        education: com.schedulejs.domain.PermissionEducationState
    ): List<PermissionEducationCardUiState> {
        val cards = mutableListOf<PermissionEducationCardUiState>()
        if (education.shouldShowNotificationsCard) {
            cards += PermissionEducationCardUiState(
                id = "notifications",
                title = "Notifications",
                description = "Enable app notifications so schedule reminders can surface on time.",
                actionLabel = "Open Notifications"
            )
        }
        if (education.shouldShowExactAlarmsCard) {
            cards += PermissionEducationCardUiState(
                id = "exact_alarms",
                title = "Exact Alarms",
                description = "Grant exact alarm access on Android 12+ for tighter reminder timing.",
                actionLabel = "Allow Exact Alarms"
            )
        }
        if (education.shouldShowDndCard) {
            cards += PermissionEducationCardUiState(
                id = "dnd",
                title = "Do Not Disturb",
                description = "Allow DND access if you want focus sessions to temporarily silence distractions.",
                actionLabel = "Open DND Access"
            )
        }
        if (education.shouldShowBatteryOptimizationCard) {
            cards += PermissionEducationCardUiState(
                id = "battery_optimization",
                title = "Battery Optimization",
                description = "Ignore battery optimization only if reminders or the live status service are being delayed.",
                actionLabel = "Review Battery Settings"
            )
        }
        return cards
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
                        settingsRepository = OfflineSettingsRepository(database, seedData, context),
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
                dateLabel = "Loading...",
                currentTask = TaskSnapshot("Loading schedule", "--", "Resolving today's template."),
                nextTask = TaskSnapshot("Loading", "--", "Preparing next block."),
                progressPercent = 0f,
                timelineItems = emptyList()
            )
        }

        private fun loadingWorkoutState(): WorkoutUiState {
            return WorkoutUiState(
                dayLabel = "Loading workout",
                muscleGroup = "Loading",
                purposeNote = "Loading workout note.",
                dayOfWeek = "Today",
                weeklyStreak = 0,
                weekDays = emptyList(),
                routineItems = emptyList(),
                bellyRoutineState = BellyRoutineState(
                    ctaLabel = "Start Belly Routine",
                    steps = emptyList(),
                    currentStepIndex = 0,
                    secondsRemaining = 0,
                    repsCompleted = 0,
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
                historySummaries = emptyList(),
                answerDraft = ReviewAnswerDraft("", "", "", "", ""),
                validationMessages = emptyList(),
                saveStatus = null
            )
        }

        private fun loadingSettingsState(): SettingsUiState {
            return SettingsUiState(
                notificationLeadTime = NotificationLeadTime.FIVE_MINUTES.displayLabel,
                transitAlertsEnabled = true,
                templateSummaries = emptyList(),
                editableTemplates = emptyList(),
                permissionEducationCards = emptyList(),
                validationMessages = emptyList(),
                saveStatus = null
            )
        }

        private fun reviewQuestions(): List<ReviewQuestion> {
            return listOf(
                ReviewQuestion("What did I cover well this week?", "Name the work that actually moved."),
                ReviewQuestion("Where did I fall behind?", "Call out the bottleneck honestly."),
                ReviewQuestion("How did tuition prep go?", "Was prep protected or rushed?"),
                ReviewQuestion("What happened to my energy?", "Note sleep, transitions, or recovery issues."),
                ReviewQuestion("What changes next week?", "Pick one concrete adjustment.")
            )
        }
    }
}

enum class TemplateField {
    TITLE,
    START_TIME,
    END_TIME,
    DETAILS
}

enum class ReviewField {
    COVERED,
    BEHIND,
    TUITION,
    ENERGY,
    ADJUSTMENT
}

private fun ReviewEntryDraft.toUi(): ReviewAnswerDraft {
    return ReviewAnswerDraft(
        covered = covered,
        behind = behind,
        tuition = tuition,
        energy = energy,
        adjustment = adjustment
    )
}

private fun TodaySchedule.toDashboardUiState(
    liveContent: com.schedulejs.services.DashboardLiveContent,
    now: LocalDateTime
): DashboardUiState {
    val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d")

    // Helper to capitalize enum names for icon lookup or display
    fun TaskCategory.toDisplay(): String = name.lowercase().replaceFirstChar { it.uppercase() }

    return DashboardUiState(
        dateLabel = now.format(dateFormatter),
        dayType = dayType.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
        currentTask = TaskSnapshot(
            title = liveContent.currentTitle,
            timeLabel = liveContent.currentTimeLabel,
            subtitle = liveContent.currentSubtitle,
            category = liveContent.currentTaskCategory?.toDisplay() ?: ""
        ),
        nextTask = TaskSnapshot(
            title = liveContent.nextTitle,
            timeLabel = liveContent.nextTimeLabel,
            subtitle = liveContent.nextSubtitle,
            category = liveContent.nextTaskCategory?.toDisplay() ?: ""
        ),
        progressPercent = liveContent.progressPercent,
        timelineItems = tasks.map { task ->
            val state = when {
                now.toLocalTime().toSecondOfDay() / 60 >= task.endMinuteOfDay -> TimelineItemState.PAST
                now.toLocalTime().toSecondOfDay() / 60 >= task.startMinuteOfDay -> TimelineItemState.CURRENT
                else -> TimelineItemState.UPCOMING
            }
            TimelineItem(
                timeLabel = "${task.startMinuteOfDay.toClockLabel()} - ${task.endMinuteOfDay.toClockLabel()}",
                title = task.title,
                detail = task.details,
                state = state,
                category = task.category.toDisplay()
            )
        }
    )
}

private fun WorkoutPlan.toWorkoutUiState(
    session: BellyRoutineSession,
    isWorkoutComplete: Boolean,
    setProgress: Map<String, Int>,
    restTimers: Map<String, Int>,
    weekDays: List<WeeklyWorkoutDay>,
    weeklyStreak: Int,
    repsCompleted: Int
): WorkoutUiState {
    val statusLabel = when (session.status) {
        TimerStatus.IDLE -> "Five-minute core reset ready."
        TimerStatus.RUNNING -> "Step ${session.currentStepIndex + 1} running, ${session.stepRemainingSeconds}s left."
        TimerStatus.PAUSED -> "Paused with ${session.remainingSeconds}s remaining."
        TimerStatus.COMPLETED -> "Belly routine complete."
    }
    val bellySteps = bellyRoutineSteps.map(::parseBellyRoutineStep)
    val currentStepIndex = session.currentStepIndex.coerceIn(0, (bellySteps.lastIndex).coerceAtLeast(0))
    return WorkoutUiState(
        dayLabel = dayLabel,
        muscleGroup = dayLabel.substringAfter("Today:", dayLabel).trim(),
        purposeNote = purposeForWorkoutDay(dayOfWeek),
        dayOfWeek = dayOfWeek.displayName(),
        weeklyStreak = weeklyStreak,
        weekDays = weekDays,
        routineItems = routineItems.map { item ->
            val parsed = parsePrescription(item.prescription)
            val id = item.idForUi()
            RoutineItem(
                id = id,
                title = item.title,
                prescription = item.prescription,
                totalSets = parsed.first,
                repsOrDuration = parsed.second,
                setsCompleted = (setProgress[id] ?: 0).coerceAtMost(parsed.first),
                restSecondsLeft = restTimers[id] ?: 0,
                note = item.note
            )
        },
        bellyRoutineState = BellyRoutineState(
            ctaLabel = when (session.status) {
                TimerStatus.RUNNING -> "Pause Belly Routine"
                TimerStatus.PAUSED -> "Resume Belly Routine"
                else -> "Start Belly Routine"
            },
            steps = bellySteps,
            currentStepIndex = currentStepIndex,
            secondsRemaining = session.stepRemainingSeconds,
            repsCompleted = repsCompleted,
            statusLabel = statusLabel,
            isTimerVisible = session.status == TimerStatus.RUNNING || session.status == TimerStatus.PAUSED,
            secondaryCtaLabel = if (session.status == TimerStatus.RUNNING || session.status == TimerStatus.PAUSED) {
                "Cancel"
            } else {
                null
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
    val morning = firstOrNull { it.blockType == StudyBlockType.MORNING }
    val evening = firstOrNull { it.blockType == StudyBlockType.EVENING }
    return StudyUiState(
        morningSubject = morning?.subject ?: "No morning study",
        eveningSubject = evening?.subject ?: "No evening study",
        focusTimerState = FocusTimerState(
            ctaLabel = when (focusSession.status) {
                TimerStatus.RUNNING -> "Pause Deep Work"
                TimerStatus.PAUSED -> "Resume Deep Work"
                else -> "Enter Deep Work"
            },
            durationLabel = formatDuration(focusSession.remainingSeconds),
            statusLabel = when (focusSession.status) {
                TimerStatus.IDLE -> "Ready for ${defaultFocusDurationMinutes(dayType)} minutes of focus."
                TimerStatus.RUNNING -> "Focus timer running."
                TimerStatus.PAUSED -> "Focus timer paused."
                TimerStatus.COMPLETED -> "Focus block complete."
            },
            secondaryCtaLabel = if (focusSession.status == TimerStatus.RUNNING || focusSession.status == TimerStatus.PAUSED) "Cancel" else null,
            isDndEnabled = focusModeEnabled,
            isDndPermissionGranted = dndAccessGranted,
            dndStatusLabel = if (dndAccessGranted) {
                if (focusModeEnabled) "DND will be applied during focus sessions." else "DND access is available."
            } else {
                "DND access is not granted."
            },
            dndPermissionCtaLabel = if (dndAccessGranted) null else "Grant DND Access"
        ),
        reminderText = listOfNotNull(morning?.notes, evening?.notes).joinToString(" ")
    )
}

private const val DEFAULT_REST_SECONDS = 45

private fun parseBellyRoutineStep(raw: String): BellyRoutineStep {
    val parts = raw.split("-").map { it.trim() }
    val name = parts.firstOrNull().orEmpty().ifBlank { "Step" }
    val descriptor = parts.getOrNull(1).orEmpty().lowercase()
    return when {
        descriptor.contains("rep") -> {
            val reps = Regex("""(\d+)""").find(descriptor)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            BellyRoutineStep(name = name, type = StepType.REPS, targetReps = reps)
        }
        else -> {
            val duration = parseDurationToSeconds(descriptor)
            BellyRoutineStep(name = name, type = StepType.TIMED, durationSeconds = duration)
        }
    }
}

private fun parsePrescription(prescription: String): Pair<Int, String> {
    val normalized = prescription.trim().lowercase()
    val setsFirst = Regex("""(\d+)\s*x\s*([a-z0-9\s-]+)""").find(normalized)
    if (setsFirst != null) {
        val sets = setsFirst.groupValues[1].toIntOrNull() ?: 1
        val payload = setsFirst.groupValues[2].trim().ifBlank { "max" }
        val repsOrDuration = if (payload.matches(Regex("""\d+"""))) "$payload reps" else payload
        return sets to repsOrDuration
    }

    val setsLast = Regex("""([a-z0-9\s-]+)\s*x\s*(\d+)""").find(normalized)
    if (setsLast != null) {
        val payload = setsLast.groupValues[1].trim().ifBlank { "max" }
        val sets = setsLast.groupValues[2].toIntOrNull() ?: 1
        val repsOrDuration = if (payload.matches(Regex("""\d+"""))) "$payload reps" else payload
        return sets to repsOrDuration
    }

    val fallback = if (normalized.matches(Regex("""\d+"""))) "$normalized reps" else normalized
    return 1 to fallback
}

private fun parseDurationToSeconds(label: String): Int {
    val value = Regex("""(\d+)""").find(label)?.groupValues?.get(1)?.toIntOrNull() ?: return 0
    return when {
        label.contains("min") -> value * 60
        else -> value
    }
}

private fun purposeForWorkoutDay(day: DayOfWeek): String {
    return when (day) {
        DayOfWeek.SATURDAY -> "Build chest while tightening the stomach."
        DayOfWeek.SUNDAY -> "Strong legs improve hormone response and overall muscle gain."
        DayOfWeek.MONDAY -> "This widens shoulders and tightens the waist."
        DayOfWeek.TUESDAY -> "Posture and mobility reset to stay fresh for later sessions."
        DayOfWeek.WEDNESDAY -> "Back training is key for the V-shape."
        DayOfWeek.THURSDAY -> "This day boosts metabolism and overall conditioning."
        DayOfWeek.FRIDAY -> "Light stretching only to recover for the next cycle."
    }
}

private fun DayOfWeek.displayName(): String {
    return name.lowercase().replaceFirstChar { it.uppercase() }
}

private fun DayOfWeek.shortLabel(): String {
    return name.take(3).lowercase().replaceFirstChar { it.uppercase() }
}

private fun startOfWorkoutWeek(today: LocalDate): LocalDate {
    var start = today
    while (start.dayOfWeek != DayOfWeek.SATURDAY) {
        start = start.minusDays(1)
    }
    return start
}

private fun muscleEmoji(dayLabel: String): String {
    return when {
        dayLabel.contains("Chest", ignoreCase = true) -> "\uD83D\uDCAA"
        dayLabel.contains("Leg", ignoreCase = true) -> "\uD83E\uDDB5"
        dayLabel.contains("Shoulder", ignoreCase = true) -> "\uD83E\uDDD8"
        dayLabel.contains("Back", ignoreCase = true) -> "\uD83D\uDD19"
        dayLabel.contains("Full Body", ignoreCase = true) -> "\uD83D\uDD25"
        else -> ""
    }
}

private fun computeWeeklyStreak(days: List<WeeklyWorkoutDay>): Int {
    var streak = 0
    days.forEach { day ->
        if (day.isCurrent) {
            if (day.isRestDay || day.isCompleted) streak += 1 else streak = 0
            return streak
        }
        if (day.isRestDay || day.isCompleted) {
            streak += 1
        } else {
            streak = 0
        }
    }
    return streak
}

private fun com.schedulejs.domain.WorkoutRoutineItem.idForUi(): String {
    return "${title.lowercase().replace(" ", "-")}-${prescription.lowercase().replace(" ", "")}"
}

private fun defaultFocusDurationMinutes(dayType: DayType): Int {
    return when (dayType) {
        DayType.CLASS_DAY -> 35
        DayType.OFFICE_DAY -> 45
        DayType.FRIDAY -> 30
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun LocalDateTime.minuteOfDay(): Int = hour * 60 + minute
