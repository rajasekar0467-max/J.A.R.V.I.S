package com.jarvis.assistant.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jarvis.assistant.conversation.DatabaseProvider
import com.jarvis.assistant.core.ErrorLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val now = System.currentTimeMillis()
                val dao = DatabaseProvider.get(appContext).reminderDao()
                val alarms = ReminderAlarms(appContext, ErrorLog())
                dao.pending(now).forEach { entity ->
                    alarms.schedule(entity.id, entity.text, entity.triggerAt)
                }
            } catch (_: Throwable) {
                // Best-effort boot rescheduling; never crash broadcast receiver.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
