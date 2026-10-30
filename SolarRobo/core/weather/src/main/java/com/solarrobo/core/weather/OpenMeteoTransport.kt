package com.solarrobo.core.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Clock
import java.time.Instant
import java.time.Duration

/** Free, no-key Open-Meteo forecast API adapter. Any failure is handled by CachedForecastProvider. */
class OpenMeteoTransport(
    private val clock: Clock = Clock.systemUTC(),
    private val connectTimeoutMs: Int = 4_000,
    private val readTimeoutMs: Int = 5_000
) : ForecastTransport {
    override suspend fun fetch(request: ForecastRequest): ForecastSnapshot = withContext(Dispatchers.IO) {
        val uri = "https://api.open-meteo.com/v1/forecast?latitude=${request.latitude}&longitude=${request.longitude}" +
            "&hourly=cloud_cover,precipitation,wind_speed_10m&forecast_days=2&wind_speed_unit=ms&timezone=UTC"
        val connection = URL(uri).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("Open-Meteo HTTP $code")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            decode(body, request, clock.instant())
        } finally { connection.disconnect() }
    }

    internal fun decode(body: String, request: ForecastRequest, fetched: Instant): ForecastSnapshot {
        val root = JSONObject(body)
        val hourly = root.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val clouds = hourly.optJSONArray("cloud_cover")
        val rain = hourly.optJSONArray("precipitation")
        val wind = hourly.optJSONArray("wind_speed_10m")
        val end = fetched.plus(request.horizon)
        val points = (0 until times.length()).mapNotNull { i ->
            val at = runCatching { Instant.parse(times.getString(i) + ":00Z") }.getOrNull() ?: return@mapNotNull null
            if (at.isBefore(fetched.minus(Duration.ofHours(1))) || at.isAfter(end)) return@mapNotNull null
            fun value(array: org.json.JSONArray?): Double? = if (array == null || array.isNull(i)) null else array.optDouble(i).takeIf { it.isFinite() && it >= 0.0 }
            ForecastPoint(at, value(clouds)?.coerceAtMost(100.0), value(rain), value(wind))
        }
        // Open-Meteo does not expose a model issuance timestamp in this response. fetchedAt is used
        // explicitly as a retrieval-time proxy; consumers must not mistake it for provider issue time.
        return ForecastSnapshot("Open-Meteo", fetched, fetched, fetched.plus(Duration.ofHours(24)), points, ForecastStatus.FRESH)
    }
}
