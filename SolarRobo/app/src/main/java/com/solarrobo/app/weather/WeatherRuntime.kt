package com.solarrobo.app.weather

import android.content.Context
import com.solarrobo.core.camera.CameraXFrameSource
import com.solarrobo.core.contracts.TrackerEngine
import com.solarrobo.core.tracker.RgbFrame
import com.solarrobo.core.tracker.SunDetector
import com.solarrobo.core.weather.AdvisoryAction
import com.solarrobo.core.weather.CachedForecastProvider
import com.solarrobo.core.weather.ClimateProfile
import com.solarrobo.core.weather.CloudMotionEstimate
import com.solarrobo.core.weather.ConservativeRiskCombiner
import com.solarrobo.core.weather.ForecastRequest
import com.solarrobo.core.weather.ForecastSnapshot
import com.solarrobo.core.weather.ForecastStatus
import com.solarrobo.core.weather.LearningStatus
import com.solarrobo.core.weather.LiteSkyClassifier
import com.solarrobo.core.weather.OpenMeteoTransport
import com.solarrobo.core.weather.ProvisionalClimatology
import com.solarrobo.core.weather.RiskInputs
import com.solarrobo.core.weather.SkyPrediction
import com.solarrobo.core.weather.SkyOpticalFlow
import com.solarrobo.core.weather.OpticalFlowEstimate
import com.solarrobo.core.weather.VerifiedOutcome
import com.solarrobo.core.weather.VerifiedOutcomeLearning
import com.solarrobo.core.weather.WeatherRecommendation
import com.solarrobo.core.weather.WeatherRisk
import com.solarrobo.feature.settings.domain.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.LocalDate
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** All fields are advisory. Missing observations stay absent rather than being fabricated. */
data class WeatherUiState(
    val modelStatus: String = "Loading on-device model",
    val cameraStatus: String = "Waiting for tracking camera frames",
    val prediction: SkyPrediction? = null,
    val brightness: Float? = null,
    val blueFraction: Float? = null,
    val whiteFraction: Float? = null,
    val opticalFlow: OpticalFlowEstimate? = null,
    val obstructionEstimate: String = "Not available",
    val forecast: ForecastSnapshot? = null,
    val forecastStatusText: String = "Forecast not requested",
    val locationText: String = "Set installation coordinates in Settings",
    val climatologyText: String = "Provisional default · source not verified for this location",
    val learning: LearningStatus = LearningStatus(0, 100, false, null),
    val recommendation: WeatherRecommendation = WeatherRecommendation(WeatherRisk.UNKNOWN, AdvisoryAction.REVIEW, listOf("Weather information unavailable"), false),
    val isRefreshing: Boolean = false,
    val error: String? = null
)

/**
 * Runs only on-device image inference and advisory calculations. It never has a RoboDevice,
 * TrackerEngine, motor, command bus, or SafetyGate reference, so a prediction cannot actuate hardware.
 */
