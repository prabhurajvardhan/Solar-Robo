package com.solarrobo.core.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import com.solarrobo.core.contracts.CommandResult
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.tracker.Motor
import com.solarrobo.core.tracker.MotorStatus
import com.solarrobo.core.tracker.MotorLimitReachedException
import com.solarrobo.core.tracker.MoveDirection
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.roundToInt

/** Exact status returned by the supplied firmware. */
data class FirmwareStatus(
    val netMs: Long,
    val limitMs: Long,
    val running: Boolean,
    val ranMs: Long,
    val clamped: Boolean
)

/** Injectable protocol boundary for tests; a move result is an acknowledgement, not completion. */
interface Esp32Transport {
    suspend fun status(): FirmwareStatus
    suspend fun move(direction: Int, durationMs: Int): FirmwareStatus
    suspend fun stop(): FirmwareStatus
    suspend fun home(): FirmwareStatus
    suspend fun sync(signedMs: Long): FirmwareStatus
    suspend fun setLimit(limitMs: Long): FirmwareStatus
}

sealed class DeviceFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Http(val status: Int, detail: String) : DeviceFailure("HTTP $status: $detail")
    class Busy : DeviceFailure("Firmware reports the motor is busy")
    class Clamped : DeviceFailure("Firmware clamped the requested move to its travel limit")
    class Timeout(cause: Throwable? = null) : DeviceFailure("Device request timed out; outcome may be unknown", cause)
    class Malformed(cause: Throwable? = null) : DeviceFailure("Malformed firmware response", cause)
    class Disconnected : DeviceFailure("ESP32 Wi-Fi network is not connected")
    class Transport(cause: Throwable) : DeviceFailure("ESP32 transport failed", cause)
    class Uncalibrated : DeviceFailure("Absolute movement requires the calibrated TrackerController")
}

