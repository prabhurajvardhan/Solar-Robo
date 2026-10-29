package com.solarrobo.core.weather

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

/** Contract deliberately reports image descriptors only; it cannot claim calibrated meteorology. */
data class CameraObservation(
    val observedAt: Instant,
    val cloudFractionEstimate: Double,
    val sunVisible: Boolean? = null,
    val cloudCentroidX: Double? = null,
    val cloudCentroidY: Double? = null,
    val frameWidth: Int? = null,
    val frameHeight: Int? = null
) {
    init {
        require(cloudFractionEstimate in 0.0..1.0)
        require((cloudCentroidX == null) == (cloudCentroidY == null))
        if (cloudCentroidX != null) require(frameWidth != null && frameHeight != null && frameWidth > 0 && frameHeight > 0)
    }
}
data class CloudMotionEstimate(val dxPixelsPerSecond: Double?, val dyPixelsPerSecond: Double?, val trendPerSecond: Double?, val sampleCount: Int, val isImageEstimate: Boolean = true)
interface CameraNowcast { fun estimate(observations: List<CameraObservation>): CloudMotionEstimate }
class ImageTrendNowcast(private val maximumGap: Duration = Duration.ofMinutes(3)) : CameraNowcast {
    override fun estimate(observations: List<CameraObservation>): CloudMotionEstimate {
        val samples = observations.sortedBy { it.observedAt }.takeLast(12)
        if (samples.size < 2) return CloudMotionEstimate(null, null, null, samples.size)
        val first = samples.first(); val last = samples.last()
        val seconds = Duration.between(first.observedAt, last.observedAt).toMillis() / 1000.0
        if (seconds <= 0 || Duration.between(first.observedAt, last.observedAt) > maximumGap) return CloudMotionEstimate(null, null, null, samples.size)
        val dx = if (first.cloudCentroidX != null && last.cloudCentroidX != null && first.frameWidth == last.frameWidth) (last.cloudCentroidX - first.cloudCentroidX) / seconds else null
        val dy = if (first.cloudCentroidY != null && last.cloudCentroidY != null && first.frameHeight == last.frameHeight) (last.cloudCentroidY - first.cloudCentroidY) / seconds else null
        return CloudMotionEstimate(dx, dy, (last.cloudFractionEstimate - first.cloudFractionEstimate) / seconds, samples.size)
    }
}

/** A model can be supplied later. This API makes no assertion that a CNN is trained or present. */
interface CameraInference { fun infer(observation: CameraObservation): CameraObservation }

data class GeoCoverage(val southLat: Double, val westLon: Double, val northLat: Double, val eastLon: Double) {
    init { require(southLat in -90.0..90.0 && northLat in southLat..90.0 && westLon in -180.0..180.0 && eastLon in westLon..180.0) }
    fun contains(latitude: Double, longitude: Double) = latitude in southLat..northLat && longitude in westLon..eastLon
}
data class ClimatologySource(val name: String, val reference: String?, val retrievedOn: LocalDate?, val coverage: GeoCoverage?, val note: String)
data class ClimateProfile(
    val source: ClimatologySource,
    val cloudFractionBaseline: Double,
    val windSpeedMpsBaseline: Double?,
    val isProvisional: Boolean,
    val siteLatitude: Double,
    val siteLongitude: Double
) { init { require(cloudFractionBaseline in 0.0..1.0); require(siteLatitude in -90.0..90.0 && siteLongitude in -180.0..180.0) } }
interface LocalClimatology { fun profile(latitude: Double, longitude: Double, date: LocalDate): ClimateProfile }
/** Intentionally conservative fallback. It is not a locally measured climatology. */
class ProvisionalClimatology(private val configuredCloudBaseline: Double = 0.5, private val configuredWindBaselineMps: Double? = null) : LocalClimatology {
    init { require(configuredCloudBaseline in 0.0..1.0); require(configuredWindBaselineMps == null || configuredWindBaselineMps >= 0.0) }
    override fun profile(latitude: Double, longitude: Double, date: LocalDate) = ClimateProfile(
        ClimatologySource("configured provisional default", null, date, null, "No verified local climate source configured"),
        configuredCloudBaseline, configuredWindBaselineMps, true, latitude, longitude)
}