@Singleton
class WeatherRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    cache: SharedPreferencesForecastCache
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cameraFrames = Channel<RgbFrame>(Channel.CONFLATED)
    private val provider = CachedForecastProvider(OpenMeteoTransport(), cache)
    private val riskCombiner = ConservativeRiskCombiner(maxForecastAge = Duration.ofHours(12))
    private val learning = VerifiedOutcomeLearning(minimumVerifiedOutcomes = 100)
    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()
    private val classifier: LiteSkyClassifier?
    private val modelLoadFailure: String?
    private val observations = mutableListOf<JSONObject>()
    private val observationPrefs = context.getSharedPreferences("weather_camera_observations", Context.MODE_PRIVATE)
    @Volatile private var latitude: Double? = null
    @Volatile private var longitude: Double? = null
    @Volatile private var currentForecast: ForecastSnapshot? = null
    @Volatile private var latestPrediction: SkyPrediction? = null
    @Volatile private var latestBrightness: Float? = null
    @Volatile private var previousFrame: RgbFrame? = null
    @Volatile private var previousFrameAtMillis: Long? = null
    @Volatile private var latestNowcast: CloudMotionEstimate? = null
    @Volatile private var currentClimate: ClimateProfile? = null

    init {
        val loaded = runCatching {
            val meta = JSONObject(context.assets.open("sky_conditions_labels.json").bufferedReader().use { it.readText() })
            val expected = meta.getJSONArray("labels")
            require(expected.length() == 7 && (0 until 7).map { expected.getString(it) } == com.solarrobo.core.weather.SkyClass.labels) {
                "Model label file does not match app class order"
            }
            val bytes = context.assets.open("sky_conditions.tflite").use { it.readBytes() }
            val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
            buffer.put(bytes); buffer.rewind()
            LiteSkyClassifier(buffer, meta.optInt("input_size", 160), meta.optDouble("minimum_confidence", .55).toFloat())
        }
        classifier = loaded.getOrNull()
        modelLoadFailure = loaded.exceptionOrNull()?.message ?: if (classifier == null) "Model asset unavailable" else null
        _state.value = _state.value.copy(
            modelStatus = if (classifier == null) "Model unavailable" else "On-device CNN ready · offline inference",
            error = modelLoadFailure
        )
        observations.addAll(readCameraHistory())
        scope.launch(Dispatchers.Default) {
            for (frame in cameraFrames) processFrame(frame)
        }
    }

    /** Installs an optional local observer on the exact CameraX source used by the tracker. */
    fun attachTo(camera: CameraXFrameSource) {
        camera.onFrameObserved = { frame -> cameraFrames.trySend(frame) }
    }

    private fun processFrame(frame: RgbFrame) {
        val stats = runCatching { SunDetector.skyStats(frame) }.getOrNull()
        val nowMillis = System.currentTimeMillis()
        val prior = previousFrame
        val priorAt = previousFrameAtMillis
        val flow = if (prior != null && priorAt != null && nowMillis > priorAt && nowMillis - priorAt <= 180_000) {
            runCatching { SkyOpticalFlow.estimate(prior.pixels, frame.pixels, frame.width, frame.height, (nowMillis - priorAt) / 1000.0) }.getOrNull()
        } else null
        previousFrame = frame.copy(pixels = frame.pixels.copyOf())
        previousFrameAtMillis = nowMillis
        latestNowcast = flow?.let { CloudMotionEstimate(it.dxPixelsPerSecond, it.dyPixelsPerSecond, null, it.matchedPatches) }
        val prediction = runCatching {
            classifier?.classify(frame.pixels, frame.width, frame.height, nowMillis)
        }.getOrNull()
        latestPrediction = prediction
        latestBrightness = stats?.brightness
        val next = _state.value.copy(
            cameraStatus = if (prediction == null) "Camera frame received; classification unavailable" else "Local camera inference complete",
            prediction = prediction,
            brightness = stats?.brightness,
            blueFraction = stats?.blueFraction,
            whiteFraction = stats?.whiteFraction,
            opticalFlow = flow,
            obstructionEstimate = obstructionEstimate(prediction),
            error = if (prediction == null) modelLoadFailure ?: "Inference failed" else null
        )
        _state.value = next.copy(recommendation = recommendation(currentForecast, prediction))
        persistObservation(frame, prediction, stats, flow)
    }

    private fun obstructionEstimate(prediction: SkyPrediction?): String {
        if (prediction == null || !prediction.accepted) return "Unknown · model low confidence"
        return when (prediction.label) {
            "clear" -> "Likely low · class-based heuristic"
            "fair_weather_cumulus" -> "Likely low to mixed · class-based heuristic"
            "overcast_or_stratus" -> "Likely high · class-based heuristic"
            "towering_cumulus" -> "Likely mixed to high · class-based heuristic"
            "cumulonimbus_or_storm_like" -> "Likely high · class-based heuristic"
            "fog_or_obscured_sky" -> "Likely very high · class-based heuristic"
            "rain_or_precipitation" -> "Likely high · class-based heuristic"
            else -> "Unknown · unmapped class"
        }
    }

    private fun persistObservation(frame: RgbFrame, prediction: SkyPrediction?, stats: com.solarrobo.core.tracker.SkyStats?, flow: OpticalFlowEstimate?) {
        val row = JSONObject()
            .put("observedAt", Instant.now().toString())
            .put("skyClass", prediction?.label ?: JSONObject.NULL)
            .put("confidence", prediction?.confidence ?: JSONObject.NULL)
            .put("brightnessMeanRgb", stats?.brightness ?: JSONObject.NULL)
            .put("blueFraction", stats?.blueFraction ?: JSONObject.NULL)
            .put("whiteFraction", stats?.whiteFraction ?: JSONObject.NULL)
            .put("frameWidth", frame.width).put("frameHeight", frame.height)
            // These are null until the tracker exposes explicit values; never infer them from sky pixels.
            .put("cloudMotionDxPixelsPerSecond", flow?.dxPixelsPerSecond ?: JSONObject.NULL)
            .put("cloudMotionDyPixelsPerSecond", flow?.dyPixelsPerSecond ?: JSONObject.NULL)
            .put("cloudMotionConfidence", flow?.confidence ?: JSONObject.NULL)
            .put("trackingErrorDeg", JSONObject.NULL)
            .put("imageJitter", JSONObject.NULL)
            .put("action", JSONObject.NULL)
            .put("verifiedOutcome", JSONObject.NULL)
        synchronized(observations) {
            observations.add(row)
            while (observations.size > 500) observations.removeAt(0)
            observationPrefs.edit().putString("rows", JSONArray(observations).toString()).apply()
        }
    }

    private fun readCameraHistory(): List<JSONObject> = runCatching {
        val raw = observationPrefs.getString("rows", null) ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).map { array.getJSONObject(it) }
    }.getOrDefault(emptyList())

    fun refreshForecast() {
        scope.launch {
            _state.value = _state.value.copy(isRefreshing = true)
            try {
                val saved = settingsRepository.load().first()
                val lat = saved.latitudeDeg
                val lon = saved.longitudeDeg
                if (lat == null || lon == null) {
                    _state.value = _state.value.copy(isRefreshing = false, locationText = "Set installation coordinates in Settings", forecastStatusText = "No location configured")
                    return@launch
                }
                latitude = lat; longitude = lon
                val snapshot = provider.forecast(ForecastRequest(lat, lon, Duration.ofHours(24)))
                currentForecast = snapshot
                val climate = ProvisionalClimatology().profile(lat, lon, LocalDate.now())
                currentClimate = climate
                val now = Instant.now()
                val rec = recommendation(snapshot, latestPrediction, climate, now)
                val ageHours = Duration.between(snapshot.fetchedAt, now).toMinutes().coerceAtLeast(0) / 60.0
                val status = when {
                    snapshot.status == ForecastStatus.UNAVAILABLE -> "Unavailable · network/cache has no data"
                    ageHours >= 24 -> "Stale · retrieved %.1f h ago%s".format(ageHours, if (snapshot.status == ForecastStatus.OFFLINE) " · offline cache" else "")
                    ageHours > 12 -> "Aged · retrieved %.1f h ago (preferred ≤12 h)".format(ageHours)
                    snapshot.status == ForecastStatus.OFFLINE -> "Offline cached · retrieved %.1f h ago".format(ageHours)
                    else -> "Retrieved %.1f h ago · valid up to 24 h".format(ageHours)
                }
                _state.value = _state.value.copy(
                    isRefreshing = false, forecast = snapshot, forecastStatusText = status,
                    locationText = "%.4f°, %.4f°".format(lat, lon),
                    climatologyText = "Provisional default · no verified local table · profile for %.4f°, %.4f°".format(lat, lon),
                    recommendation = rec, learning = learning.status(), error = if (snapshot.status == ForecastStatus.UNAVAILABLE) "Forecast unavailable; local tracking is not blocked" else null
                )
            } catch (error: Exception) {
                _state.value = _state.value.copy(isRefreshing = false, forecastStatusText = "Unavailable · refresh failed", error = error.message ?: "Weather refresh failed")
            }
        }
    }

    /** Re-evaluates the cached snapshot age without issuing a network request. */
    fun updateFreshness() {
        val snapshot = currentForecast ?: return
        val now = Instant.now()
        val ageHours = Duration.between(snapshot.fetchedAt, now).toMinutes().coerceAtLeast(0) / 60.0
        val offlineSuffix = if (snapshot.status == ForecastStatus.OFFLINE) " · offline cache" else ""
        val status = when {
            snapshot.status == ForecastStatus.UNAVAILABLE -> "Unavailable · network/cache has no data"
            ageHours >= 24 -> "Stale · retrieved %.1f h ago%s".format(ageHours, offlineSuffix)
            ageHours > 12 -> "Aged · retrieved %.1f h ago (preferred ≤12 h)%s".format(ageHours, offlineSuffix)
            snapshot.status == ForecastStatus.OFFLINE -> "Offline cached · retrieved %.1f h ago".format(ageHours)
            else -> "Retrieved %.1f h ago · valid up to 24 h".format(ageHours)
        }
        _state.value = _state.value.copy(
            forecastStatusText = status,
            recommendation = recommendation(snapshot, latestPrediction, currentClimate, now)
        )
    }

    private fun recommendation(
        forecast: ForecastSnapshot?, prediction: SkyPrediction?, climate: ClimateProfile? = currentClimate,
        now: Instant = Instant.now()
    ) = riskCombiner.recommend(
        RiskInputs(forecast = forecast, nowcast = latestNowcast, climatology = climate,
            learning = learning.status(), skyClass = prediction?.label, skyConfidence = prediction?.confidence?.toDouble()), now
    )

    /** Called only by an explicitly verified future weather-record workflow; camera observations never call this. */
    fun addVerifiedOutcome(outcome: VerifiedOutcome) {
        learning.addVerifiedOutcome(outcome)
        _state.value = _state.value.copy(learning = learning.status(), recommendation = recommendation(currentForecast, latestPrediction))
    }
}
