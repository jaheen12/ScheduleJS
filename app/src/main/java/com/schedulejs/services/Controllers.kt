package com.schedulejs.services

import android.media.AudioManager
import android.media.ToneGenerator
import com.schedulejs.data.local.BellyRoutineDao
import com.schedulejs.data.local.BellyRoutineStateEntity
import com.schedulejs.data.local.FocusTimerDao
import com.schedulejs.data.local.FocusTimerStateEntity
import com.schedulejs.domain.BellyRoutineSession
import com.schedulejs.domain.DashboardSnapshot
import com.schedulejs.domain.FocusTimerSession
import com.schedulejs.domain.ScheduleTask
import com.schedulejs.domain.TimerStatus
import com.schedulejs.domain.TodaySchedule
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface TimeEngine {
    fun getDashboardSnapshot(schedule: TodaySchedule, now: LocalDateTime): DashboardSnapshot
}

interface FocusTimerController {
    suspend fun start(durationMinutes: Int, enableDnd: Boolean)
    suspend fun pause()
    suspend fun resume()
    suspend fun cancel()
    suspend fun getState(): FocusTimerSession
}

interface RoutineTimerController {
    suspend fun startBellyRoutine()
    suspend fun pause()
    suspend fun resume()
    suspend fun cancel()
    suspend fun getState(): BellyRoutineSession
}

