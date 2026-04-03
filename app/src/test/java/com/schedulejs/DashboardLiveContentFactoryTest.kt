package com.schedulejs

import com.schedulejs.domain.DashboardSnapshot
import com.schedulejs.domain.DayType
import com.schedulejs.domain.ScheduleTask
import com.schedulejs.domain.TaskCategory
import com.schedulejs.domain.TodaySchedule
import com.schedulejs.services.DashboardLiveContentFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DashboardLiveContentFactoryTest {
    @Test
    fun createsLiveCurrentAndNextContentWhileTaskIsRunning() {
        val commute = task(
            id = 1L,
            title = "Commute",
            startMinuteOfDay = 8 * 60,
            endMinuteOfDay = 8 * 60 + 30
        )
        val office = task(
            id = 2L,
            title = "Office",
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 17 * 60
        )
        val schedule = TodaySchedule(
            date = LocalDate.of(2026, 4, 3),
            dayType = DayType.OFFICE_DAY,
            tasks = listOf(commute, office)
        )

        val content = DashboardLiveContentFactory.create(
            schedule = schedule,
            snapshot = DashboardSnapshot(currentTask = commute, nextTask = office, progressPercent = 0.5f),
            now = LocalDateTime.of(2026, 4, 3, 8, 15)
        )

        assertEquals("Commute", content.currentTitle)
        assertEquals("08:00 - 08:30", content.currentTimeLabel)
        assertEquals("Remaining: 15m", content.currentSubtitle)
        assertEquals("Office", content.nextTitle)
        assertEquals("09:00", content.nextTimeLabel)
        assertEquals(50, content.progressPercentInt)
    }

    @Test
    fun marksProgressCompleteAfterLastBlock() {
        val office = task(
            id = 1L,
            title = "Office",
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 17 * 60
        )
        val schedule = TodaySchedule(
            date = LocalDate.of(2026, 4, 3),
            dayType = DayType.OFFICE_DAY,
            tasks = listOf(office)
        )

        val content = DashboardLiveContentFactory.create(
            schedule = schedule,
            snapshot = DashboardSnapshot(currentTask = office, nextTask = null, progressPercent = 1f),
            now = LocalDateTime.of(2026, 4, 3, 17, 30)
        )

        assertEquals("No further blocks today", content.nextTitle)
        assertTrue(content.currentSubtitle.contains("Current block resolved"))
        assertEquals(100, content.progressPercentInt)
    }

    private fun task(
        id: Long,
        title: String,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int
    ): ScheduleTask {
        return ScheduleTask(
            id = id,
            title = title,
            startMinuteOfDay = startMinuteOfDay,
            endMinuteOfDay = endMinuteOfDay,
            category = TaskCategory.OFFICE,
            details = "$title details",
            dayType = DayType.OFFICE_DAY
        )
    }
}
