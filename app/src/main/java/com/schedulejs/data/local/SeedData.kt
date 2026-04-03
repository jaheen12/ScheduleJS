package com.schedulejs.data.local

import com.schedulejs.domain.NotificationLeadTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SeedData(private val database: ScheduleDatabase) {
    private val seedMutex = Mutex()

    suspend fun seedIfNeeded() {
        if (seeded) return
        seedMutex.withLock {
            if (seeded) return
            if (database.dayTemplateDao().count() > 0) {
                seeded = true
                return
            }

            val classTemplateId = database.dayTemplateDao().insert(
                DayTemplateEntity(
                    title = "Class Day",
                    dayType = "CLASS_DAY",
                    wakeUpTime = "06:30"
                )
            )
            val officeTemplateId = database.dayTemplateDao().insert(
                DayTemplateEntity(
                    title = "Office Day",
                    dayType = "OFFICE_DAY",
                    wakeUpTime = "06:30"
                )
            )
            val fridayTemplateId = database.dayTemplateDao().insert(
                DayTemplateEntity(
                    title = "Friday",
                    dayType = "FRIDAY",
                    wakeUpTime = "07:30"
                )
            )

            database.templateTaskDao().insertAll(classDayTasks(classTemplateId))
            database.templateTaskDao().insertAll(officeDayTasks(officeTemplateId))
            database.templateTaskDao().insertAll(fridayTasks(fridayTemplateId))
            database.studyRotationDao().insertAll(studyRotations())
            database.workoutRotationDao().insertAll(workoutRotations())
            database.weeklyReviewLogDao().insertAll(reviewLogs())
            database.appSettingsDao().upsert(
                AppSettingsEntity(
                    notificationLeadTime = NotificationLeadTime.FIVE_MINUTES.name,
                    transitAlertsEnabled = true,
                    notificationsEducationDismissed = false,
                    exactAlarmEducationDismissed = false,
                    dndEducationDismissed = false,
                    batteryOptimizationEducationDismissed = false
                )
            )
            seeded = true
        }
    }

    companion object {
        @Volatile
        private var seeded = false
    }

    private fun classDayTasks(templateId: Long): List<TemplateTaskEntity> {
        return listOf(
            TemplateTaskEntity(templateId = templateId, title = "Wake Up & Hydrate", startTime = "06:30", endTime = "06:40", category = "ROUTINE", details = "Drink one large glass of water and finish the 5-minute belly routine.", sortOrder = 0),
            TemplateTaskEntity(templateId = templateId, title = "Morning Workout", startTime = "06:40", endTime = "07:20", category = "WORKOUT", details = "Workout screen loads today's focus from the workout rotation table.", sortOrder = 1),
            TemplateTaskEntity(templateId = templateId, title = "Cooldown", startTime = "07:20", endTime = "07:30", category = "ROUTINE", details = "Quick wipe down and prep for deep work.", sortOrder = 2),
            TemplateTaskEntity(templateId = templateId, title = "Morning Deep Work", startTime = "07:30", endTime = "08:30", category = "STUDY", details = "Study screen loads today's major subject from the morning rotation.", sortOrder = 3),
            TemplateTaskEntity(templateId = templateId, title = "Breakfast & Dress", startTime = "08:30", endTime = "09:15", category = "ROUTINE", details = "Eat, dress for college, and pack the bag.", sortOrder = 4),
            TemplateTaskEntity(templateId = templateId, title = "Bicycle to College", startTime = "09:15", endTime = "09:45", category = "TRANSIT", details = "20-minute ride with a 10-minute class buffer.", sortOrder = 5),
            TemplateTaskEntity(templateId = templateId, title = "College Classes", startTime = "09:45", endTime = "12:45", category = "COLLEGE", details = "Govt Azizul Haque College class block.", sortOrder = 6),
            TemplateTaskEntity(templateId = templateId, title = "Bicycle to Home", startTime = "12:45", endTime = "13:05", category = "TRANSIT", details = "Ride home after classes.", sortOrder = 7),
            TemplateTaskEntity(templateId = templateId, title = "The 35-Min Sprint", startTime = "13:05", endTime = "13:40", category = "PREP", details = "Cold face wash, 5-minute shower, eat pre-plated lunch, and dress for office.", sortOrder = 8),
            TemplateTaskEntity(templateId = templateId, title = "Bicycle to Office", startTime = "13:40", endTime = "14:00", category = "TRANSIT", details = "15-minute ride plus 5-minute buffer.", sortOrder = 9),
            TemplateTaskEntity(templateId = templateId, title = "Visiwave Internship", startTime = "14:00", endTime = "17:40", category = "OFFICE", details = "Market analysis and QA work.", sortOrder = 10),
            TemplateTaskEntity(templateId = templateId, title = "Bicycle to Home", startTime = "17:40", endTime = "17:55", category = "TRANSIT", details = "Ride back from office.", sortOrder = 11),
            TemplateTaskEntity(templateId = templateId, title = "Pit Stop", startTime = "17:55", endTime = "18:07", category = "ROUTINE", details = "Wash face and grab tuition materials.", sortOrder = 12),
            TemplateTaskEntity(templateId = templateId, title = "Tuition Class", startTime = "18:07", endTime = "19:23", category = "TUITION", details = "Ride to tuition, teach (18:15-19:15), then ride home.", sortOrder = 13),
            TemplateTaskEntity(templateId = templateId, title = "Freshen Up", startTime = "19:23", endTime = "19:30", category = "ROUTINE", details = "Change clothes and drink water.", sortOrder = 14),
            TemplateTaskEntity(templateId = templateId, title = "Tuition Prep", startTime = "19:30", endTime = "20:10", category = "PREP", details = "Plan tomorrow's Class 10 math lesson.", sortOrder = 15),
            TemplateTaskEntity(templateId = templateId, title = "Dinner", startTime = "20:10", endTime = "20:45", category = "MEAL", details = "Eat and decompress.", sortOrder = 16),
            TemplateTaskEntity(templateId = templateId, title = "Evening Study", startTime = "20:45", endTime = "21:25", category = "STUDY", details = "Study screen loads today's lighter subject from the evening rotation.", sortOrder = 17),
            TemplateTaskEntity(templateId = templateId, title = "Free Time & Prep", startTime = "21:25", endTime = "23:00", category = "PREP", details = "Relax, then complete the nightly check: clothes ready and lunch prepped.", sortOrder = 18)
        )
    }

    private fun officeDayTasks(templateId: Long): List<TemplateTaskEntity> {
        return listOf(
            TemplateTaskEntity(templateId = templateId, title = "Wake Up & Hydrate", startTime = "06:30", endTime = "06:40", category = "ROUTINE", details = "Drink one large glass of water and finish the 5-minute belly routine.", sortOrder = 0),
            TemplateTaskEntity(templateId = templateId, title = "Morning Workout", startTime = "06:40", endTime = "07:20", category = "WORKOUT", details = "Workout screen loads today's focus from the workout rotation table.", sortOrder = 1),
            TemplateTaskEntity(templateId = templateId, title = "Shower", startTime = "07:20", endTime = "07:30", category = "ROUTINE", details = "10-minute shower.", sortOrder = 2),
            TemplateTaskEntity(templateId = templateId, title = "Morning Deep Work", startTime = "07:30", endTime = "09:00", category = "STUDY", details = "Study screen loads today's major subject from the morning rotation.", sortOrder = 3),
            TemplateTaskEntity(templateId = templateId, title = "Breakfast & Dress", startTime = "09:00", endTime = "09:40", category = "ROUTINE", details = "Eat, relax briefly, and dress for office.", sortOrder = 4),
            TemplateTaskEntity(templateId = templateId, title = "Bicycle to Office", startTime = "09:40", endTime = "10:00", category = "TRANSIT", details = "15-minute ride plus 5-minute buffer.", sortOrder = 5),
            TemplateTaskEntity(templateId = templateId, title = "Visiwave Internship", startTime = "10:00", endTime = "17:45", category = "OFFICE", details = "Python learning, QA, and market analysis.", sortOrder = 6),
            TemplateTaskEntity(templateId = templateId, title = "Bicycle to Home", startTime = "17:45", endTime = "18:00", category = "TRANSIT", details = "Ride back from office.", sortOrder = 7),
            TemplateTaskEntity(templateId = templateId, title = "Pit Stop", startTime = "18:00", endTime = "18:07", category = "ROUTINE", details = "Drop bag and grab tuition materials.", sortOrder = 8),
            TemplateTaskEntity(templateId = templateId, title = "Tuition Class", startTime = "18:07", endTime = "19:23", category = "TUITION", details = "Ride to tuition, teach (18:15-19:15), then ride home.", sortOrder = 9),
            TemplateTaskEntity(templateId = templateId, title = "Freshen Up", startTime = "19:23", endTime = "19:30", category = "ROUTINE", details = "Change clothes and drink water.", sortOrder = 10),
            TemplateTaskEntity(templateId = templateId, title = "Tuition Prep", startTime = "19:30", endTime = "20:10", category = "PREP", details = "Plan tomorrow's Class 10 math lesson.", sortOrder = 11),
            TemplateTaskEntity(templateId = templateId, title = "Dinner", startTime = "20:10", endTime = "20:45", category = "MEAL", details = "Eat and decompress.", sortOrder = 12),
            TemplateTaskEntity(templateId = templateId, title = "Evening Study", startTime = "20:45", endTime = "21:25", category = "STUDY", details = "Study screen loads today's lighter subject from the evening rotation.", sortOrder = 13),
            TemplateTaskEntity(templateId = templateId, title = "Free Time & Prep", startTime = "21:25", endTime = "23:00", category = "PREP", details = "Relax, then complete the nightly check: clothes ready and lunch prepped.", sortOrder = 14)
        )
    }

    private fun fridayTasks(templateId: Long): List<TemplateTaskEntity> {
        return listOf(
            TemplateTaskEntity(templateId = templateId, title = "Relaxed Wake Up", startTime = "07:30", endTime = "08:00", category = "ROUTINE", details = "Water, 5-minute belly routine, and a light stretch.", sortOrder = 0),
            TemplateTaskEntity(templateId = templateId, title = "Breakfast", startTime = "08:00", endTime = "08:30", category = "MEAL", details = "Relaxed breakfast pace.", sortOrder = 1),
            TemplateTaskEntity(templateId = templateId, title = "Personal Time", startTime = "08:30", endTime = "11:30", category = "PERSONAL", details = "Laundry, room cleaning, and phone time.", sortOrder = 2),
            TemplateTaskEntity(templateId = templateId, title = "Jumma Namaz", startTime = "11:30", endTime = "13:30", category = "ROUTINE", details = "Prep, shower, and walk to mosque.", sortOrder = 3),
            TemplateTaskEntity(templateId = templateId, title = "Recovery", startTime = "13:30", endTime = "15:30", category = "REST", details = "Nap or complete rest.", sortOrder = 4),
            TemplateTaskEntity(templateId = templateId, title = "Weekly System Review", startTime = "15:30", endTime = "16:30", category = "REVIEW", details = "Use the Friday review page to log energy and study gaps.", sortOrder = 5),
            TemplateTaskEntity(templateId = templateId, title = "Free Time", startTime = "16:30", endTime = "19:30", category = "PERSONAL", details = "Zero obligations.", sortOrder = 6),
            TemplateTaskEntity(templateId = templateId, title = "Tuition Prep", startTime = "19:30", endTime = "20:10", category = "PREP", details = "Plan Saturday's Class 10 math lesson.", sortOrder = 7),
            TemplateTaskEntity(templateId = templateId, title = "Dinner & Free Time", startTime = "20:10", endTime = "23:00", category = "ROUTINE", details = "Dinner and full decompression.", sortOrder = 8)
        )
    }

    private fun studyRotations(): List<StudyRotationEntity> {
        return listOf(
            StudyRotationEntity(dayOfWeek = 7, blockType = "MORNING", subject = "Calculus", notes = "60m major block."),
            StudyRotationEntity(dayOfWeek = 7, blockType = "EVENING", subject = "History of Bangladesh", notes = "40m light block."),
            StudyRotationEntity(dayOfWeek = 1, blockType = "MORNING", subject = "Linear Algebra", notes = "90m major block."),
            StudyRotationEntity(dayOfWeek = 1, blockType = "EVENING", subject = "Statistics", notes = "40m light block."),
            StudyRotationEntity(dayOfWeek = 2, blockType = "MORNING", subject = "Fundamental Math", notes = "60m major block."),
            StudyRotationEntity(dayOfWeek = 2, blockType = "EVENING", subject = "Physics 1", notes = "40m light block."),
            StudyRotationEntity(dayOfWeek = 3, blockType = "MORNING", subject = "Calculus", notes = "90m major block."),
            StudyRotationEntity(dayOfWeek = 3, blockType = "EVENING", subject = "History of Bangladesh", notes = "40m light block."),
            StudyRotationEntity(dayOfWeek = 4, blockType = "MORNING", subject = "Linear Algebra", notes = "60m major block."),
            StudyRotationEntity(dayOfWeek = 4, blockType = "EVENING", subject = "Physics 2", notes = "40m light block."),
            StudyRotationEntity(dayOfWeek = 5, blockType = "MORNING", subject = "FREE", notes = "No scheduled morning study on Friday."),
            StudyRotationEntity(dayOfWeek = 5, blockType = "EVENING", subject = "FREE", notes = "No scheduled evening study on Friday."),
            StudyRotationEntity(dayOfWeek = 6, blockType = "MORNING", subject = "Fundamental Math", notes = "90m major block."),
            StudyRotationEntity(dayOfWeek = 6, blockType = "EVENING", subject = "FREE (Psychological Reset)", notes = "No evening study block.")
        )
    }

    private fun workoutRotations(): List<WorkoutRotationEntity> {
        val bellySteps = listOf(
            "Plank - 1 min",
            "Stomach vacuum - 10 reps",
            "Leg raises - 15 reps",
            "Cobra stretch - 30 sec"
        )
        fun rows(
            dayOfWeek: Int,
            dayLabel: String,
            isRestDay: Boolean,
            items: List<Triple<String, String, String>>
        ): List<WorkoutRotationEntity> {
            return items.mapIndexed { index, item ->
                WorkoutRotationEntity(
                    dayOfWeek = dayOfWeek,
                    dayLabel = dayLabel,
                    isRestDay = isRestDay,
                    routineTitle = item.first,
                    prescription = item.second,
                    note = item.third,
                    bellyRoutineStep = bellySteps.getOrElse(index) { "" },
                    sortOrder = index
                )
            }
        }

        return buildList {
            addAll(rows(7, "Today: Back + Core", false, listOf(
                Triple("Pull-ups / Inverted rows", "5 x max or 4 x 10", "Choose pull-ups if a bar is available, otherwise inverted rows."),
                Triple("Superman hold", "4 x 30 sec", "Controlled back extension."),
                Triple("Reverse snow angels", "3 x 12", "Keep shoulders packed."),
                Triple("Leg raises", "3 x 12", "Core finisher.")
            )))
            addAll(rows(1, "Today: Chest + Core", false, listOf(
                Triple("Push-ups", "4 x 12", "Stable tempo."),
                Triple("Decline push-ups", "3 x 10", "Feet elevated."),
                Triple("Diamond push-ups", "3 x 8", "Strict form."),
                Triple("Plank", "3 x 45 sec", "Brace hard."),
                Triple("Leg raises", "3 x 12", "Controlled reps.")
            )))
            addAll(rows(2, "Today: Legs", false, listOf(
                Triple("Squats", "4 x 20", "Main lower-body volume."),
                Triple("Lunges", "3 x 12 each leg", "Stay balanced."),
                Triple("Bulgarian split squats", "3 x 10 each leg", "Controlled depth."),
                Triple("Wall sit", "3 x 40 sec", "Hold steady.")
            )))
            addAll(rows(3, "Today: Shoulders + Core", false, listOf(
                Triple("Pike push-ups", "4 x 10", "Drive through shoulders."),
                Triple("Shoulder taps", "4 x 20", "Keep hips stable."),
                Triple("Plank", "3 x 60 sec", "Core stability."),
                Triple("Bicycle crunch", "3 x 20", "Controlled pace.")
            )))
            addAll(rows(4, "Today: Posture", false, listOf(
                Triple("Dead hang", "3 x 30 sec", "Open shoulders."),
                Triple("Cobra stretch", "3 x 30 sec", "Chest opening."),
                Triple("Cat-cow stretch", "1 min", "Spine mobility.")
            )))
            addAll(rows(5, "Today: Rest", true, listOf(
                Triple("Rest Day", "No workout", "Total rest. No app workout required.")
            )))
            addAll(rows(6, "Today: Full Body", false, listOf(
                Triple("Push-ups", "3 x 15", "Steady pace."),
                Triple("Squats", "3 x 20", "Full-body base."),
                Triple("Pike push-ups", "3 x 10", "Shoulder drive."),
                Triple("Plank", "3 x 45 sec", "Core tension."),
                Triple("Jump squats", "3 x 12", "Power finisher.")
            )))
        }
    }

    private fun reviewLogs(): List<WeeklyReviewLogEntity> {
        return listOf(
            WeeklyReviewLogEntity(
                reviewDate = "2026-03-20",
                q1Covered = "Good workout consistency and strong morning blocks.",
                q2Behind = "Evening study slipped into passive review.",
                q3Tuition = "Prep held on most days.",
                q4Energy = "Energy dipped after office-to-tuition transitions.",
                q5Adjustment = "Protect dinner timing and prep sooner."
            ),
            WeeklyReviewLogEntity(
                reviewDate = "2026-03-13",
                q1Covered = "Transit timing stayed on track.",
                q2Behind = "Two tuition prep blocks were rushed.",
                q3Tuition = "Teaching went fine but prep felt compressed.",
                q4Energy = "Sleep debt showed up by Thursday.",
                q5Adjustment = "Aim for earlier shutdown on Wednesday."
            ),
            WeeklyReviewLogEntity(
                reviewDate = "2026-03-06",
                q1Covered = "Friday review completed on time.",
                q2Behind = "Saturday evening plan was too ambitious.",
                q3Tuition = "Good overall balance.",
                q4Energy = "Recovery improved with the rest day.",
                q5Adjustment = "Keep Saturday night lighter."
            )
        )
    }
}
