package com.jarvis.assistant.device

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.jarvis.assistant.core.ErrorLog

/**
 * Uses inexact AlarmManager scheduling (setAndAllowWhileIdle) so no SCHEDULE_EXACT_ALARM
 * permission is ever requested or required on Android 14+.
 */
class ReminderAlarms(
    private val context: Context,
    private val errorLog: ErrorLog
) {

    fun schedule(reminderId: Long, text: String, triggerAtMs: Long): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderReceiver.EXTRA_REMINDER_TEXT, text)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return runCatching {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            true
        }.onFailure { err ->
            errorLog.record("ReminderAlarms", "Failed to schedule reminder $reminderId", err)
        }.getOrDefault(false)
    }

    fun cancel(reminderId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
        }
        val pi = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            runCatching { am.cancel(pi) }
        }
    }
}
