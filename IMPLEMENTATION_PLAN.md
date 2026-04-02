# ScheduleJS Implementation Plan

  ## Summary

  Build ScheduleJS as an offline-first Android app in
  Kotlin with Jetpack Compose, MVVM, Room, AlarmManager,
  a foreground service, and an App Widget. Deliver it in
  sequential phases so each phase ends in a usable,
  testable state and unblocks the next one without
  redesign. Use the existing BLUEPRINT.md, schedule.md,
  and weekly workout plan.md as the single product source
  for v1 behavior.

  ## Key Changes

  ### Phase 0: Foundation and project setup

  - Create a single Android app module with Compose,
    Material 3, Room, Navigation Compose, WorkManager
    only if needed later, and min SDK high enough for
    modern widget/notification APIs.
  - Establish package structure by layer: data, domain,
    ui, services, receivers, widget, settings.
  - Define core domain types up front: DayType,
    TaskCategory, ScheduleTask, WorkoutPlan, StudyBlock,
    ReviewEntry, NotificationLeadTime.
  - Set architectural rule: UI reads immutable screen
    state from ViewModels; repositories own schedule/
    template lookup; Android OS integrations stay behind
    service/helper interfaces.

  ### Phase 1: UI shell with static data

  - Implement the 5 primary screens: Dashboard, Workout,
    Study, Friday Review, Settings.
  - Use hardcoded preview/demo data matching current
    schedule and workout notes, including current/next
    task HUD, workout list, study blocks, review lock
    state, and settings toggles/dropdowns.
  - Define stable UI state contracts now so later phases
    replace only data sources, not composables.
  - Public UI state/interfaces to lock:
      - DashboardUiState(currentTask, nextTask,
        progressPercent, timelineItems)
      - WorkoutUiState(dayLabel, routineItems,
        bellyRoutineState, isWorkoutComplete)
      - StudyUiState(morningSubject, eveningSubject,
        focusTimerState, reminderText)
      - ReviewUiState(isUnlocked, questions,
        historySummaries)
      - SettingsUiState(notificationLeadTime,
        transitAlertsEnabled, templateSummaries)
  - Acceptance: app launches into a coherent navigation
    flow, dark mode works, and all blueprint screens are
    visibly represented.

  ### Phase 2: Data model and schedule engine

  - Implement Room schema based on the blueprint,
    normalized for maintainability:
      - day_templates
      - template_tasks
      - study_rotations
      - workout_rotations
      - weekly_review_logs
      - app_settings
  - Seed the database on first launch from the existing
    markdown plans so the app is functional offline
    immediately.
  - Build a schedule resolver that maps the current day
    to:
      - class day vs non-class office day vs Friday
        behavior
      - today’s ordered task list
      - morning/evening study subjects
      - workout assignment or rest day
  - Lock repository interfaces:
      - ScheduleRepository.getTodaySchedule(LocalDate)
      - WorkoutRepository.getWorkoutForDate(LocalDate)
      - StudyRepository.getStudyBlocksForDate(LocalDate)
      - ReviewRepository.getReviewState(LocalDateTime)
      - SettingsRepository.observeSettings()
  - Acceptance: replacing static data with repository-
    backed state changes no screen contracts and the app
    shows correct schedule content for all 7 days.

  ### Phase 3: Timers, progress, and interactive state

  - Implement a time engine that computes current task,
    next task, elapsed percent, and remaining duration
    using device local time.
  - Add the 5-minute belly routine timer with step
    sequence and audible transition cue.
  - Add the study focus timer with start, pause/cancel,
    completion, and state restoration after process
    recreation.
  - Persist interactive completion flags that matter for
    same-day UX, such as workout completion and active
    timer state.
  - Define service/helper interfaces:
      - TimeEngine.getDashboardSnapshot(now)
      - FocusTimerController.start(duration, enableDnd)
      - RoutineTimerController.startBellyRoutine()
  - Acceptance: HUD updates correctly across task
    boundaries, timers survive backgrounding, and all
    timer states are reflected in UI and notification
    text.

  ### Phase 4: Automation and Android OS integrations

  - Implement exact alarms for scheduled reminders with
    configurable lead time and a separate high-priority
    path for transit alerts.
  - Add a foreground service for persistent notification
    text showing current task and time remaining.
  - Implement Friday review unlock logic at Friday 15:30
    local time and history browsing for previous entries.
  - Implement the nightly checklist flow at 21:25 as a
    full-screen interruption only if Android policy
    allows it; otherwise fall back to the strongest
    allowed combination of full-screen notification,
    activity launch, and alarm UX.
  - Add DND permission flow for study focus mode and
    restore previous interruption state when the timer
    ends.
  - Build a 4x2 App Widget that mirrors the dashboard HUD
    and refreshes at least every minute or on significant
    schedule changes within Android limits.
  - Acceptance: reminders trigger at the right times,
    persistent notification reflects live state, widget
    displays current/next task, and the nightly checklist
    path behaves predictably on a real device.

  ### Phase 5: Editing, resilience, and release hardening

  - Implement settings-backed CRUD for templates, task
    timing, notification lead time, and transit alert
    toggle.
  - Add validation rules: no overlapping tasks within a
    template, start time before end time, and required
    fields for review entries.
  - Add first-run permission education for notifications,
    exact alarms if required by OS version, DND access,
    and battery optimization guidance only where
    necessary.
  - Harden background behavior across reboot, timezone
    change, date rollover, and app update by rehydrating
    alarms/services from persisted state.
  - Acceptance: a user can change schedule templates
    without code changes, reboot the phone, and still get
    correct next-day behavior.

  ### Phase 6: Post-v1 roadmap

  - Add analytics only if kept local/on-device; do not
    introduce cloud sync in v1.
  - Expand widget options, richer review insights, and
    trend summaries from weekly_review_logs.
  - Consider export/import of templates and review logs
    as JSON for backup.
  - Consider wearable or quick-settings tile support only
    after core reminder reliability is proven.

  ## Tests and Scenarios

  - Unit tests for day classification, task ordering,
    current/next task selection, progress calculation,
    Friday unlock timing, and notification lead time
    offsets.
  - Repository tests for Room seed data, CRUD behavior,
    and schedule resolution for all days named in
    schedule.md.
  - ViewModel tests for each screen state, including
    empty/error/loading transitions where applicable.
  - Instrumented tests for navigation flow, review lock/
    unlock UI, settings persistence, and template editing
    validation.
  - Device tests for alarm delivery, reboot rescheduling,
    foreground notification updates, DND enable/restore
    flow, widget refresh behavior, and timer survival
    through background/foreground transitions.
  - Manual acceptance scenarios:
      - Class day afternoon transition from college to
        office
      - Non-class office day evening flow
      - Friday review unavailable before 15:30 and
        available after
      - Saturday night without evening study
      - Rest-day workout screen behavior