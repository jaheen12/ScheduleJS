package com.schedulejs.ui

object DemoData {
    val dashboard = DashboardUiState(
        dateLabel = "Thursday, April 3",
        dayType = "Class Day",
        currentTask = TaskSnapshot(
            title = "Office",
            timeLabel = "14:00 - 17:40",
            subtitle = "Remaining: 1h 15m"
        ),
        nextTask = TaskSnapshot(
            title = "Transit to Home",
            timeLabel = "17:40",
            subtitle = "Bicycle departure with loud alert"
        ),
        progressPercent = 0.68f,
        timelineItems = listOf(
            TimelineItem("06:30", "Wake + Belly Routine", "Water, wake-up, 5-minute core reset.", TimelineItemState.PAST, "Routine"),
            TimelineItem("06:40 - 07:20", "Main Workout", "Leg day block for class-day energy.", TimelineItemState.PAST, "Workout"),
            TimelineItem("07:30 - 08:30", "Morning Study", "NU board questions only, no passive reading.", TimelineItemState.PAST, "Study"),
            TimelineItem("09:15", "Transit to College", "Leave by bicycle for the 09:45 class buffer.", TimelineItemState.PAST, "Transit"),
            TimelineItem("12:45 - 13:40", "College Return + Sprint", "Ride home, lunch, switch gear, leave again.", TimelineItemState.PAST, "College"),
            TimelineItem("14:00 - 17:40", "Office", "Focused work block at Visiwave.", TimelineItemState.CURRENT, "Office"),
            TimelineItem("18:07", "Transit to Tuition", "Fast reset at home, then depart.", TimelineItemState.UPCOMING, "Transit"),
            TimelineItem("19:30 - 20:10", "Tuition Prep", "Non-negotiable prep window.", TimelineItemState.UPCOMING, "Tuition"),
            TimelineItem("20:45 - 21:25", "Evening Study", "Lighter subjects and recap work.", TimelineItemState.UPCOMING, "Study"),
            TimelineItem("21:25 - 23:00", "Prepare Tomorrow", "Clothes, lunch, bag, then free time.", TimelineItemState.UPCOMING, "Prep")
        )
    )

    val workout = WorkoutUiState(
        dayLabel = "Today: Legs",
        muscleGroup = "Legs",
        purposeNote = "Strong legs improve hormone response and overall muscle gain.",
        dayOfWeek = "Sunday",
        weeklyStreak = 3,
        weekDays = listOf(
            WeeklyWorkoutDay("Sat", "\uD83D\uDCAA", isRestDay = false, isCompleted = true, isCurrent = false),
            WeeklyWorkoutDay("Sun", "\uD83E\uDDB5", isRestDay = false, isCompleted = false, isCurrent = true),
            WeeklyWorkoutDay("Mon", "\uD83E\uDDD8", isRestDay = false, isCompleted = false, isCurrent = false),
            WeeklyWorkoutDay("Tue", "\u2014", isRestDay = true, isCompleted = false, isCurrent = false),
            WeeklyWorkoutDay("Wed", "\uD83D\uDD19", isRestDay = false, isCompleted = false, isCurrent = false),
            WeeklyWorkoutDay("Thu", "\uD83D\uDD25", isRestDay = false, isCompleted = false, isCurrent = false),
            WeeklyWorkoutDay("Fri", "\u2014", isRestDay = true, isCompleted = false, isCurrent = false)
        ),
        routineItems = listOf(
            RoutineItem(
                id = "squats",
                title = "Squats",
                prescription = "4 x 20",
                totalSets = 4,
                repsOrDuration = "20 reps",
                setsCompleted = 1,
                note = "Primary lower-body volume."
            ),
            RoutineItem(
                id = "lunges",
                title = "Lunges",
                prescription = "3 x 12 each leg",
                totalSets = 3,
                repsOrDuration = "12 each leg",
                setsCompleted = 0,
                note = "Keep rest short."
            ),
            RoutineItem(
                id = "bulgarian-split-squats",
                title = "Bulgarian Split Squats",
                prescription = "3 x 10 each leg",
                totalSets = 3,
                repsOrDuration = "10 each leg",
                setsCompleted = 0
            ),
            RoutineItem(
                id = "wall-sit",
                title = "Wall Sit",
                prescription = "3 x 40 sec",
                totalSets = 3,
                repsOrDuration = "40 sec",
                setsCompleted = 0,
                note = "Finish with control."
            )
        ),
        bellyRoutineState = BellyRoutineState(
            ctaLabel = "Start 5-Min Belly Routine",
            steps = listOf(
                BellyRoutineStep("Plank", StepType.TIMED, durationSeconds = 60),
                BellyRoutineStep("Stomach vacuum", StepType.REPS, targetReps = 10),
                BellyRoutineStep("Leg raises", StepType.REPS, targetReps = 15),
                BellyRoutineStep("Cobra stretch", StepType.TIMED, durationSeconds = 30)
            ),
            currentStepIndex = 0,
            secondsRemaining = 60,
            repsCompleted = 0
        ),
        isWorkoutComplete = false
    )

    val study = StudyUiState(
        morningSubject = "Linear Algebra",
        eveningSubject = "Physics 1",
        focusTimerState = FocusTimerState(
            ctaLabel = "Enter Deep Work",
            durationLabel = "60 min",
            statusLabel = "Ready to start a 60-minute focus block.",
            isDndEnabled = true,
            isDndPermissionGranted = true,
            dndStatusLabel = "Focus mode will enable Do Not Disturb for the session."
        ),
        reminderText = "Solve board questions first. Reading is not the session."
    )

    val review = ReviewUiState(
        isUnlocked = false,
        questions = listOf(
            ReviewQuestion("What did I fully cover this week?", "Topics, chapters, repetitions"),
            ReviewQuestion("Where did I fall behind?", "Missed blocks, skipped transitions"),
            ReviewQuestion("How did tuition affect the day?", "Timing, energy, prep quality"),
            ReviewQuestion("What drained my energy?", "Sleep, commute, friction points"),
            ReviewQuestion("What should change next week?", "One concrete adjustment")
        ),
        historySummaries = listOf(
            ReviewHistoryItem("Mar 21", "Good workout consistency, evening study too passive."),
            ReviewHistoryItem("Mar 14", "Transit timing held, tuition prep slipped twice."),
            ReviewHistoryItem("Mar 07", "Friday review completed, Saturday rest worked well.")
        ),
        answerDraft = ReviewAnswerDraft("", "", "", "", ""),
        validationMessages = emptyList(),
        saveStatus = null
    )

    val settings = SettingsUiState(
        notificationLeadTime = "5 minutes before",
        transitAlertsEnabled = true,
        templateSummaries = listOf(
            TemplateSummary("Class Day", "College -> 35-minute sprint -> office -> tuition"),
            TemplateSummary("Office Day", "Morning study -> office -> tuition -> evening reset"),
            TemplateSummary("Friday", "Office flow with review unlock at 15:30")
        ),
        editableTemplates = emptyList(),
        permissionEducationCards = emptyList(),
        validationMessages = emptyList(),
        saveStatus = null
    )
}