/** Android NetworkSpecifier connection and fixed-host firmware client. */
class Esp32WifiDevice(
    context: Context,
    private val transportFactory: (Network) -> Esp32Transport = { network -> SocketEsp32Transport(network) },
    private val pollIntervalMs: Long = 150L,
    private val moveTimeoutMs: Long = 12_000L
) : RoboDevice {
    private val connectivity = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkRef = AtomicReference<Network?>(null)
    @Volatile private var callback: ConnectivityManager.NetworkCallback? = null
    @Volatile private var transport: Esp32Transport? = null
    @Volatile private var outcomeUnknown = false
    private val snapshots = MutableStateFlow(disconnectedSnapshot())

    fun createMotorAdapter(): Esp32MotorAdapter = Esp32MotorAdapter({ requireTransport() }, { 3.0 })

    override suspend fun connect(): Result<Unit> = try {
        if (Build.VERSION.SDK_INT < 29) error("Wi-Fi NetworkSpecifier requires Android 10 or newer")
        if (networkRef.get() == null) requestWifiNetwork()
        requireTransport().status() // Verify the HTTP API after the local network is available.
        snapshots.value = snapshots.value.copy(connected = true, timestamp = System.currentTimeMillis())
        Result.success(Unit)
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        Result.failure(t)
    }

    private suspend fun requestWifiNetwork() = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(SSID)
                .setWpa2Passphrase(AP_PASSWORD)
                .build()
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build()
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    networkRef.set(network)
                    transport = transportFactory(network)
                    snapshots.value = snapshots.value.copy(connected = true, timestamp = System.currentTimeMillis())
                    if (continuation.isActive) continuation.resume(Unit)
                }
                override fun onLost(network: Network) {
                    if (networkRef.compareAndSet(network, null)) {
                        transport = null
                        outcomeUnknown = true
                        snapshots.value = snapshots.value.copy(connected = false, timestamp = System.currentTimeMillis())
                    }
                }
                override fun onUnavailable() {
                    networkRef.set(null)
                    transport = null
                    if (continuation.isActive) continuation.resumeWithException(DeviceFailure.Disconnected())
                }
            }
            callback = cb
            try {
                connectivity.requestNetwork(request, cb, NETWORK_REQUEST_TIMEOUT_MS)
            } catch (t: Throwable) {
                callback = null
                if (continuation.isActive) continuation.resumeWithException(t)
            }
            continuation.invokeOnCancellation {
                if (callback === cb) callback = null
                runCatching { connectivity.unregisterNetworkCallback(cb) }
            }
        }
    }

    override suspend fun disconnect() {
        callback?.let { runCatching { connectivity.unregisterNetworkCallback(it) } }
        callback = null
        networkRef.set(null)
        transport = null
        outcomeUnknown = false
        snapshots.value = disconnectedSnapshot()
    }

    override suspend fun getSnapshot(): RoboSnapshot {
        return try {
            val status = requireTransport().status()
            if (!status.running) outcomeUnknown = false
            snapshots.value = snapshots.value.copy(
                connected = true,
                mode = if (status.running) RoboMode.NORMAL else snapshots.value.mode,
                generationWatts = null,
                batteryPercent = null,
                timestamp = System.currentTimeMillis()
            )
            snapshots.value
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            snapshots.value = snapshots.value.copy(connected = networkRef.get() != null, timestamp = System.currentTimeMillis())
            snapshots.value
        }
    }

    /** Absolute-angle actuation is deliberately refused here; only calibrated TrackerController may move. */
    override suspend fun sendCommand(command: RoboCommand): CommandResult {
        val id = UUID.randomUUID().toString()
        return try {
            val device = requireTransport()
            if (outcomeUnknown) {
                val state = device.status()
                if (state.running) throw DeviceFailure.Busy()
                outcomeUnknown = false
            }
            when (command) {
                is RoboCommand.MoveToAngle -> throw DeviceFailure.Uncalibrated()
                RoboCommand.StopMotion -> {
                    device.stop()
                    outcomeUnknown = false
                    CommandResult(true, id, "Stop sent")
                }
                RoboCommand.Home -> {
                    device.home()
                    outcomeUnknown = false
                    CommandResult(true, id, "Flat reference declared by user; firmware does not move the panel")
                }
                RoboCommand.SafePosition -> CommandResult(
                    false, id,
                    "Firmware /home only declares the current position as flat; it does not move the panel. Use the calibrated tracker to stow."
                )
            }
        } catch (e: CancellationException) {
            outcomeUnknown = true
            throw e
        } catch (e: Exception) {
            if (e is DeviceFailure.Timeout) outcomeUnknown = true
            CommandResult(false, id, e.message ?: "ESP32 command failed")
        }
    }

    override fun observeSnapshot(): Flow<RoboSnapshot> = snapshots.asStateFlow()

    private fun requireTransport(): Esp32Transport = transport ?: throw DeviceFailure.Disconnected()

    private fun disconnectedSnapshot() = RoboSnapshot(
        deviceId = "ESP32-SOLAR-TRACKER", name = "SolarTracker ESP32", connected = false,
        mode = RoboMode.SAFE, panelAngleDeg = 0f, targetAngleDeg = 0f,
        generationWatts = null, batteryPercent = null, timestamp = System.currentTimeMillis()
    )

    companion object {
        const val SSID = "SolarTracker"
        const val AP_PASSWORD = "sun12345"
        const val BASE_URL = "http://192.168.4.1"
        const val MAX_REQUEST_MS = 6_000
        const val NETWORK_REQUEST_TIMEOUT_MS = 30_000
    }
}

/** Maps calibrated tracker operations to the firmware's electrical forward/reverse millisecond API. */
class Esp32MotorAdapter(
    private val transportProvider: () -> Esp32Transport,
    private val calibratedDegreesPerSecond: () -> Double,
    private val pollIntervalMs: Long = 100L,
    private val moveTimeoutMs: Long = 12_000L
) : Motor {
    @Volatile private var controllerSpeed: Double? = null

    override fun updateCalibrationSpeed(degreesPerSecond: Double) {
        require(abs(degreesPerSecond) >= 0.2)
        controllerSpeed = degreesPerSecond
    }

    private fun currentSpeed(): Double = controllerSpeed ?: calibratedDegreesPerSecond()

    override suspend fun move(direction: MoveDirection, milliseconds: Long): Long {
        require(milliseconds in 1..Esp32WifiDevice.MAX_REQUEST_MS.toLong())
        val transport = transportProvider()
        val before = transport.status()
        if (before.running) throw DeviceFailure.Busy()
        val accepted = transport.move(direction.sign, milliseconds.toInt()) // Never retry this call.
        if (!accepted.running && accepted.ranMs == 0L) {
            if (accepted.clamped) throw MotorLimitReachedException()
            return 0L
        }
        val began = System.currentTimeMillis()
        var after = accepted
        while (after.running) {
            if (System.currentTimeMillis() - began > moveTimeoutMs) {
                // The outcome remains unknown; caller must query status before any subsequent move.
                throw DeviceFailure.Timeout()
            }
            delay(pollIntervalMs)
            after = transport.status()
        }
        return abs(after.netMs - before.netMs)
    }

    override suspend fun status(): MotorStatus = try {
        val status = transportProvider().status()
        MotorStatus(connected = true, thetaDeg = null, busy = status.running)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        MotorStatus(connected = false, thetaDeg = null, busy = false)
    }

    override suspend fun stop() { transportProvider().stop() }

    /** This endpoint is a user-confirmed declaration, not physical homing. */
    override suspend fun home(): Boolean {
        transportProvider().home()
        return true
    }

    override suspend fun sync(thetaDeg: Double) {
        val tps = currentSpeed()
        require(abs(tps) >= 0.2)
        transportProvider().sync((thetaDeg / tps * 1_000.0).roundToInt().toLong())
    }

    override suspend fun setLimit(minThetaDeg: Double, maxThetaDeg: Double) {
        val tps = currentSpeed()
        require(abs(tps) >= 0.2 && minThetaDeg < maxThetaDeg)
        transportProvider().setLimit((maxOf(abs(minThetaDeg), abs(maxThetaDeg)) / abs(tps) * 1_000.0).roundToInt().toLong())
    }
}

