package com.jarvis.assistant.ui.permissions

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.jarvis.assistant.ui.theme.JarvisCyanPrimary
import com.jarvis.assistant.ui.theme.JarvisGreenVerified
import com.jarvis.assistant.ui.theme.JarvisSurfaceCard

@Composable
fun PermissionsScreen() {
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }

    val ignoringBatteryOpt = powerManager.isIgnoringBatteryOptimizations(context.packageName)
    val notifListenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.packageName)

    val declaredPermissions = remember {
        runCatching {
            val pkgInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            pkgInfo.requestedPermissions?.toList() ?: emptyList()
        }.getOrDefault(emptyList())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("RUNTIME DEVICE TELEMETRY", style = MaterialTheme.typography.titleMedium, color = JarvisCyanPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Manufacturer / Model: ${Build.MANUFACTURER} ${Build.MODEL}", style = MaterialTheme.typography.bodyMedium)
                Text("Android Version: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Runtime Memory (ActivityManager): ${memInfo.availMem / (1024 * 1024)} MB free / ${memInfo.totalMem / (1024 * 1024)} MB total",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("PHASE 0 MANIFEST PERMISSION AUDIT", style = MaterialTheme.typography.titleMedium, color = JarvisCyanPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    if (declaredPermissions.isEmpty())
                        "Verified: 0 permissions declared in Phase 0 AndroidManifest.xml (Progressive permission policy active)."
                    else
                        "Declared permissions (${declaredPermissions.size}): ${declaredPermissions.joinToString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisGreenVerified
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Progressive Schedule:\n" +
                        "• Phase 1: RECORD_AUDIO, INTERNET\n" +
                        "• Phase 3: FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE, POST_NOTIFICATIONS, BLUETOOTH_CONNECT\n" +
                        "• Phase 5: SET_ALARM, READ_CALENDAR, WRITE_CALENDAR\n" +
                        "• Phase 6: READ_CONTACTS",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ANDROID SPECIAL ACCESS FLOWS", style = MaterialTheme.typography.titleMedium, color = JarvisCyanPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Battery Optimization Ignored: $ignoringBatteryOpt", style = MaterialTheme.typography.bodyMedium)
                Text("Notification Listener Enabled: $notifListenerEnabled", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                }) {
                    Text("Open Battery Optimization Settings")
                }

                Spacer(modifier = Modifier.height(6.dp))
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                }) {
                    Text("Open Notification Listener Settings")
                }

                Spacer(modifier = Modifier.height(6.dp))
                Button(onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                }) {
                    Text("Open App Info (Realme UI Background Settings)")
                }
            }
        }
    }
}