data class VerifiedOutcome(val occurredAt: Instant, val observedCloudFraction: Double, val forecastCloudFraction: Double, val verifiedBy: String) {
    init { require(observedCloudFraction in 0.0..1.0 && forecastCloudFraction in 0.0..1.0 && verifiedBy.isNotBlank()) }
}
data class LearningStatus(val verifiedCount: Int, val requiredMinimum: Int, val sufficient: Boolean, val meanAbsoluteError: Double?)
interface WeatherLearning { fun addVerifiedOutcome(outcome: VerifiedOutcome); fun status(): LearningStatus }
/** Unlabeled logs have no API path into this accumulator and can never increment its count. */
class VerifiedOutcomeLearning(private val minimumVerifiedOutcomes: Int = 100) : WeatherLearning {
    init { require(minimumVerifiedOutcomes > 0) }
    private val errors = mutableListOf<Double>()
    @Synchronized override fun addVerifiedOutcome(outcome: VerifiedOutcome) { errors += abs(outcome.observedCloudFraction - outcome.forecastCloudFraction) }
    @Synchronized override fun status(): LearningStatus = LearningStatus(errors.size, minimumVerifiedOutcomes, errors.size >= minimumVerifiedOutcomes, errors.takeIf { it.isNotEmpty() }?.average())
}

enum class ForecastStatus { FRESH, STALE, OFFLINE, UNAVAILABLE }
data class ForecastPoint(val validAt: Instant, val cloudCoverPercent: Double?, val precipitationMm: Double?, val windSpeedMps: Double?)
data class ForecastSnapshot(
    val source: String,
    val fetchedAt: Instant,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val points: List<ForecastPoint>,
    val status: ForecastStatus
) {
    fun withStatus(now: Instant, offline: Boolean = false): ForecastSnapshot = copy(status = when {
        offline -> ForecastStatus.OFFLINE
        now.isBefore(expiresAt) -> ForecastStatus.FRESH
        else -> ForecastStatus.STALE
    })
}
data class ForecastRequest(val latitude: Double, val longitude: Double, val horizon: Duration = Duration.ofHours(6)) {
    init { require(latitude in -90.0..90.0 && longitude in -180.0..180.0 && !horizon.isNegative && !horizon.isZero) }
}
interface ForecastProvider { suspend fun forecast(request: ForecastRequest): ForecastSnapshot }
interface ForecastTransport { suspend fun fetch(request: ForecastRequest): ForecastSnapshot }
interface ForecastCache { suspend fun read(key: String): ForecastSnapshot?; suspend fun write(key: String, value: ForecastSnapshot) }
class MemoryForecastCache : ForecastCache {
    private val entries = mutableMapOf<String, ForecastSnapshot>()
    private val lock = Any()
    override suspend fun read(key: String) = synchronized(lock) { entries[key] }
    override suspend fun write(key: String, value: ForecastSnapshot) { synchronized(lock) { entries[key] = value } }
}
/** Failure-tolerant wrapper: network errors return cached data (marked stale/offline), never throw. */
class CachedForecastProvider(private val transport: ForecastTransport, private val cache: ForecastCache, private val clock: Clock = Clock.systemUTC()) : ForecastProvider {
    override suspend fun forecast(request: ForecastRequest): ForecastSnapshot {
        val key = "${request.latitude},${request.longitude},${request.horizon.toMinutes()}"
        return try {
            val received = transport.fetch(request)
            val checked = received.withStatus(clock.instant())
            cache.write(key, checked)
            checked
        } catch (_: Exception) {
            cache.read(key)?.withStatus(clock.instant(), offline = true)
                ?: ForecastSnapshot("Open-Meteo", clock.instant(), clock.instant(), clock.instant(), emptyList(), ForecastStatus.UNAVAILABLE)
        }
    }
}

