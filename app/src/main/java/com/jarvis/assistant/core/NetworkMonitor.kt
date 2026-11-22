package com.jarvis.assistant.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetworkState(
    val connected: Boolean = false,
    val validated: Boolean = false,
    val transport: String = "None"
)

class NetworkMonitor(
    context: Context,
    private val errorLog: ErrorLog
) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val _state = MutableStateFlow(currentSnapshot())
    val state: StateFlow<NetworkState> = _state.asStateFlow()

    private var registered = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { _state.value = currentSnapshot() }
        override fun onLost(network: Network) { _state.value = currentSnapshot() }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            _state.value = currentSnapshot()
        }
    }

    fun start() {
        val manager = cm ?: return
        if (registered) return
        runCatching {
            val req = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            manager.registerNetworkCallback(req, callback)
            registered = true
            _state.value = currentSnapshot()
        }.onFailure {
            errorLog.record("NetworkMonitor", "Failed to register network callback", it)
        }
    }

    fun stop() {
        val manager = cm ?: return
        if (!registered) return
        runCatching { manager.unregisterNetworkCallback(callback) }
        registered = false
    }

    fun currentSnapshot(): NetworkState {
        val manager = cm ?: return NetworkState()
        return runCatching {
            val active = manager.activeNetwork ?: return NetworkState()
            val caps = manager.getNetworkCapabilities(active) ?: return NetworkState()
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val transport = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Other"
            }
            NetworkState(connected = hasInternet, validated = validated, transport = transport)
        }.getOrElse {
            errorLog.record("NetworkMonitor", "Snapshot failed", it)
            NetworkState()
        }
    }
}
