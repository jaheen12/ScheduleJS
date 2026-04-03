package com.schedulejs.services

import com.schedulejs.domain.DashboardSnapshot
import com.schedulejs.domain.ScheduleTask
import com.schedulejs.domain.TodaySchedule
import java.time.LocalDateTime

data class DashboardLiveContent(
    val currentTitle: String,
    val currentTimeLabel: String,
    val currentSubtitle: String,
    val nextTitle: String,
    val nextTimeLabel: String,
    val nextSubtitle: String,
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
        val nowMinute = now.hour * 60 + now.minute
        val current = snapshot.currentTask.toCurrentContent(nowMinute)
        val next = snapshot.nextTask.toNextContent()
        return DashboardLiveContent(
            currentTitle = current.title,
            currentTimeLabel = current.timeLabel,
            currentSubtitle = current.subtitle,
            nextTitle = next.title,
            nextTimeLabel = next.timeLabel,
            nextSubtitle = next.subtitle,
            progressPercent = progressPercent(schedule, snapshot, nowMinute)
        )
    }

    private fun progressPercent(
        schedule: TodaySchedule,
        snapshot: DashboardSnapshot,
        nowMinute: Int
    ): Float {
        val currentTask = snapshot.currentTask
        val isLiveTask = currentTask != null && nowMinute in currentTask.startMinuteOfDay until currentTask.endMinuteOfDay
        if (isLiveTask) {
            return snapshot.progressPercent
        }
        return if (schedule.tasks.isNotEmpty() && nowMinute >= schedule.tasks.last().endMinuteOfDay) 1f else 0f
    }
}

private data class TaskContent(
    val title: String,
    val timeLabel: String,
    val subtitle: String
)

private fun ScheduleTask?.toCurrentContent(nowMinute: Int): TaskContent {
    if (this == null) {
        return TaskContent(
            title = "No active block",
            timeLabel = "--",
            subtitle = "Waiting for the first task of the day."
        )
    }
    val isLive = nowMinute in startMinuteOfDay until endMinuteOfDay
    val remainingMinutes = (endMinuteOfDay - nowMinute).coerceAtLeast(0)
    return TaskContent(
        title = title,
        timeLabel = "${startMinuteOfDay.toClockLabel()} - ${endMinuteOfDay.toClockLabel()}",
        subtitle = if (isLive) {
            "Remaining: ${remainingMinutes.toMinuteDurationLabel()}"
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
