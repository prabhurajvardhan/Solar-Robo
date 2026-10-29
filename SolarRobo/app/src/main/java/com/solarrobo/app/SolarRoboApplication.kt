package com.solarrobo.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.solarrobo.app.di.TrackerRuntimeHolder
import com.solarrobo.app.service.TrackerForegroundService
import com.solarrobo.core.contracts.AccessMode
import com.solarrobo.core.contracts.LinkState
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.SafetyContext
import com.solarrobo.core.contracts.SafetyDecision
import com.solarrobo.core.contracts.SafetyGate
import com.solarrobo.core.device.RoboDevice
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@HiltAndroidApp
class SolarRoboApplication : Application(), Application.ActivityLifecycleCallbacks {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var authSession: AuthSession
    @Inject lateinit var trackerRuntimeHolder: TrackerRuntimeHolder
    @Inject lateinit var roboDevice: RoboDevice
    @Inject lateinit var safetyGate: SafetyGate

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
        lifecycleScope.launch {
            authSession.accessMode.collect { mode ->
                if (mode != AccessMode.OPERATOR && trackerRuntimeHolder.engine.value != null) {
                    stopTrackerForRestrictedMode()
                }
            }
        }
    }

    private fun stopTrackerForRestrictedMode() {
        val engine = trackerRuntimeHolder.engine.value ?: return
        lifecycleScope.launch {
            runCatching { engine.stop() }
            val decision = safetyGate.evaluate(
                RoboCommand.StopMotion,
                SafetyContext(LinkState.OFFLINE, statusFresh = false, motorBusy = true, calibrated = false, faultActive = false, windHold = false)
            )
            if (decision == SafetyDecision.Allow) runCatching { roboDevice.sendCommand(RoboCommand.StopMotion) }
            trackerRuntimeHolder.clear()
            stopService(Intent(this@SolarRoboApplication, TrackerForegroundService::class.java))
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
