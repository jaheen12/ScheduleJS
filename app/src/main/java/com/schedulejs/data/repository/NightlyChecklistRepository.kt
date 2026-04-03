package com.schedulejs.data.repository

import com.schedulejs.data.local.NightlyChecklistDao
import com.schedulejs.data.local.NightlyChecklistEntity
import java.time.Clock
import java.time.LocalDate

data class NightlyChecklistState(
    val date: LocalDate,
    val clothesLaidOut: Boolean,
    val lunchPrepped: Boolean,
    val bagPacked: Boolean,
    val isComplete: Boolean
)

class NightlyChecklistRepository(
    private val checklistDao: NightlyChecklistDao,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    suspend fun getForDate(date: LocalDate = LocalDate.now(clock)): NightlyChecklistState {
        val entity = checklistDao.getForDate(date.toString())
        return NightlyChecklistState(
            date = date,
            clothesLaidOut = entity?.clothesLaidOut ?: false,
            lunchPrepped = entity?.lunchPrepped ?: false,
            bagPacked = entity?.bagPacked ?: false,
            isComplete = entity?.completedAtEpochMillis != null
        )
    }

    suspend fun update(
        date: LocalDate,
        clothesLaidOut: Boolean,
        lunchPrepped: Boolean,
        bagPacked: Boolean
    ): NightlyChecklistState {
        val completedAt = if (clothesLaidOut && lunchPrepped && bagPacked) clock.millis() else null
        checklistDao.upsert(
            NightlyChecklistEntity(
                date = date.toString(),
                clothesLaidOut = clothesLaidOut,
                lunchPrepped = lunchPrepped,
                bagPacked = bagPacked,
                completedAtEpochMillis = completedAt
            )
        )
        return getForDate(date)
    }
}
