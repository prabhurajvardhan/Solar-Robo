package com.solarrobo.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeNetworkMonitor(
    initialStatus: NetworkStatus = NetworkStatus.CONNECTED
) : NetworkMonitor {

    private val _status = MutableStateFlow(initialStatus)

    override fun observeNetworkStatus(): Flow<NetworkStatus> = _status.asStateFlow()

    override val isConnected: Boolean
        get() = _status.value == NetworkStatus.CONNECTED

    fun setStatus(status: NetworkStatus) {
        _status.value = status
    }
}
