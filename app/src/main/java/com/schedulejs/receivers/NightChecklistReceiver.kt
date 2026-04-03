package com.schedulejs.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.schedulejs.NightChecklistActivity
import com.schedulejs.R
import com.schedulejs.services.NotificationChannels

class NightChecklistReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val activityIntent = Intent(context, NightChecklistActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        runCatching {
            ContextCompat.startActivity(context, activityIntent, null)
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            4001,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationManagerCompat.from(context).notify(
            CHECKLIST_NOTIFICATION_ID,
            NotificationCompat.Builder(context, NotificationChannels.CHECKLIST_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(context.getString(R.string.notification_checklist_title))
                .setContentText(context.getString(R.string.notification_checklist_text))
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        context.getString(R.string.notification_checklist_text)
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setFullScreenIntent(contentIntent, true)
                .setContentIntent(contentIntent)
                .build()
        )
    }

    companion object {
        private const val CHECKLIST_NOTIFICATION_ID = 4002

        fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, NightChecklistReceiver::class.java)
            return PendingIntent.getBroadcast(
                context,
                4000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
