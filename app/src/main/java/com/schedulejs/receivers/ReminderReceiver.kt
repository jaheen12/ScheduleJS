package com.schedulejs.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.schedulejs.MainActivity
import com.schedulejs.R
import com.schedulejs.services.NotificationChannels

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isTransitAlert = intent.getBooleanExtra(EXTRA_IS_TRANSIT, false)
        val channelId = if (isTransitAlert) {
            NotificationChannels.TRANSIT_CHANNEL_ID
        } else {
            NotificationChannels.REMINDERS_CHANNEL_ID
        }
        val title = if (isTransitAlert) {
            context.getString(R.string.notification_transit_title)
        } else {
            context.getString(R.string.notification_reminder_title)
        }
        val content = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val detail = intent.getStringExtra(EXTRA_DETAIL).orEmpty()

        NotificationManagerCompat.from(context).notify(
            content.hashCode(),
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$content\n$detail"))
                .setPriority(if (isTransitAlert) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent(context))
                .build()
        )
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_DETAIL = "detail"
        private const val EXTRA_IS_TRANSIT = "is_transit"

        fun pendingIntent(
            context: Context,
            requestCode: Int,
            title: String,
            detail: String,
            isTransitAlert: Boolean
        ): PendingIntent {
            val intent = Intent(context, ReminderReceiver::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_DETAIL, detail)
                .putExtra(EXTRA_IS_TRANSIT, isTransitAlert)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun contentIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
            return PendingIntent.getActivity(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
