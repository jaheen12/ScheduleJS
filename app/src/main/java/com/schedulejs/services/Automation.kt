package com.schedulejs.services

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.schedulejs.MainActivity
import com.schedulejs.R
import com.schedulejs.data.local.ScheduleDatabase
import com.schedulejs.data.local.SeedData
import com.schedulejs.data.repository.OfflineScheduleRepository
import com.schedulejs.data.repository.OfflineSettingsRepository
import com.schedulejs.domain.NotificationLeadTime
import com.schedulejs.domain.TaskCategory
import com.schedulejs.domain.TodaySchedule
import com.schedulejs.receivers.ReminderReceiver
import com.schedulejs.receivers.SystemEventReceiver
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object ScheduleAutomationCoordinator {
    fun initialize(context: Context) {
        NotificationChannels.ensureCreated(context)
        val appContext = context.applicationContext
        val serviceIntent = Intent(appContext, PersistentStatusService::class.java)
        ContextCompat.startForegroundService(appContext, serviceIntent)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            ReminderScheduler(appContext).scheduleDailyReminders()
        }
    }
}

object NotificationChannels {
    const val STATUS_CHANNEL_ID = "schedulejs_status"
    const val REMINDERS_CHANNEL_ID = "schedulejs_reminders"
    const val TRANSIT_CHANNEL_ID = "schedulejs_transit"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel(
                STATUS_CHANNEL_ID,
                context.getString(R.string.channel_status_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_status_description)
            },
            NotificationChannel(
                REMINDERS_CHANNEL_ID,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_reminders_description)
            },
            NotificationChannel(
                TRANSIT_CHANNEL_ID,
                context.getString(R.string.channel_transit_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_transit_description)
            }
        )
        notificationManager.createNotificationChannels(channels)
    }
}

class ReminderScheduler(
    private val context: Context,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val database by lazy { ScheduleDatabase.getInstance(context) }
    private val seedData by lazy { SeedData(database) }
    private val scheduleRepository by lazy { OfflineScheduleRepository(database, seedData) }
    private val settingsRepository by lazy { OfflineSettingsRepository(database, seedData) }

    suspend fun scheduleDailyReminders(targetDate: LocalDate = LocalDate.now(clock)) {
        val schedule = scheduleRepository.getTodaySchedule(targetDate)
        val settings = settingsRepository.observeSettings().first()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = LocalDateTime.now(clock)

        schedule.tasks.forEachIndexed { index, task ->
            val triggerTime = triggerDateTime(task = task, schedule = schedule, leadTime = settings.notificationLeadTime)
            if (!triggerTime.isAfter(now)) return@forEachIndexed

            val pendingIntent = ReminderReceiver.pendingIntent(
                context = context,
                requestCode = reminderRequestCode(targetDate, index),
                title = task.title,
                detail = task.details,
                isTransitAlert = task.category == TaskCategory.TRANSIT && settings.transitAlertsEnabled
            )
            scheduleAlarm(
                alarmManager = alarmManager,
                triggerAtMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                pendingIntent = pendingIntent
            )
        }

        scheduleResyncAlarm(alarmManager, targetDate.plusDays(1))
    }

    private fun scheduleResyncAlarm(alarmManager: AlarmManager, nextDate: LocalDate) {
        val triggerAt = nextDate.atTime(LocalTime.of(0, 1))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        scheduleAlarm(
            alarmManager = alarmManager,
            triggerAtMillis = triggerAt,
            pendingIntent = SystemEventReceiver.resyncPendingIntent(context)
        )
    }

    private fun triggerDateTime(
        task: com.schedulejs.domain.ScheduleTask,
        schedule: TodaySchedule,
        leadTime: NotificationLeadTime
    ): LocalDateTime {
        val leadMinutes = when (leadTime) {
            NotificationLeadTime.ON_TIME -> 0
            NotificationLeadTime.FIVE_MINUTES -> 5
            NotificationLeadTime.TEN_MINUTES -> 10
        }
        return schedule.date.atStartOfDay().plusMinutes((task.startMinuteOfDay - leadMinutes).coerceAtLeast(0).toLong())
    }

    private fun reminderRequestCode(date: LocalDate, index: Int): Int {
        return (date.toEpochDay().toInt() * 100) + index
    }

    private fun scheduleAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }.getOrElse {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}

class PersistentStatusService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database by lazy { ScheduleDatabase.getInstance(applicationContext) }
    private val seedData by lazy { SeedData(database) }
    private val scheduleRepository by lazy { OfflineScheduleRepository(database, seedData) }
    private val timeEngine by lazy { DefaultTimeEngine() }
    private val clock: Clock = Clock.systemDefaultZone()

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        startForeground(NOTIFICATION_ID, placeholderNotification())
        serviceScope.launch {
            while (isActive) {
                runCatching { updateNotification() }
                delay(60_000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            runCatching { updateNotification() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun updateNotification() {
        val now = LocalDateTime.now(clock)
        val schedule = scheduleRepository.getTodaySchedule(now.toLocalDate())
        val snapshot = timeEngine.getDashboardSnapshot(schedule, now)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildStatusNotification(snapshot, now))
    }

    private fun placeholderNotification(): Notification {
        return NotificationCompat.Builder(this, NotificationChannels.STATUS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(getString(R.string.notification_status_title))
            .setContentText("Loading current schedule...")
            .setOngoing(true)
            .setContentIntent(contentIntent())
            .build()
    }

    private fun buildStatusNotification(
        snapshot: com.schedulejs.domain.DashboardSnapshot,
        now: LocalDateTime
    ): Notification {
        val current = snapshot.currentTask
        val next = snapshot.nextTask
        val nowMinute = now.hour * 60 + now.minute
        val currentText = if (current == null) {
            "No active block right now."
        } else {
            val remaining = (current.endMinuteOfDay - nowMinute).coerceAtLeast(0)
            "Currently: ${current.title}. ${remaining.toMinuteDurationLabel()} left."
        }
        val nextText = if (next == null) {
            "No further blocks today."
        } else {
            "Next: ${next.title} at ${next.startMinuteOfDay.toClockLabel()}."
        }

        return NotificationCompat.Builder(this, NotificationChannels.STATUS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(getString(R.string.notification_status_title))
            .setContentText(currentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$currentText $nextText"))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .build()
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
    }
}

private fun Int.toClockLabel(): String {
    val hours = this / 60
    val minutes = this % 60
    return "%02d:%02d".format(hours, minutes)
}

private fun Int.toMinuteDurationLabel(): String {
    val totalMinutes = this.coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
