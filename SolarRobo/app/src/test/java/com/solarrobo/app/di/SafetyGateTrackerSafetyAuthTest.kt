package com.solarrobo.app.di

import com.solarrobo.core.contracts.LinkState
import com.solarrobo.core.contracts.SafetyContext
import com.solarrobo.core.contracts.SafetyGate
import com.solarrobo.feature.auth.domain.AuthSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyGateTrackerSafetyAuthTest {
    @Test
    fun viewerCannotAuthorizePhysicalMotionButCanStop() = runBlocking {
        val session = AuthSession()
        val safety = SafetyGateTrackerSafety(SafetyGate(), session)
        val context = SafetyContext(
            link = LinkState.CONNECTED,
            statusFresh = true,
            motorBusy = false,
            calibrated = true,
            faultActive = false,
            windHold = false
        )

        assertFalse(safety.authorize("track", context))
        session.unlockOperator()
        assertTrue(safety.authorize("track", context))
        session.lock()
        assertTrue(safety.authorize("stop", context))
    }
}