enum class WeatherRisk { UNKNOWN, LOW, ELEVATED, HIGH }
enum class AdvisoryAction { CONTINUE_TRACKING, REVIEW, CONSIDER_PROTECTIVE_STOW }
data class WeatherRecommendation(val risk: WeatherRisk, val action: AdvisoryAction, val reasons: List<String>, val actionable: Boolean = false)
data class RiskInputs(
    val forecast: ForecastSnapshot?,
    val nowcast: CloudMotionEstimate?,
    val climatology: ClimateProfile?,
    val learning: LearningStatus,
    val skyClass: String? = null,
    val skyConfidence: Double? = null
)
interface WeatherRiskCombiner { fun recommend(inputs: RiskInputs, now: Instant): WeatherRecommendation }
/** Advisory only. Even a HIGH result cannot call a motor, engine, command bus, or SafetyGate. */
class ConservativeRiskCombiner(private val maxForecastAge: Duration = Duration.ofHours(12), private val gustRiskMps: Double = 15.0) : WeatherRiskCombiner {
    override fun recommend(inputs: RiskInputs, now: Instant): WeatherRecommendation {
        val reasons = mutableListOf<String>(); var high = false; var elevated = false
        val forecast = inputs.forecast
        if (forecast == null || forecast.status == ForecastStatus.UNAVAILABLE || forecast.status == ForecastStatus.OFFLINE || Duration.between(forecast.fetchedAt, now) > maxForecastAge) {
            reasons += "Forecast missing, offline, or too old; no weather automation"
        } else {
            if (forecast.points.any { (it.windSpeedMps ?: 0.0) >= gustRiskMps }) { high = true; reasons += "Forecast wind reaches configured advisory threshold" }
            if (forecast.points.any { (it.precipitationMm ?: 0.0) > 0.0 }) { elevated = true; reasons += "Precipitation forecast" }
        }
        if (inputs.nowcast?.trendPerSecond?.let { it > 0.001 } == true) { elevated = true; reasons += "Image-estimated cloud cover increasing" }
        if ((inputs.skyConfidence ?: 0.0) >= 0.65) {
            when (inputs.skyClass) {
                SkyClass.CUMULONIMBUS.label -> { high = true; reasons += "CNN visual storm-like class (advisory only)" }
                SkyClass.TOWERING_CUMULUS.label, SkyClass.FOG.label, SkyClass.PRECIPITATION.label, SkyClass.STRATUS.label -> {
                    elevated = true; reasons += "CNN visual weather class (advisory only)"
                }
            }
        }
        if (inputs.climatology?.isProvisional == true) reasons += "Climatology is provisional and does not establish a local baseline"
        if (!inputs.learning.sufficient) reasons += "Local learning insufficient (${inputs.learning.verifiedCount}/${inputs.learning.requiredMinimum} verified outcomes)"
        val forecastUsable = forecast != null && forecast.status == ForecastStatus.FRESH &&
            Duration.between(forecast.fetchedAt, now) <= maxForecastAge
        val risk = when { high -> WeatherRisk.HIGH; elevated -> WeatherRisk.ELEVATED; !forecastUsable -> WeatherRisk.UNKNOWN; else -> WeatherRisk.LOW }
        val action = when (risk) { WeatherRisk.HIGH -> AdvisoryAction.CONSIDER_PROTECTIVE_STOW; WeatherRisk.ELEVATED, WeatherRisk.UNKNOWN -> AdvisoryAction.REVIEW; WeatherRisk.LOW -> AdvisoryAction.CONTINUE_TRACKING }
        return WeatherRecommendation(risk, action, reasons, actionable = false)
    }
}

/** Clock helper for transports whose issued time is unavailable: request creation time, not model issue metadata. */
fun provisionalForecast(source: String, now: Instant = Instant.now(), ttl: Duration = Duration.ofMinutes(30), points: List<ForecastPoint> = emptyList()) =
    ForecastSnapshot(source, now, now, now.plus(ttl), points, ForecastStatus.FRESH)
