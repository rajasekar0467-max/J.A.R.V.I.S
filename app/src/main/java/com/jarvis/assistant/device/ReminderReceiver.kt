package com.jarvis.assistant.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jarvis.assistant.conversation.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER) return
        val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val text = intent.getStringExtra(EXTRA_REMINDER_TEXT).orEmpty().ifBlank { "Scheduled reminder" }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                NotificationHelper.showReminderNotification(context.applicationContext, id, text)
                if (id > 0L) {
                    runCatching {
                        DatabaseProvider.get(context.applicationContext).reminderDao().markFired(id)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMINDER = "com.jarvis.assistant.ACTION_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TEXT = "extra_reminder_text"
    }
}