/** Raw HTTP socket bound to the selected Wi-Fi Network; only the fixed firmware host is used. */
class SocketEsp32Transport(
    private val network: Network,
    private val host: String = "192.168.4.1",
    private val port: Int = 80,
    private val connectTimeoutMs: Int = 3_000,
    private val readTimeoutMs: Int = 5_000
) : Esp32Transport {
    override suspend fun status() = parseStatus(request("/status"))

    override suspend fun move(direction: Int, durationMs: Int): FirmwareStatus {
        require(direction == 1 || direction == -1)
        val ms = durationMs.coerceIn(1, Esp32WifiDevice.MAX_REQUEST_MS)
        return parseStatus(request("/move?dir=$direction&ms=$ms"))
    }

    override suspend fun stop() = parseStatus(request("/stop"))
    override suspend fun home() = parseStatus(request("/home"))
    override suspend fun sync(signedMs: Long) = parseStatus(request("/sync?ms=$signedMs"))
    override suspend fun setLimit(limitMs: Long) = parseStatus(request("/setlimit?ms=${limitMs.coerceAtLeast(1L)}"))

    private suspend fun request(path: String): JSONObject = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = network.socketFactory.createSocket()
            socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
            socket.soTimeout = readTimeoutMs
            val out = OutputStreamWriter(socket.getOutputStream(), Charsets.US_ASCII)
            out.write("GET $path HTTP/1.1\r\nHost: $host\r\nConnection: close\r\nAccept: application/json\r\n\r\n")
            out.flush()
            val input = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val statusLine = input.readLine() ?: throw DeviceFailure.Malformed()
            val code = statusLine.split(' ').getOrNull(1)?.toIntOrNull() ?: throw DeviceFailure.Malformed()
            var contentLength: Int? = null
            while (true) {
                val line = input.readLine() ?: throw DeviceFailure.Malformed()
                if (line.isEmpty()) break
                if (line.startsWith("Content-Length:", true)) contentLength = line.substringAfter(':').trim().toIntOrNull()
            }
            val body = buildString {
                if (contentLength != null) repeat(contentLength!!) {
                    val char = input.read()
                    if (char < 0) throw DeviceFailure.Malformed()
                    append(char.toChar())
                } else while (true) {
                    val char = input.read()
                    if (char < 0) break
                    append(char.toChar())
                }
            }
            if (code == 409) throw DeviceFailure.Busy()
            if (code !in 200..299) throw DeviceFailure.Http(code, body.take(200))
            try { JSONObject(body) } catch (e: JSONException) { throw DeviceFailure.Malformed(e) }
        } catch (e: DeviceFailure) { throw e }
        catch (e: java.net.SocketTimeoutException) { throw DeviceFailure.Timeout(e) }
        catch (e: java.io.IOException) { throw DeviceFailure.Timeout(e) }
        catch (e: Exception) { throw DeviceFailure.Transport(e) }
        finally { runCatching { socket?.close() } }
    }

    private fun parseStatus(json: JSONObject): FirmwareStatus = try {
        FirmwareStatus(
            netMs = json.getLong("net_ms"),
            limitMs = json.getLong("limit_ms"),
            running = json.getBoolean("running"),
            ranMs = json.getLong("ran_ms"),
            clamped = json.getBoolean("clamped")
        )
    } catch (e: JSONException) { throw DeviceFailure.Malformed(e) }
}
