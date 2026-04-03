package com.schedulejs.data.local

import com.schedulejs.domain.NotificationLeadTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SeedData(private val database: ScheduleDatabase) {
    private val seedMutex = Mutex()

    suspend fun seedIfNeeded() {
        seedMutex.withLock {
            if (database.dayTemplateDao().count() > 0) {
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
                    wakeUpTime = "06:30"
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
                    transitAlertsEnabled = true
                )
            )
        }
    }

    private fun classDayTasks(templateId: Long): List<TemplateTaskEntity> {
        return listOf(
            TemplateTaskEntity(templateId = templateId, title = "Wake + Belly Routine", startTime = "06:30", endTime = "06:40", category = "ROUTINE", details = "Drink water and complete the 5-minute belly routine.", sortOrder = 0),
            TemplateTaskEntity(templateId = templateId, title = "Main Workout", startTime = "06:40", endTime = "07:20", category = "WORKOUT", details = "Primary bodyweight training block.", sortOrder = 1),
            TemplateTaskEntity(templateId = templateId, title = "Cooldown", startTime = "07:20", endTime = "07:30", category = "ROUTINE", details = "Quick wipe down or cooldown before study.", sortOrder = 2),
            TemplateTaskEntity(templateId = templateId, title = "Morning Study", startTime = "07:30", endTime = "08:30", category = "STUDY", details = "NU board questions only.", sortOrder = 3),
            TemplateTaskEntity(templateId = templateId, title = "Breakfast + Get Ready", startTime = "08:30", endTime = "09:15", category = "MEAL", details = "Breakfast and get dressed for college.", sortOrder = 4),
            TemplateTaskEntity(templateId = templateId, title = "Transit to College", startTime = "09:15", endTime = "09:35", category = "TRANSIT", details = "Bicycle ride with a 10-minute class buffer.", sortOrder = 5),
            TemplateTaskEntity(templateId = templateId, title = "College", startTime = "09:35", endTime = "12:45", category = "COLLEGE", details = "Govt Azizul Haque College class block.", sortOrder = 6),
            TemplateTaskEntity(templateId = templateId, title = "Transit Home", startTime = "12:45", endTime = "13:05", category = "TRANSIT", details = "Bicycle ride back home from college.", sortOrder = 7),
            TemplateTaskEntity(templateId = templateId, title = "35-Minute Sprint", startTime = "13:05", endTime = "13:40", category = "PREP", details = "Lunch, face wash, gear switch, and immediate turnaround.", sortOrder = 8),
            TemplateTaskEntity(templateId = templateId, title = "Transit to Office", startTime = "13:40", endTime = "13:55", category = "TRANSIT", details = "Bicycle departure for Visiwave.", sortOrder = 9),
            TemplateTaskEntity(templateId = templateId, title = "Office", startTime = "14:00", endTime = "17:40", category = "OFFICE", details = "Focused work block at Visiwave.", sortOrder = 10),
            TemplateTaskEntity(templateId = templateId, title = "Transit Home", startTime = "17:40", endTime = "17:55", category = "TRANSIT", details = "Bicycle ride home with a 12-minute reset window after arrival.", sortOrder = 11),
            TemplateTaskEntity(templateId = templateId, title = "Tuition Transit", startTime = "18:07", endTime = "18:15", category = "TRANSIT", details = "Leave home and arrive for tuition on time.", sortOrder = 12),
            TemplateTaskEntity(templateId = templateId, title = "Tuition", startTime = "18:15", endTime = "19:15", category = "TUITION", details = "Teaching block.", sortOrder = 13),
            TemplateTaskEntity(templateId = templateId, title = "Transit Home", startTime = "19:15", endTime = "19:23", category = "TRANSIT", details = "Return home from tuition.", sortOrder = 14),
            TemplateTaskEntity(templateId = templateId, title = "Tuition Prep", startTime = "19:30", endTime = "20:10", category = "PREP", details = "Non-negotiable prep window.", sortOrder = 15),
            TemplateTaskEntity(templateId = templateId, title = "Dinner + Decompress", startTime = "20:10", endTime = "20:45", category = "MEAL", details = "Dinner and short decompression block.", sortOrder = 16),
            TemplateTaskEntity(templateId = templateId, title = "Evening Study", startTime = "20:45", endTime = "21:25", category = "STUDY", details = "Easier subjects and recap work.", sortOrder = 17),
            TemplateTaskEntity(templateId = templateId, title = "Prepare Tomorrow", startTime = "21:25", endTime = "23:00", category = "PREP", details = "Clothes, lunch, bag, then free time.", sortOrder = 18),
            TemplateTaskEntity(templateId = templateId, title = "Sleep", startTime = "23:00", endTime = "23:59", category = "SLEEP", details = "Target sleep time.", sortOrder = 19)
        )
    }

    private fun officeDayTasks(templateId: Long): List<TemplateTaskEntity> {
        return listOf(
            TemplateTaskEntity(templateId = templateId, title = "Wake + Belly Routine", startTime = "06:30", endTime = "06:40", category = "ROUTINE", details = "Drink water and complete the 5-minute belly routine.", sortOrder = 0),
            TemplateTaskEntity(templateId = templateId, title = "Main Workout", startTime = "06:40", endTime = "07:20", category = "WORKOUT", details = "Primary bodyweight training block.", sortOrder = 1),
            TemplateTaskEntity(templateId = templateId, title = "Shower", startTime = "07:20", endTime = "07:30", category = "ROUTINE", details = "Quick shower and reset.", sortOrder = 2),
            TemplateTaskEntity(templateId = templateId, title = "Morning Study", startTime = "07:30", endTime = "09:00", category = "STUDY", details = "Main 90-minute study block.", sortOrder = 3),
            TemplateTaskEntity(templateId = templateId, title = "Breakfast + Get Ready", startTime = "09:00", endTime = "09:40", category = "MEAL", details = "Breakfast and departure prep.", sortOrder = 4),
            TemplateTaskEntity(templateId = templateId, title = "Transit to Office", startTime = "09:40", endTime = "09:55", category = "TRANSIT", details = "Bicycle ride to Visiwave.", sortOrder = 5),
            TemplateTaskEntity(templateId = templateId, title = "Office", startTime = "10:00", endTime = "17:45", category = "OFFICE", details = "Main office block.", sortOrder = 6),
            TemplateTaskEntity(templateId = templateId, title = "Transit Home", startTime = "17:45", endTime = "18:00", category = "TRANSIT", details = "Bicycle ride home.", sortOrder = 7),
            TemplateTaskEntity(templateId = templateId, title = "Reset Window", startTime = "18:00", endTime = "18:07", category = "ROUTINE", details = "Drop the bag and switch to tuition mode.", sortOrder = 8),
            TemplateTaskEntity(templateId = templateId, title = "Tuition Transit", startTime = "18:07", endTime = "18:15", category = "TRANSIT", details = "Depart for tuition.", sortOrder = 9),
            TemplateTaskEntity(templateId = templateId, title = "Tuition", startTime = "18:15", endTime = "19:15", category = "TUITION", details = "Teaching block.", sortOrder = 10),
            TemplateTaskEntity(templateId = templateId, title = "Transit Home", startTime = "19:15", endTime = "19:23", category = "TRANSIT", details = "Return home from tuition.", sortOrder = 11),
            TemplateTaskEntity(templateId = templateId, title = "Tuition Prep", startTime = "19:30", endTime = "20:10", category = "PREP", details = "Non-negotiable prep window.", sortOrder = 12),
            TemplateTaskEntity(templateId = templateId, title = "Dinner + Decompress", startTime = "20:10", endTime = "20:45", category = "MEAL", details = "Dinner and decompression.", sortOrder = 13),
            TemplateTaskEntity(templateId = templateId, title = "Evening Study", startTime = "20:45", endTime = "21:25", category = "STUDY", details = "Lighter evening study block.", sortOrder = 14),
            TemplateTaskEntity(templateId = templateId, title = "Prepare Tomorrow", startTime = "21:25", endTime = "23:00", category = "PREP", details = "Clothes, lunch, bag, then free time.", sortOrder = 15),
            TemplateTaskEntity(templateId = templateId, title = "Sleep", startTime = "23:00", endTime = "23:59", category = "SLEEP", details = "Target sleep time.", sortOrder = 16)
        )
    }

    private fun fridayTasks(templateId: Long): List<TemplateTaskEntity> {
        return officeDayTasks(templateId).mapIndexed { index, task ->
            when (index) {
                12 -> task.copy(title = "Friday Review Prep", details = "Wrap the week and prepare for the Friday review unlock.")
                14 -> task.copy(title = "Weekly Reflection", details = "Use the unlocked Friday review if it is after 15:30.")
                else -> task
            }
        }
    }

    private fun studyRotations(): List<StudyRotationEntity> {
        return listOf(
            StudyRotationEntity(dayOfWeek = 1, blockType = "MORNING", subject = "Calculus", notes = "Solve before reading."),
            StudyRotationEntity(dayOfWeek = 1, blockType = "EVENING", subject = "Physics 1", notes = "Light recap after tuition."),
            StudyRotationEntity(dayOfWeek = 2, blockType = "MORNING", subject = "Chemistry", notes = "Reaction sets and recall."),
            StudyRotationEntity(dayOfWeek = 2, blockType = "EVENING", subject = "English", notes = "Short reading and writing revision."),
            StudyRotationEntity(dayOfWeek = 3, blockType = "MORNING", subject = "ICT", notes = "Concept checks and examples."),
            StudyRotationEntity(dayOfWeek = 3, blockType = "EVENING", subject = "Calculus", notes = "Easier recap session."),
            StudyRotationEntity(dayOfWeek = 4, blockType = "MORNING", subject = "Linear Algebra", notes = "Board questions first."),
            StudyRotationEntity(dayOfWeek = 4, blockType = "EVENING", subject = "Physics 1", notes = "Recap and easier problems."),
            StudyRotationEntity(dayOfWeek = 5, blockType = "MORNING", subject = "Weekly Review Buffer", notes = "Use this block to finish loose study ends."),
            StudyRotationEntity(dayOfWeek = 5, blockType = "EVENING", subject = "Friday Review", notes = "Weekly reflection after unlock."),
            StudyRotationEntity(dayOfWeek = 6, blockType = "MORNING", subject = "English", notes = "Weekend review and lighter work."),
            StudyRotationEntity(dayOfWeek = 6, blockType = "EVENING", subject = "Rest Night", notes = "Saturday night has no study block."),
            StudyRotationEntity(dayOfWeek = 7, blockType = "MORNING", subject = "Linear Algebra", notes = "Board-focused reps."),
            StudyRotationEntity(dayOfWeek = 7, blockType = "EVENING", subject = "Physics 1", notes = "Keep the load lighter.")
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
            addAll(rows(6, "Today: Chest + Core", false, listOf(
                Triple("Push-ups", "4 x 12", "Build chest volume."),
                Triple("Decline push-ups", "3 x 10", "Feet elevated."),
                Triple("Diamond push-ups", "3 x 8", "Keep form strict."),
                Triple("Plank", "3 x 45 sec", "Tight core brace."),
                Triple("Leg raises", "3 x 12", "Controlled reps.")
            )))
            addAll(rows(7, "Today: Legs", false, listOf(
                Triple("Squats", "4 x 20", "Primary lower-body volume."),
                Triple("Lunges", "3 x 12 each leg", "Keep rest short."),
                Triple("Bulgarian split squats", "3 x 10 each leg", ""),
                Triple("Wall sit", "3 x 40 sec", "Finish with control.")
            )))
            addAll(rows(1, "Today: Shoulders + Core", false, listOf(
                Triple("Pike push-ups", "4 x 10", ""),
                Triple("Shoulder taps", "4 x 20", ""),
                Triple("Plank", "3 x 60 sec", ""),
                Triple("Bicycle crunch", "3 x 20", "")
            )))
            addAll(rows(2, "Today: Rest / Light Activity", true, listOf(
                Triple("Dead hang", "3 x 30 sec", "Posture work."),
                Triple("Cobra stretch", "30 sec x 3", ""),
                Triple("Cat-cow stretch", "1 min", ""),
                Triple("Optional walk", "20 min", "Only if energy is good.")
            )))
            addAll(rows(3, "Today: Back + Core", false, listOf(
                Triple("Pull-ups", "5 x max", "Use this if a bar is available."),
                Triple("Inverted rows under table", "4 x 10", "Fallback if no bar."),
                Triple("Superman hold", "4 x 30 sec", ""),
                Triple("Reverse snow angels", "3 x 12", ""),
                Triple("Leg raises", "3 x 12", "")
            )))
            addAll(rows(4, "Today: Full Body", false, listOf(
                Triple("Push-ups", "3 x 15", ""),
                Triple("Squats", "3 x 20", ""),
                Triple("Pike push-ups", "3 x 10", ""),
                Triple("Plank", "3 x 45 sec", ""),
                Triple("Jump squats", "3 x 12", "")
            )))
            addAll(rows(5, "Today: Rest", true, listOf(
                Triple("Light stretching", "10-15 min", "Friday is the recovery day.")
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
