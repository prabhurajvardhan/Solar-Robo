package com.solarrobo.app.weather

import android.content.Context
import android.util.Base64
import com.solarrobo.core.weather.ForecastCache
import com.solarrobo.core.weather.ForecastPoint
import com.solarrobo.core.weather.ForecastSnapshot
import com.solarrobo.core.weather.ForecastStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Small local forecast cache. No credentials or camera images are stored. */
@Singleton
class SharedPreferencesForecastCache @Inject constructor(@ApplicationContext context: Context) : ForecastCache {
    private val preferences = context.getSharedPreferences("weather_forecast_cache", Context.MODE_PRIVATE)

    private fun preferenceKey(key: String): String = "forecast_" + Base64.encodeToString(
        key.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    )

    override suspend fun read(key: String): ForecastSnapshot? = withContext(Dispatchers.IO) {
        val raw = preferences.getString(preferenceKey(key), null) ?: return@withContext null
        runCatching {
            val json = JSONObject(raw)
            val values = json.getJSONArray("points")
            val points = (0 until values.length()).map { i ->
                val p = values.getJSONObject(i)
                fun number(name: String) = if (p.isNull(name)) null else p.getDouble(name)
                ForecastPoint(Instant.parse(p.getString("validAt")), number("cloud"), number("precipitation"), number("wind"))
            }
            ForecastSnapshot(
                source = json.getString("source"),
                fetchedAt = Instant.parse(json.getString("fetchedAt")),
                issuedAt = Instant.parse(json.getString("issuedAt")),
                expiresAt = Instant.parse(json.getString("expiresAt")),
                points = points,
                status = ForecastStatus.valueOf(json.getString("status"))
            )
        }.getOrNull()
    }

    override suspend fun write(key: String, value: ForecastSnapshot) = withContext(Dispatchers.IO) {
        val points = JSONArray()
        value.points.forEach { p ->
            points.put(JSONObject().put("validAt", p.validAt.toString())
                .put("cloud", p.cloudCoverPercent ?: JSONObject.NULL)
                .put("precipitation", p.precipitationMm ?: JSONObject.NULL)
                .put("wind", p.windSpeedMps ?: JSONObject.NULL))
        }
        val encoded = JSONObject().put("source", value.source)
            .put("fetchedAt", value.fetchedAt.toString())
            // The current free provider exposes no issue time; this field is an explicit retrieval-time proxy.
            .put("issuedAt", value.issuedAt.toString())
            .put("expiresAt", value.expiresAt.toString())
            .put("status", value.status.name).put("points", points).toString()
        preferences.edit().putString(preferenceKey(key), encoded).apply()
    }
}
