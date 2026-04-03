package com.schedulejs

import com.schedulejs.ui.DemoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDataTest {
    @Test
    fun dashboardContainsCurrentAndUpcomingTasks() {
        assertEquals("Office", DemoData.dashboard.currentTask.title)
        assertTrue(DemoData.dashboard.timelineItems.any { it.title == "Evening Study" })
    }

    @Test
    fun reviewStartsLockedForStaticPreview() {
        assertFalse(DemoData.review.isUnlocked)
    }
}
