package com.solarrobo.core.weather

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WeatherIntelligenceTest {
    private val t0 = Instant.parse("2026-10-25T10:00:00Z")
    @Test fun cameraNowcastIsAnImageEstimateAndNeedsTemporalSamples() {
        val nowcast = ImageTrendNowcast()
        assertEquals(null, nowcast.estimate(emptyList()).trendPerSecond)
        val result = nowcast.estimate(listOf(
            CameraObservation(t0, .2, cloudCentroidX = 10.0, cloudCentroidY = 5.0, frameWidth = 100, frameHeight = 50),
            CameraObservation(t0.plusSeconds(10), .5, cloudCentroidX = 30.0, cloudCentroidY = 15.0, frameWidth = 100, frameHeight = 50)
        ))
        assertEquals(.03, result.trendPerSecond!!, 1e-9)
        assertEquals(2.0, result.dxPixelsPerSecond!!, 1e-9)
        assertTrue(result.isImageEstimate)
    }
    @Test fun climatologyRecordsSourceAndProvisionalCoverageAndLearningRequiresVerifiedLabels() {
        val climate = ProvisionalClimatology().profile(18.1, 83.4, LocalDate.parse("2026-10-25"))
        assertTrue(climate.isProvisional)
        assertNull(climate.source.coverage)
        assertNull(climate.source.reference)
        val learner = VerifiedOutcomeLearning(2)
        assertEquals(false, learner.status().sufficient)
        learner.addVerifiedOutcome(VerifiedOutcome(t0, .4, .3, "human-reviewed"))
        assertFalse(learner.status().sufficient)
        learner.addVerifiedOutcome(VerifiedOutcome(t0.plusSeconds(10), .5, .5, "sensor-verified"))
        assertTrue(learner.status().sufficient)
        assertEquals(2, learner.status().verifiedCount)
    }
    @Test fun failedRefreshReturnsCachedSnapshotMarkedOfflineAndExpiredSnapshotStale() = runBlocking {
        val cache = MemoryForecastCache()
        val request = ForecastRequest(18.1, 83.4)
        val cached = ForecastSnapshot("Open-Meteo", t0.minusSeconds(3600), t0.minusSeconds(3600), t0.minusSeconds(1800), emptyList(), ForecastStatus.FRESH)
        cache.write("18.1,83.4,360", cached)
        val provider = CachedForecastProvider(object : ForecastTransport {
            override suspend fun fetch(request: ForecastRequest): ForecastSnapshot = error("offline")
        }, cache, Clock.fixed(t0, ZoneOffset.UTC))
        val response = provider.forecast(request)
        assertEquals(ForecastStatus.OFFLINE, response.status)
        assertEquals(t0.minusSeconds(3600), response.fetchedAt)
        assertEquals(ForecastStatus.STALE, cached.withStatus(t0).status)
    }
    @Test fun riskRecommendationNeverClaimsToBeActionable() {
        val snapshot = ForecastSnapshot("test", t0, t0, t0.plusSeconds(1000), listOf(ForecastPoint(t0.plusSeconds(60), 20.0, 0.0, 20.0)), ForecastStatus.FRESH)
        val recommendation = ConservativeRiskCombiner(gustRiskMps = 15.0).recommend(
            RiskInputs(snapshot, null, null, LearningStatus(0, 100, false, null)), t0)
        assertEquals(WeatherRisk.HIGH, recommendation.risk)
        assertEquals(AdvisoryAction.CONSIDER_PROTECTIVE_STOW, recommendation.action)
        assertFalse(recommendation.actionable)
    }
}
