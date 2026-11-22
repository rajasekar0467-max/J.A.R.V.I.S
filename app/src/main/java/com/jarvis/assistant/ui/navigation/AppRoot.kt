package com.jarvis.assistant.ui.navigation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jarvis.assistant.core.AppContainer
import com.jarvis.assistant.device.NotificationHelper
import com.jarvis.assistant.ui.AssistantViewModel
import com.jarvis.assistant.ui.DiagnosticsViewModel
import com.jarvis.assistant.ui.HistoryViewModel
import com.jarvis.assistant.ui.JarvisViewModelFactory
import com.jarvis.assistant.ui.SettingsViewModel
import com.jarvis.assistant.ui.components.ChatGlyph
import com.jarvis.assistant.ui.components.HistoryGlyph
import com.jarvis.assistant.ui.components.JarvisHeader
import com.jarvis.assistant.ui.components.MicGlyph
import com.jarvis.assistant.ui.screens.StartupScreen
import com.jarvis.assistant.ui.theme.DeepEmerald
import com.jarvis.assistant.ui.theme.EmeraldPrimary
import com.jarvis.assistant.ui.theme.SurfaceEmerald
import com.jarvis.assistant.ui.theme.TextSecondary
import com.jarvis.assistant.ui.theme.VoidBlack
import com.jarvis.assistant.voice.MicPermissionResolver

@Composable
fun AppRoot(container: AppContainer) {
    val initialized by container.initialized.collectAsState()

    LaunchedEffect(Unit) {
        container.initialize()
    }

    if (!initialized) {
        StartupScreen()
        return
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val factory = remember(container) { JarvisViewModelFactory(container) }
    val assistantVm: AssistantViewModel = viewModel(factory = factory)
    val historyVm: HistoryViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val diagnosticsVm: DiagnosticsViewModel = viewModel(factory = factory)

    var hasRequestedMic by remember { mutableStateOf(false) }
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    var notificationsGranted by remember {
        mutableStateOf(NotificationHelper.canPostNotifications(context))
    }
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRequestedMic = true
        micGranted = granted
    }

    val shouldShowRationale = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
    } ?: false

    val micState = MicPermissionResolver.resolve(
        isGranted = micGranted,
        hasRequestedBefore = hasRequestedMic,
        shouldShowRationale = shouldShowRationale
    )

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.HOME
    val networkState by assistantVm.networkState.collectAsState()
    val settings by assistantVm.settings.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = VoidBlack,
        topBar = {
            JarvisHeader(
                title = when (currentRoute) {
                    Routes.CHAT -> "JARVIS Chat"
                    Routes.VOICE -> "JARVIS Voice"
                    Routes.HISTORY -> "History"
                    Routes.SETTINGS -> "Settings"
                    Routes.DIAGNOSTICS -> "Diagnostics"
                    else -> "JARVIS"
                },
                networkState = networkState,
                languageMode = settings.language,
                onOpenDiagnostics = {
                    if (currentRoute != Routes.DIAGNOSTICS) {
                        navController.navigate(Routes.DIAGNOSTICS) { launchSingleTop = true }
                    }
                }
            )
        },
        bottomBar = {
            if (currentRoute in Routes.BOTTOM_TABS) {
                NavigationBar(containerColor = DeepEmerald) {
                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = SurfaceEmerald,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                    fun selectTab(route: String) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    NavigationBarItem(
                        selected = currentRoute == Routes.HOME,
                        onClick = { selectTab(Routes.HOME) },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.CHAT,
                        onClick = { selectTab(Routes.CHAT) },
                        icon = { ChatGlyph(color = if (currentRoute == Routes.CHAT) EmeraldPrimary else TextSecondary) },
                        label = { Text("Chat") },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.VOICE,
                        onClick = { selectTab(Routes.VOICE) },
                        icon = { MicGlyph(color = if (currentRoute == Routes.VOICE) EmeraldPrimary else TextSecondary, size = 22.dp) },
                        label = { Text("Voice") },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.HISTORY,
                        onClick = { selectTab(Routes.HISTORY) },
                        icon = { HistoryGlyph(color = if (currentRoute == Routes.HISTORY) EmeraldPrimary else TextSecondary) },
                        label = { Text("History") },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = { selectTab(Routes.SETTINGS) },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = itemColors
                    )
                }
            }
        }
    ) { innerPadding ->
        JarvisNavHost(
            navController = navController,
            assistantViewModel = assistantVm,
            historyViewModel = historyVm,
            settingsViewModel = settingsVm,
            diagnosticsViewModel = diagnosticsVm,
            micPermissionState = micState,
            notificationsGranted = notificationsGranted,
            onRequestMicPermission = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onRequestNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onOpenAppSettings = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}
