package com.schedulejs.services

import com.schedulejs.domain.DashboardSnapshot
import com.schedulejs.domain.ScheduleTask
import com.schedulejs.domain.TaskCategory
import com.schedulejs.domain.TodaySchedule
import java.time.LocalDateTime

data class DashboardLiveContent(
    val currentTitle: String,
    val currentTimeLabel: String,
    val currentSubtitle: String,
    val currentTaskCategory: TaskCategory? = null,
    val nextTitle: String,
    val nextTimeLabel: String,
    val nextSubtitle: String,
    val nextTaskCategory: TaskCategory? = null,
    val progressPercent: Float
) {
    val progressPercentInt: Int
        get() = (progressPercent * 100).toInt().coerceIn(0, 100)

    fun currentSentence(): String {
        return if (currentTimeLabel == "--") {
            currentTitle
        } else {
            "$currentTitle ($currentTimeLabel)"
        }
    }

    fun nextSentence(): String {
        return if (nextTimeLabel == "--") {
            nextTitle
        } else {
            "$nextTitle at $nextTimeLabel"
        }
    }
}

object DashboardLiveContentFactory {
    fun create(
        schedule: TodaySchedule,
        snapshot: DashboardSnapshot,
        now: LocalDateTime
    ): DashboardLiveContent {
        val current = snapshot.currentTask.toCurrentContent(now)
        val next = snapshot.nextTask.toNextContent()
        return DashboardLiveContent(
            currentTitle = current.title,
            currentTimeLabel = current.timeLabel,
            currentSubtitle = current.subtitle,
            currentTaskCategory = snapshot.currentTask?.category,
            nextTitle = next.title,
            nextTimeLabel = next.timeLabel,
            nextSubtitle = next.subtitle,
            nextTaskCategory = snapshot.nextTask?.category,
            progressPercent = progressPercent(schedule, snapshot, now)
        )
    }

    private fun progressPercent(
        schedule: TodaySchedule,
        snapshot: DashboardSnapshot,
        now: LocalDateTime
    ): Float {
        val currentTask = snapshot.currentTask
        if (currentTask != null) {
            val nowSeconds = now.toLocalTime().toSecondOfDay()
            val startSeconds = currentTask.startMinuteOfDay * 60
            val endSeconds = currentTask.endMinuteOfDay * 60
            val isLiveTask = nowSeconds in startSeconds until endSeconds
            if (isLiveTask) {
                val durationSeconds = (endSeconds - startSeconds).coerceAtLeast(1)
                val elapsedSeconds = (nowSeconds - startSeconds).coerceIn(0, durationSeconds)
                return elapsedSeconds.toFloat() / durationSeconds.toFloat()
            }
        }
        val nowMinute = now.hour * 60 + now.minute
        return if (schedule.tasks.isNotEmpty() && nowMinute >= schedule.tasks.last().endMinuteOfDay) 1f else 0f
    }
}

private data class TaskContent(
    val title: String,
    val timeLabel: String,
    val subtitle: String
)

private fun ScheduleTask?.toCurrentContent(now: LocalDateTime): TaskContent {
    if (this == null) {
        return TaskContent(
            title = "No active block",
            timeLabel = "--",
            subtitle = "Waiting for the first task of the day."
        )
    }
    val nowSeconds = now.toLocalTime().toSecondOfDay()
    val startSeconds = startMinuteOfDay * 60
    val endSeconds = endMinuteOfDay * 60
    val isLive = nowSeconds in startSeconds until endSeconds
    val remainingSeconds = (endSeconds - nowSeconds).coerceAtLeast(0)
    return TaskContent(
        title = title,
        timeLabel = "${startMinuteOfDay.toClockLabel()} - ${endMinuteOfDay.toClockLabel()}",
        subtitle = if (isLive) {
            "Remaining: ${remainingSeconds.toDurationLabel()}"
        } else {
            "Current block resolved from today's template."
        }
    )
}

private fun ScheduleTask?.toNextContent(): TaskContent {
    return if (this == null) {
        TaskContent(
            title = "No further blocks today",
            timeLabel = "--",
            subtitle = "End of schedule."
        )
    } else {
        TaskContent(
            title = title,
            timeLabel = startMinuteOfDay.toClockLabel(),
            subtitle = details
        )
    }
}

fun Int.toClockLabel(): String {
    val hours = this / 60
    val minutes = this % 60
    return "%02d:%02d".format(hours, minutes)
}

fun Int.toMinuteDurationLabel(): String {
    val totalMinutes = this.coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

private fun Int.toDurationLabel(): String {
    val totalSeconds = coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 && minutes > 0 && seconds > 0 -> "${hours}h ${minutes}m ${seconds}s"
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 && seconds > 0 -> "${hours}h ${seconds}s"
        hours > 0 -> "${hours}h"
        minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}
