package com.jarvis.assistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.conversation.ExportManager
import com.jarvis.assistant.core.CrashLogger
import com.jarvis.assistant.core.ErrorLog
import com.jarvis.assistant.diagnostics.DiagnosticsCollector
import com.jarvis.assistant.diagnostics.DiagnosticsReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiagnosticsViewModel(
    private val collector: DiagnosticsCollector,
    private val exportManager: ExportManager,
    private val crashLogger: CrashLogger?,
    private val errorLog: ErrorLog
) : ViewModel() {

    private val _report = MutableStateFlow<DiagnosticsReport?>(null)
    val report: StateFlow<DiagnosticsReport?> = _report.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun refresh(runNetworkProbe: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _report.value = collector.collect(runNetworkProbe = runNetworkProbe)
            _isLoading.value = false
        }
    }

    fun shareReport() {
        val current = _report.value ?: return
        val res = exportManager.shareTextExport("jarvis_diagnostics.txt", current.toShareableText())
        _statusMessage.value = if (res.isSuccess) "Redacted diagnostics report ready to share." else "Could not open share sheet."
    }

    fun clearLogs() {
        crashLogger?.clear()
        errorLog.clear()
        _statusMessage.value = "Crash and error logs cleared."
        refresh(runNetworkProbe = false)
    }

    fun clearStatus() {
        _statusMessage.value = null
    }
}
