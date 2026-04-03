package com.schedulejs.ui

object DemoData {
    val dashboard = DashboardUiState(
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
            TimelineItem("06:30", "Wake + Belly Routine", "Water, wake-up, 5-minute core reset.", TimelineItemState.PAST),
            TimelineItem("06:40 - 07:20", "Main Workout", "Leg day block for class-day energy.", TimelineItemState.PAST),
            TimelineItem("07:30 - 08:30", "Morning Study", "NU board questions only, no passive reading.", TimelineItemState.PAST),
            TimelineItem("09:15", "Transit to College", "Leave by bicycle for the 09:45 class buffer.", TimelineItemState.PAST),
            TimelineItem("12:45 - 13:40", "College Return + Sprint", "Ride home, lunch, switch gear, leave again.", TimelineItemState.PAST),
            TimelineItem("14:00 - 17:40", "Office", "Focused work block at Visiwave.", TimelineItemState.CURRENT),
            TimelineItem("18:07", "Transit to Tuition", "Fast reset at home, then depart.", TimelineItemState.UPCOMING),
            TimelineItem("19:30 - 20:10", "Tuition Prep", "Non-negotiable prep window.", TimelineItemState.UPCOMING),
            TimelineItem("20:45 - 21:25", "Evening Study", "Lighter subjects and recap work.", TimelineItemState.UPCOMING),
            TimelineItem("21:25 - 23:00", "Prepare Tomorrow", "Clothes, lunch, bag, then free time.", TimelineItemState.UPCOMING)
        )
    )

    val workout = WorkoutUiState(
        dayLabel = "Today: Legs",
        routineItems = listOf(
            RoutineItem("Squats", "4 x 20", "Primary lower-body volume."),
            RoutineItem("Lunges", "3 x 12 each leg", "Keep rest short."),
            RoutineItem("Bulgarian Split Squats", "3 x 10 each leg"),
            RoutineItem("Wall Sit", "3 x 40 sec", "Finish with control.")
        ),
        bellyRoutineState = BellyRoutineState(
            ctaLabel = "Start 5-Min Belly Routine",
            steps = listOf(
                "Plank - 1 min",
                "Stomach vacuum - 10 reps",
                "Leg raises - 15 reps",
                "Cobra stretch - 30 sec"
            )
        ),
        isWorkoutComplete = false
    )

    val study = StudyUiState(
        morningSubject = "Linear Algebra",
        eveningSubject = "Physics 1",
        focusTimerState = FocusTimerState(
            ctaLabel = "Enter Deep Work",
            durationLabel = "60 min",
            statusLabel = "Ready to start with DND later in Phase 4"
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
        )
    )

    val settings = SettingsUiState(
        notificationLeadTime = "5 minutes before",
        transitAlertsEnabled = true,
        templateSummaries = listOf(
            TemplateSummary("Class Day", "College -> 35-minute sprint -> office -> tuition"),
            TemplateSummary("Office Day", "Morning study -> office -> tuition -> evening reset"),
            TemplateSummary("Friday", "Office flow with review unlock at 15:30")
        )
    )
}
