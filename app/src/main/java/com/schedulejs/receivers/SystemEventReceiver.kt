package com.schedulejs.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.schedulejs.services.ScheduleAutomationCoordinator

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ScheduleAutomationCoordinator.initialize(context.applicationContext)
    }

    companion object {
        fun resyncPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, SystemEventReceiver::class.java)
            return PendingIntent.getBroadcast(
                context,
                3001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
