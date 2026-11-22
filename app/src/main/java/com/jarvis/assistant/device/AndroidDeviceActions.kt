package com.jarvis.assistant.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.Settings
import com.jarvis.assistant.core.ErrorLog
import com.jarvis.assistant.memory.MemoryRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AndroidDeviceActions(
    private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val reminderAlarms: ReminderAlarms,
    private val memoryRepository: MemoryRepository,
    private val errorLog: ErrorLog,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : DeviceActions {

    override suspend fun execute(command: Command): CommandExecutionResult = when (command) {
        Command.None -> CommandExecutionResult(handled = false, reply = "")
        Command.Time -> {
            val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
            CommandExecutionResult(true, "It's ${fmt.format(Date(clock()))}.", true)
        }
        Command.Date -> {
            val fmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
            CommandExecutionResult(true, "Today is ${fmt.format(Date(clock()))}.", true)
        }
        is Command.SetReminder -> {
            val triggerAt = clock() + command.delayMinutes * 60_000L
            val id = reminderRepository.add(command.text, triggerAt)
            val scheduled = reminderAlarms.schedule(id, command.text, triggerAt)
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(triggerAt))
            val notifNote = if (!NotificationHelper.canPostNotifications(context)) {
                " Enable Notifications in Settings so you see the alert."
            } else {
                ""
            }
            val msg = if (scheduled) {
                "Reminder saved for $timeStr: \"${command.text}\".$notifNote"
            } else {
                "Reminder saved locally for $timeStr, though the system alarm could not be scheduled.$notifNote"
            }
            CommandExecutionResult(true, msg, scheduled)
        }
        Command.ListReminders -> {
            val upcoming = reminderRepository.pending(clock())
            if (upcoming.isEmpty()) {
                CommandExecutionResult(true, "You have no upcoming reminders.", true)
            } else {
                val fmt = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
                val lines = upcoming.joinToString("\n") { "• ${it.text} (${fmt.format(Date(it.triggerAt))})" }
                CommandExecutionResult(true, "Upcoming reminders:\n$lines", true)
            }
        }
        is Command.SetTimer -> launchTimer(command.seconds, command.label)
        is Command.OpenSettings -> openSettings(command.section)
        is Command.OpenApp -> openApp(command.appQuery)
        is Command.RememberFact -> {
            memoryRepository.addFact(command.fact)
            CommandExecutionResult(true, "Saved to your local memory: \"${command.fact}\".", true)
        }
        Command.ListMemory -> {
            val facts = memoryRepository.getFacts()
            if (facts.isEmpty()) {
                CommandExecutionResult(true, "No memory facts saved yet. Say \"Remember that ...\" to save one.", true)
            } else {
                val lines = facts.joinToString("\n") { "• ${it.text}" }
                CommandExecutionResult(true, "Saved memory:\n$lines", true)
            }
        }
        Command.Help -> CommandExecutionResult(
            handled = true,
            reply = "Local device commands I can run right away:\n" +
                "• Time & date (\"What time is it?\", \"Today's date\")\n" +
                "• Reminders (\"Remind me in 10 minutes to drink water\", \"Show reminders\")\n" +
                "• Timers (\"Set a timer for 5 minutes\")\n" +
                "• Open apps & settings (\"Open WhatsApp\", \"Open Wi-Fi settings\")\n" +
                "• Local memory (\"Remember that I prefer Tanglish\", \"Show memory\")\n" +
                "Everything else goes directly to your configured AI model.",
            actionTaken = true
        )
    }

    private fun launchTimer(seconds: Int, label: String): CommandExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            CommandExecutionResult(true, "Opened your Clock app with a ${formatDuration(seconds)} timer.", true)
        }.onFailure { err ->
            errorLog.record("DeviceActions", "Timer launch failed", err)
        }.getOrElse {
            CommandExecutionResult(true, "No compatible Clock app was found to start that timer.", false)
        }
    }

    private fun openSettings(section: String): CommandExecutionResult {
        val action = when (section.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            CommandExecutionResult(true, "Opened Android ${section.replaceFirstChar { it.uppercase() }} settings.", true)
        }.onFailure { err ->
            errorLog.record("DeviceActions", "Open settings failed", err)
        }.getOrElse {
            CommandExecutionResult(true, "Could not open that settings screen on this device.", false)
        }
    }

    private fun openApp(query: String): CommandExecutionResult {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val candidates = runCatching {
            pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        }.getOrDefault(emptyList())

        val normalized = query.trim().lowercase()
        val match = candidates.firstOrNull { info ->
            val label = info.loadLabel(pm)?.toString().orEmpty().lowercase()
            label == normalized || label.contains(normalized) || info.activityInfo.packageName.lowercase().contains(normalized)
        }

        if (match == null) {
            return CommandExecutionResult(true, "Couldn't find an installed app matching \"$query\".", false)
        }

        val launch = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: return CommandExecutionResult(true, "Found \"${match.loadLabel(pm)}\", but it has no launchable screen.", false)

        return runCatching {
            context.startActivity(launch)
            CommandExecutionResult(true, "Opening ${match.loadLabel(pm)}.", true)
        }.onFailure { err ->
            errorLog.record("DeviceActions", "App launch failed for $query", err)
        }.getOrElse {
            CommandExecutionResult(true, "Could not launch ${match.loadLabel(pm)}.", false)
        }
    }

    private fun formatDuration(totalSec: Int): String {
        val mins = totalSec / 60
        val secs = totalSec % 60
        return when {
            mins > 0 && secs > 0 -> "${mins}m ${secs}s"
            mins > 0 -> "$mins minute${if (mins == 1) "" else "s"}"
            else -> "$secs second${if (secs == 1) "" else "s"}"
        }
    }
}
