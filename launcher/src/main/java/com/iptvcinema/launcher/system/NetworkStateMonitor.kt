package com.iptvcinema.launcher.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetworkState { Wifi, Ethernet, Other, Offline }

/** The default network's kind, for the header; registered only while the home screen is in front. */
class NetworkStateMonitor(context: Context) {
    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val _state = MutableStateFlow(NetworkState.Offline)
    val state: StateFlow<NetworkState> = _state.asStateFlow()
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun start() {
        val manager = manager ?: return
        _state.value = stateOf(manager.getNetworkCapabilities(manager.activeNetwork))
        if (callback != null) return
        callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                _state.value = stateOf(capabilities)
            }

            override fun onLost(network: Network) {
                _state.value = NetworkState.Offline
            }
        }.also { runCatching { manager.registerDefaultNetworkCallback(it) } }
    }

    fun stop() {
        val current = callback ?: return
        runCatching { manager?.unregisterNetworkCallback(current) }
        callback = null
    }

    private fun stateOf(capabilities: NetworkCapabilities?): NetworkState = when {
        capabilities == null -> NetworkState.Offline
        !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetworkState.Offline
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkState.Wifi
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkState.Ethernet
        else -> NetworkState.Other
    }
}
