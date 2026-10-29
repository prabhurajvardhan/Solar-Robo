package com.solarrobo.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.solarrobo.app.MainActivity
import com.solarrobo.app.R
import com.solarrobo.app.di.TrackerRuntimeFactory
import com.solarrobo.app.di.TrackerRuntimeHolder
import com.solarrobo.app.weather.WeatherRuntime
import com.solarrobo.core.camera.CameraXFrameSource
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.LinkState
import com.solarrobo.core.contracts.SafetyContext
import com.solarrobo.core.contracts.SafetyDecision
import com.solarrobo.core.contracts.SafetyGate
import com.solarrobo.core.contracts.TrackerFault
import com.solarrobo.core.device.RoboDevice
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class TrackerForegroundService : LifecycleService() {
    @Inject lateinit var camera: CameraXFrameSource
    @Inject lateinit var runtimeFactory: TrackerRuntimeFactory
    @Inject lateinit var runtimeHolder: TrackerRuntimeHolder
    @Inject lateinit var roboDevice: RoboDevice
    @Inject lateinit var safetyGate: SafetyGate
    @Inject lateinit var weatherRuntime: WeatherRuntime
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        when (intent?.action ?: ACTION_START) {
            ACTION_STOP -> stopTracking()
            ACTION_START -> startTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (runtimeHolder.engine.value != null) return
        lifecycleScope.launch {
            try {
                check(ContextCompat.checkSelfPermission(this@TrackerForegroundService, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    "Camera permission is required. Enable it in Android Settings, then retry."
                }
                weatherRuntime.attachTo(camera)
                camera.start(this@TrackerForegroundService)
                val engine = runtimeFactory.create()
                runtimeHolder.attach(engine)
                engine.start()
                if (engine.observeState().first().faults.contains(TrackerFault.LINK_LOST)) {
                    error("SafetyGate or ESP32 rejected tracker startup. Clear the emergency latch and verify the link, then retry.")
                }
                acquireTrackerWakeLock()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                runtimeHolder.reportError(error.localizedMessage ?: "Unable to start tracker")
                runtimeHolder.clear()
                camera.stop()
                releaseTrackerWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun stopTracking() {
        lifecycleScope.launch {
            safetyGate.triggerEmergencyStop()
            runCatching { runtimeHolder.engine.value?.stop() }
            val decision = safetyGate.evaluate(RoboCommand.StopMotion, SafetyContext(LinkState.OFFLINE, false, true, false, false, false))
            if (decision == SafetyDecision.Allow) runCatching { roboDevice.sendCommand(RoboCommand.StopMotion) }
            runtimeHolder.clear()
            camera.stop()
            releaseTrackerWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        releaseTrackerWakeLock()
        camera.stop()
        super.onDestroy()
    }

    private fun acquireTrackerWakeLock() {
        val manager = getSystemService(PowerManager::class.java)
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:TrackerLoop").apply {
            setReferenceCounted(false)
            if (!isHeld) acquire()
        }
    }

    private fun releaseTrackerWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun notification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 10, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 11, Intent(this, TrackerForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_solar_robo)
            .setContentTitle("Solar Robo tracking")
            .setContentText("Camera-based tracker service is active")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Tracker", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Ongoing camera-based tracker operation"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "solar_robo_tracker"
        private const val NOTIFICATION_ID = 4101
        const val ACTION_START = "com.solarrobo.action.START_TRACKING"
        const val ACTION_STOP = "com.solarrobo.action.STOP_TRACKING"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, TrackerForegroundService::class.java).setAction(ACTION_START))
        }
        fun stop(context: Context) {
            context.startService(Intent(context, TrackerForegroundService::class.java).setAction(ACTION_STOP))
        }
    }
}
