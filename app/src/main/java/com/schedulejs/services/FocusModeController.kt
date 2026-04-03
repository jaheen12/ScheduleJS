package com.schedulejs.services

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

interface FocusModeController {
    fun hasNotificationPolicyAccess(): Boolean
    fun buildPermissionIntent(): Intent
    suspend fun enableFocusMode()
    suspend fun restorePreviousMode()
}

class NotificationPolicyFocusModeController(
    context: Context
) : FocusModeController {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun hasNotificationPolicyAccess(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    override fun buildPermissionIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    override suspend fun enableFocusMode() {
        if (!hasNotificationPolicyAccess()) return
        val currentFilter = notificationManager.currentInterruptionFilter
        if (!preferences.getBoolean(KEY_HAS_SNAPSHOT, false)) {
            preferences.edit()
                .putBoolean(KEY_HAS_SNAPSHOT, true)
                .putInt(KEY_PREVIOUS_FILTER, currentFilter)
                .apply()
        }
        if (currentFilter != NotificationManager.INTERRUPTION_FILTER_NONE) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
    }

    override suspend fun restorePreviousMode() {
        if (!preferences.getBoolean(KEY_HAS_SNAPSHOT, false)) return
        if (!hasNotificationPolicyAccess()) {
            clearSnapshot()
            return
        }
        val previousFilter = preferences.getInt(
            KEY_PREVIOUS_FILTER,
            NotificationManager.INTERRUPTION_FILTER_ALL
        )
        notificationManager.setInterruptionFilter(previousFilter)
        clearSnapshot()
    }

    private fun clearSnapshot() {
        preferences.edit()
            .remove(KEY_HAS_SNAPSHOT)
            .remove(KEY_PREVIOUS_FILTER)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "focus_mode_state"
        const val KEY_HAS_SNAPSHOT = "has_snapshot"
        const val KEY_PREVIOUS_FILTER = "previous_filter"
    }
}
