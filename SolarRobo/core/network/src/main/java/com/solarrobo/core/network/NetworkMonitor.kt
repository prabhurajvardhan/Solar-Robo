package com.solarrobo.core.network

import kotlinx.coroutines.flow.Flow

/**
 * Network connectivity monitoring contract.
 */
interface NetworkMonitor {
    fun observeNetworkStatus(): Flow<NetworkStatus>
    val isConnected: Boolean
}
