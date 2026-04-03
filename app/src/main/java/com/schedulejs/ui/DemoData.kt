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
            TimelineItem("06:30", "Wake Up & Hydrate", "Water and 5-minute belly routine.", TimelineItemState.PAST, "Routine"),
            TimelineItem("06:40 - 07:20", "Morning Workout", "Daily focus from the workout rotation table.", TimelineItemState.PAST, "Workout"),
            TimelineItem("07:30 - 08:30", "Morning Deep Work", "Major subject from the study rotation.", TimelineItemState.PAST, "Study"),
            TimelineItem("09:15", "Bicycle to College", "Leave by bicycle for the 09:45 class buffer.", TimelineItemState.PAST, "Transit"),
            TimelineItem("12:45 - 13:40", "College Return + Sprint", "Ride home, lunch, switch gear, leave again.", TimelineItemState.PAST, "College"),
            TimelineItem("14:00 - 17:40", "Office", "Focused work block at Visiwave.", TimelineItemState.CURRENT, "Office"),
            TimelineItem("18:07", "Transit to Tuition", "Fast reset at home, then depart.", TimelineItemState.UPCOMING, "Transit"),
            TimelineItem("19:30 - 20:10", "Tuition Prep", "Non-negotiable prep window.", TimelineItemState.UPCOMING, "Tuition"),
            TimelineItem("20:45 - 21:25", "Evening Study", "Lighter subjects and recap work.", TimelineItemState.UPCOMING, "Study"),
            TimelineItem("21:25 - 23:00", "Free Time & Prep", "Relax, clothes ready, and lunch prepped.", TimelineItemState.UPCOMING, "Prep")
        )
    )

    val workout = WorkoutUiState(
        dayLabel = "Today: Back + Core",
        muscleGroup = "Back + Core",
        purposeNote = "Back + core day to reinforce posture and pulling strength.",
        dayOfWeek = "Sunday",
        weeklyStreak = 3,
        weekDays = listOf(
            WeeklyWorkoutDay("Sun", "\uD83D\uDD19", isRestDay = false, isCompleted = false, isCurrent = true),
            WeeklyWorkoutDay("Mon", "\uD83D\uDCAA", isRestDay = false, isCompleted = false, isCurrent = false),
            WeeklyWorkoutDay("Tue", "\uD83E\uDDB5", isRestDay = false, isCompleted = false, isCurrent = false),
            WeeklyWorkoutDay("Wed", "\uD83E\uDDD8", isRestDay = false, isCompleted = false, isCurrent = false),
            WeeklyWorkoutDay("Thu", "\uD83E\uDDD6", isRestDay = false, isCompleted = false, isCurrent = false),
            WeeklyWorkoutDay("Fri", "\u2014", isRestDay = true, isCompleted = false, isCurrent = false),
            WeeklyWorkoutDay("Sat", "\uD83D\uDD25", isRestDay = false, isCompleted = true, isCurrent = false)
        ),
        routineItems = listOf(
            RoutineItem(
                id = "pullups",
                title = "Pull-ups / Inverted rows",
                prescription = "5 x max or 4 x 10",
                totalSets = 5,
                repsOrDuration = "max or 4 x 10",
                setsCompleted = 1,
                note = "Use pull-ups if a bar is available."
            ),
            RoutineItem(
                id = "superman-hold",
                title = "Superman hold",
                prescription = "4 x 30 sec",
                totalSets = 4,
                repsOrDuration = "30 sec",
                setsCompleted = 0,
                note = "Controlled back extension."
            ),
            RoutineItem(
                id = "reverse-snow-angels",
                title = "Reverse snow angels",
                prescription = "3 x 12",
                totalSets = 3,
                repsOrDuration = "12 reps",
                setsCompleted = 0
            ),
            RoutineItem(
                id = "leg-raises",
                title = "Leg raises",
                prescription = "3 x 12",
                totalSets = 3,
                repsOrDuration = "12 reps",
                setsCompleted = 0,
                note = "Core finisher."
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
            TemplateSummary("Friday", "Recovery day -> weekly review -> light prep")
        ),
        editableTemplates = emptyList(),
        permissionEducationCards = emptyList(),
        validationMessages = emptyList(),
        saveStatus = null
    )
}
