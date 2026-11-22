package com.jarvis.assistant.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jarvis.assistant.ui.AssistantViewModel
import com.jarvis.assistant.ui.DiagnosticsViewModel
import com.jarvis.assistant.ui.HistoryViewModel
import com.jarvis.assistant.ui.SettingsViewModel
import com.jarvis.assistant.ui.screens.ChatScreen
import com.jarvis.assistant.ui.screens.DiagnosticsScreen
import com.jarvis.assistant.ui.screens.HistoryScreen
import com.jarvis.assistant.ui.screens.HomeScreen
import com.jarvis.assistant.ui.screens.SettingsScreen
import com.jarvis.assistant.ui.screens.VoiceScreen
import com.jarvis.assistant.voice.MicPermissionState

@Composable
fun JarvisNavHost(
    navController: NavHostController,
    assistantViewModel: AssistantViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    diagnosticsViewModel: DiagnosticsViewModel,
    micPermissionState: MicPermissionState,
    notificationsGranted: Boolean,
    onRequestMicPermission: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = assistantViewModel,
                onNavigateToChat = { navController.navigate(Routes.CHAT) { launchSingleTop = true } },
                onNavigateToVoice = { navController.navigate(Routes.VOICE) { launchSingleTop = true } },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                onNavigateToDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) { launchSingleTop = true } }
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(
                viewModel = assistantViewModel,
                onNavigateToVoice = { navController.navigate(Routes.VOICE) { launchSingleTop = true } }
            )
        }
        composable(Routes.VOICE) {
            VoiceScreen(
                viewModel = assistantViewModel,
                micPermissionState = micPermissionState,
                onRequestMicPermission = onRequestMicPermission,
                onOpenAppSettings = onOpenAppSettings
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = historyViewModel,
                onOpenConversation = { id ->
                    assistantViewModel.openConversation(id)
                    navController.navigate(Routes.CHAT) { launchSingleTop = true }
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                notificationsGranted = notificationsGranted,
                onRequestNotifications = onRequestNotifications,
                onApiKeyChanged = { assistantViewModel.refreshKeyStatus() },
                onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) { launchSingleTop = true } }
            )
        }
        composable(Routes.DIAGNOSTICS) {
            DiagnosticsScreen(
                viewModel = diagnosticsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
