package com.solarrobo.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.solarrobo.feature.auth.domain.AuthRepository
import com.solarrobo.feature.auth.domain.AuthSession
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class SolarRoboApplication : Application(), Application.ActivityLifecycleCallbacks {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var authSession: AuthSession

    private val lifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var startedActivities = 0
    private var backgroundAtMillis: Long? = null
    @Volatile private var timeoutMillis = 5 * 60_000L
    private var autoLockJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        lifecycleScope.launch {
            authRepository.profile.collect { profile ->
                timeoutMillis = (profile?.autoLockMinutes ?: 5) * 60_000L
            }
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (startedActivities == 0) {
            val backgroundAt = backgroundAtMillis
            if (backgroundAt != null && System.currentTimeMillis() - backgroundAt >= timeoutMillis) {
                authSession.lock()
            }
            backgroundAtMillis = null
            autoLockJob?.cancel()
        }
        startedActivities++
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0) {
            backgroundAtMillis = System.currentTimeMillis()
            if (authSession.accessMode.value == com.solarrobo.core.contracts.AccessMode.OPERATOR && !authSession.isLocked.value) {
                val backgroundStart = backgroundAtMillis ?: return
                autoLockJob?.cancel()
                autoLockJob = lifecycleScope.launch {
                    val profile = authRepository.profile.first()
                    timeoutMillis = (profile?.autoLockMinutes ?: 5) * 60_000L
                    val elapsed = System.currentTimeMillis() - backgroundStart
                    delay((timeoutMillis - elapsed).coerceAtLeast(0L))
                    if (startedActivities == 0) authSession.lock()
                }
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
