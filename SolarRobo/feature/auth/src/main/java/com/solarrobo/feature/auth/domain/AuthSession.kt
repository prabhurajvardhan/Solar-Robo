package com.solarrobo.feature.auth.domain

import com.solarrobo.core.contracts.AccessMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class AuthSession @Inject constructor() {
    private val mutableMode = MutableStateFlow(AccessMode.VIEWER)
    private val mutableLocked = MutableStateFlow(true)
    val accessMode = mutableMode.asStateFlow()
    val isLocked = mutableLocked.asStateFlow()

    fun unlockOperator() {
        mutableMode.value = AccessMode.OPERATOR
        mutableLocked.value = false
    }

    fun enterViewerMode() {
        mutableMode.value = AccessMode.VIEWER
        mutableLocked.value = false
    }

    fun lock() {
        mutableMode.value = AccessMode.VIEWER
        mutableLocked.value = true
    }
}
