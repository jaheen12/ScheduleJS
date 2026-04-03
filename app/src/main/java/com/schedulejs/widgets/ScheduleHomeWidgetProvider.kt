package com.schedulejs.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.schedulejs.MainActivity
import com.schedulejs.R
import com.schedulejs.data.local.ScheduleDatabase
import com.schedulejs.data.local.SeedData
import com.schedulejs.data.repository.OfflineScheduleRepository
import com.schedulejs.services.DashboardLiveContent
import com.schedulejs.services.DashboardLiveContentFactory
import com.schedulejs.services.DefaultTimeEngine
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleHomeWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ScheduleHomeWidgetUpdater.requestUpdate(context.applicationContext)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        ScheduleHomeWidgetUpdater.requestUpdate(context.applicationContext)
    }
}

object ScheduleHomeWidgetUpdater {
    private val updateScope = CoroutineScope(Dispatchers.IO)

    fun requestUpdate(context: Context) {
        updateScope.launch {
            updateAll(context.applicationContext)
        }
    }

    suspend fun updateAll(
        context: Context,
        clock: Clock = Clock.systemDefaultZone()
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, ScheduleHomeWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (widgetIds.isEmpty()) return

        val database = ScheduleDatabase.getInstance(context)
        val seedData = SeedData(database)
        val scheduleRepository = OfflineScheduleRepository(database, seedData)
        val now = LocalDateTime.now(clock)
        val schedule = scheduleRepository.getTodaySchedule(now.toLocalDate())
        val snapshot = DefaultTimeEngine().getDashboardSnapshot(schedule, now)
        val content = DashboardLiveContentFactory.create(schedule, snapshot, now)

        widgetIds.forEach { widgetId ->
            appWidgetManager.updateAppWidget(widgetId, buildRemoteViews(context, content))
        }
    }

    private fun buildRemoteViews(
        context: Context,
        content: DashboardLiveContent
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.schedule_home_widget).apply {
            setTextViewText(R.id.widget_header, context.getString(R.string.widget_title))
            setTextViewText(R.id.widget_current_label, context.getString(R.string.widget_current_label))
            setTextViewText(R.id.widget_current_title, content.currentTitle)
            setTextViewText(R.id.widget_current_time, content.currentTimeLabel)
            setTextViewText(R.id.widget_current_subtitle, content.currentSubtitle)
            setTextViewText(R.id.widget_next_label, context.getString(R.string.widget_next_label))
            setTextViewText(R.id.widget_next_title, content.nextTitle)
            setTextViewText(R.id.widget_next_time, content.nextTimeLabel)
            setTextViewText(R.id.widget_next_subtitle, content.nextSubtitle)
            setProgressBar(R.id.widget_progress, 100, content.progressPercentInt, false)
            setTextViewText(
                R.id.widget_progress_label,
                context.getString(R.string.widget_progress_value, content.progressPercentInt)
            )
            setOnClickPendingIntent(R.id.widget_root, launchAppPendingIntent(context))
        }
    }

    private fun launchAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            4001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