class DefaultTimeEngine : TimeEngine {
    override fun getDashboardSnapshot(schedule: TodaySchedule, now: LocalDateTime): DashboardSnapshot {
        val nowMinute = now.hour * 60 + now.minute
        val currentTask = schedule.tasks.firstOrNull { nowMinute in it.startMinuteOfDay until it.endMinuteOfDay }
        val nextTask = schedule.tasks.firstOrNull { it.startMinuteOfDay > nowMinute }
        val progressPercent = if (currentTask == null) {
            if (schedule.tasks.isNotEmpty() && nowMinute >= schedule.tasks.last().endMinuteOfDay) 1f else 0f
        } else {
            val duration = (currentTask.endMinuteOfDay - currentTask.startMinuteOfDay).coerceAtLeast(1)
            ((nowMinute - currentTask.startMinuteOfDay).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        }
        return DashboardSnapshot(
            currentTask = currentTask ?: fallbackTask(schedule.tasks, nowMinute),
            nextTask = nextTask,
            progressPercent = progressPercent
        )
    }

    private fun fallbackTask(tasks: List<ScheduleTask>, nowMinute: Int): ScheduleTask? {
        return when {
            tasks.isEmpty() -> null
            nowMinute < tasks.first().startMinuteOfDay -> null
            else -> tasks.last()
        }
    }
}

class RoomFocusTimerController(
    private val focusTimerDao: FocusTimerDao,
    private val clock: Clock = Clock.systemDefaultZone()
) : FocusTimerController {
    override suspend fun start(durationMinutes: Int, enableDnd: Boolean) {
        val totalSeconds = durationMinutes * 60
        focusTimerDao.upsert(
            FocusTimerStateEntity(
                totalDurationSeconds = totalSeconds,
                remainingSeconds = totalSeconds,
                isRunning = true,
                isCompleted = false,
                startedAtEpochMillis = nowMillis(),
                enableDnd = enableDnd
            )
        )
    }

    override suspend fun pause() {
        val entity = focusTimerDao.get() ?: return
        if (!entity.isRunning || entity.startedAtEpochMillis == null) return
        val remaining = computeRemainingSeconds(entity)
        focusTimerDao.upsert(
            entity.copy(
                remainingSeconds = remaining,
                isRunning = false,
                startedAtEpochMillis = null,
                isCompleted = remaining == 0
            )
        )
    }

    override suspend fun resume() {
        val entity = focusTimerDao.get() ?: return
        if (entity.isRunning || entity.isCompleted || entity.remainingSeconds <= 0) return
        focusTimerDao.upsert(
            entity.copy(
                isRunning = true,
                startedAtEpochMillis = nowMillis()
            )
        )
    }

    override suspend fun cancel() {
        focusTimerDao.clear()
    }

    override suspend fun getState(): FocusTimerSession {
        val entity = focusTimerDao.get()
            ?: return FocusTimerSession(TimerStatus.IDLE, 0, 0, enableDnd = false)

        if (entity.isRunning && entity.startedAtEpochMillis != null) {
            val remaining = computeRemainingSeconds(entity)
            if (remaining == 0) {
                val completed = entity.copy(
                    remainingSeconds = 0,
                    isRunning = false,
                    isCompleted = true,
                    startedAtEpochMillis = null
                )
                focusTimerDao.upsert(completed)
                return completed.toDomain(status = TimerStatus.COMPLETED)
            }
            return entity.copy(remainingSeconds = remaining).toDomain(status = TimerStatus.RUNNING)
        }

        return when {
            entity.isCompleted -> entity.toDomain(status = TimerStatus.COMPLETED)
            entity.remainingSeconds < entity.totalDurationSeconds -> entity.toDomain(status = TimerStatus.PAUSED)
            else -> entity.toDomain(status = TimerStatus.IDLE)
        }
    }

    private fun computeRemainingSeconds(entity: FocusTimerStateEntity): Int {
        val startedAt = entity.startedAtEpochMillis ?: return entity.remainingSeconds
        val elapsedSeconds = ((nowMillis() - startedAt) / 1000L).toInt().coerceAtLeast(0)
        return (entity.remainingSeconds - elapsedSeconds).coerceAtLeast(0)
    }

    private fun FocusTimerStateEntity.toDomain(status: TimerStatus = TimerStatus.PAUSED): FocusTimerSession {
        return FocusTimerSession(
            status = status,
            totalDurationSeconds = totalDurationSeconds,
            remainingSeconds = remainingSeconds,
            enableDnd = enableDnd
        )
    }

    private fun nowMillis(): Long = clock.millis()
}

class RoomRoutineTimerController(
    private val bellyRoutineDao: BellyRoutineDao,
    private val cuePlayer: RoutineCuePlayer,
    private val clock: Clock = Clock.systemDefaultZone()
) : RoutineTimerController {
    private val stepDurations = listOf(60, 60, 60, 30)
    private val totalDurationSeconds = stepDurations.sum()

    override suspend fun startBellyRoutine() {
        bellyRoutineDao.upsert(
            BellyRoutineStateEntity(
                accumulatedElapsedSeconds = 0,
                isRunning = true,
                isCompleted = false,
                startedAtEpochMillis = nowMillis(),
                lastCueStepIndex = 0
            )
        )
    }

    override suspend fun pause() {
        val entity = bellyRoutineDao.get() ?: return
        if (!entity.isRunning || entity.startedAtEpochMillis == null) return
        val elapsed = computeElapsedSeconds(entity)
        bellyRoutineDao.upsert(
            entity.copy(
                accumulatedElapsedSeconds = elapsed,
                isRunning = false,
                isCompleted = elapsed >= totalDurationSeconds,
                startedAtEpochMillis = null
            )
        )
    }

    override suspend fun resume() {
        val entity = bellyRoutineDao.get() ?: return
        if (entity.isRunning || entity.isCompleted || entity.accumulatedElapsedSeconds >= totalDurationSeconds) return
        bellyRoutineDao.upsert(
            entity.copy(
                isRunning = true,
                startedAtEpochMillis = nowMillis()
            )
        )
    }

    override suspend fun cancel() {
        bellyRoutineDao.clear()
    }

    override suspend fun getState(): BellyRoutineSession {
        val entity = bellyRoutineDao.get()
            ?: return BellyRoutineSession(TimerStatus.IDLE, totalDurationSeconds, totalDurationSeconds, 0, stepDurations.first())

        val elapsed = computeElapsedSeconds(entity)
        if (elapsed >= totalDurationSeconds) {
            val completed = entity.copy(
                accumulatedElapsedSeconds = totalDurationSeconds,
                isRunning = false,
                isCompleted = true,
                startedAtEpochMillis = null,
                lastCueStepIndex = stepDurations.lastIndex
            )
            bellyRoutineDao.upsert(completed)
            return completed.toDomain(TimerStatus.COMPLETED)
        }

        val currentStepIndex = stepIndexForElapsed(elapsed)
        if (entity.isRunning && currentStepIndex > entity.lastCueStepIndex) {
            cuePlayer.playTransitionCue()
            bellyRoutineDao.upsert(entity.copy(lastCueStepIndex = currentStepIndex))
        }

        return when {
            entity.isRunning -> entity.copy(accumulatedElapsedSeconds = elapsed).toDomain(TimerStatus.RUNNING)
            entity.isCompleted -> entity.toDomain(TimerStatus.COMPLETED)
            entity.accumulatedElapsedSeconds > 0 -> entity.toDomain(TimerStatus.PAUSED)
            else -> entity.toDomain(TimerStatus.IDLE)
        }
    }

    private fun BellyRoutineStateEntity.toDomain(status: TimerStatus): BellyRoutineSession {
        val elapsed = accumulatedElapsedSeconds.coerceAtMost(totalDurationSeconds)
        val stepIndex = stepIndexForElapsed(elapsed)
        return BellyRoutineSession(
            status = status,
            totalDurationSeconds = totalDurationSeconds,
            remainingSeconds = (totalDurationSeconds - elapsed).coerceAtLeast(0),
            currentStepIndex = stepIndex,
            stepRemainingSeconds = stepRemainingSeconds(elapsed)
        )
    }

    private fun computeElapsedSeconds(entity: BellyRoutineStateEntity): Int {
        val runtimeElapsed = if (entity.isRunning && entity.startedAtEpochMillis != null) {
            ((nowMillis() - entity.startedAtEpochMillis) / 1000L).toInt().coerceAtLeast(0)
        } else {
            0
        }
        return (entity.accumulatedElapsedSeconds + runtimeElapsed).coerceAtMost(totalDurationSeconds)
    }

    private fun stepIndexForElapsed(elapsedSeconds: Int): Int {
        var boundary = 0
        stepDurations.forEachIndexed { index, duration ->
            boundary += duration
            if (elapsedSeconds < boundary) return index
        }
        return stepDurations.lastIndex
    }

    private fun stepRemainingSeconds(elapsedSeconds: Int): Int {
        var consumedBeforeStep = 0
        stepDurations.forEach { duration ->
            if (elapsedSeconds < consumedBeforeStep + duration) {
                return (consumedBeforeStep + duration - elapsedSeconds).coerceAtLeast(0)
            }
            consumedBeforeStep += duration
        }
        return 0
    }

    private fun nowMillis(): Long = clock.millis()
}

interface RoutineCuePlayer {
    suspend fun playTransitionCue()
}

class ToneRoutineCuePlayer : RoutineCuePlayer {
    override suspend fun playTransitionCue() {
        withContext(Dispatchers.Default) {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                try {
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
                    Thread.sleep(180)
                } finally {
                    tone.release()
                }
            }
        }
    }
}
