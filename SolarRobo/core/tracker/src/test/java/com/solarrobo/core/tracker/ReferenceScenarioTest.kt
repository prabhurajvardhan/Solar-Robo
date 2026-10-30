package com.solarrobo.core.tracker

import com.solarrobo.core.contracts.TrackerFault
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.toList
import org.junit.Assert.*
import org.junit.Test

/** Port of sim_test.py's world: fixed seed-free physics, injected wall clock and no real sleeps. */
class ReferenceScenarioTest {
    private val start = Instant.parse("2026-10-25T01:30:00Z")
    private val lat = 18.1067
    private val lon = 83.3956
    private val hfov = 70.0
    private val width = 320

    private class FastClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
        override fun millis(): Long = now.toEpochMilli()
        fun advance(d: Duration) { now = now.plus(d) }
    }

    private inner class World(val clock: FastClock) {
        var theta = 0.0
        val trueSpeed = -2.4
        val imageSign = -1
        var netMs = 0L
        var limitMs = Long.MAX_VALUE
        var jam = false
        var clouds = false
        var wind = false
        var stopped = 0
        lateinit var store: Store
        fun target(): Double {
            val p = SolarMath.position(18.1067, 83.3956, clock.instant())
            return SolarMath.trackerAngle(p.azimuthDeg, p.elevationDeg)
        }
        fun frame(): RgbFrame {
            val pixels = ByteArray(width * 240 * 3)
            for (i in 0 until width * 240) { pixels[i*3]=90.toByte(); pixels[i*3+1]=140.toByte(); pixels[i*3+2]=220.toByte() }
            if (clouds) {
                for (i in 0 until width * 240) { pixels[i*3]=200.toByte(); pixels[i*3+1]=200.toByte(); pixels[i*3+2]=205.toByte() }
            } else {
                val x = (width/2.0 + imageSign * (target()-theta) * width/hfov).roundToInt()
                if (x in 3 until width-3) for (y in 100..104) for (xx in x-2..x+2) {
                    val i=(y*width+xx)*3; pixels[i]=255.toByte(); pixels[i+1]=255.toByte(); pixels[i+2]=255.toByte()
                }
            }
            return RgbFrame(width,240,pixels)
        }
    }
    private class FakeMotor(val w: World) : Motor {
        var clamped = 0
        override suspend fun move(direction: MoveDirection, milliseconds: Long): Long {
            val room = if (w.limitMs == Long.MAX_VALUE) Long.MAX_VALUE
                else if(direction == MoveDirection.FORWARD) w.limitMs-w.netMs else w.limitMs+w.netMs
            val actual = milliseconds.coerceAtMost(room.coerceAtLeast(0))
            if(actual < milliseconds) clamped++
            if(!w.jam) w.theta += direction.sign * actual/1000.0*w.trueSpeed
            w.netMs += direction.sign*actual
            w.clock.advance(Duration.ofMillis(actual+400))
            return actual
        }
        override suspend fun status() = MotorStatus(true, null)
        override suspend fun stop() { w.stopped++ }
        override suspend fun home(): Boolean { w.netMs=0; return true }
        override suspend fun sync(thetaDeg: Double) { w.netMs = (thetaDeg / w.trueSpeed * 1_000.0).toLong() }
        override suspend fun setLimit(minThetaDeg: Double, maxThetaDeg: Double) {
            w.limitMs = (maxOf(abs(minThetaDeg), abs(maxThetaDeg)) / abs(w.trueSpeed) * 1_000.0).roundToInt().toLong()
        }
    }
    private class Store(var saved: PersistedTrackerState? = null) : TrackerStateStore {
        override suspend fun load() = saved
        override suspend fun save(state: PersistedTrackerState) { saved=state }
    }
    private fun setup(): Triple<World, FakeMotor, TrackerController> {
        val c=FastClock(start); val w=World(c); val m=FakeMotor(w)
        val store=Store(); w.store=store
        val t=TrackerController(m, FrameSource { w.frame() }, c,
            ControllerTuning(lat,lon, cycle=Duration.ofSeconds(60), calibrationPause=Duration.ofMinutes(6), jitterFrameSpacing=Duration.ofMillis(1)),
            store, TrackerSafety { _,_->true }, sleeper=ControllerSleeper { c.advance(it) })
        return Triple(w,m,t)
    }
    private suspend fun calibrate(w: World, t: TrackerController) {
        t.setHome()
        val result=t.calibrate().toList()
        assertTrue("calibration completes: $result", result.last().complete)
    }
    private suspend fun runTo(w: World,t: TrackerController,end: Instant, every: (World,TrackerController)->Unit = {_,_->}) {
        while(w.clock.instant()<end) {
            t.cycleOnce(); every(w,t)
            w.clock.advance(Duration.ofSeconds(60))
        }
    }

    @Test fun calibrationLearnsMotorSpeedAndImageSign() = runBlocking {
        val (w,_,t)=setup(); w.theta=w.target()-8.0
        calibrate(w,t)
        val learned = w.store.saved ?: error("calibration state was not persisted")
        assertEquals("image direction",w.imageSign,learned.signX)
        assertTrue("motor speed estimate: ${learned.thetaPerSecond} vs ${w.trueSpeed}",abs(learned.thetaPerSecond-w.trueSpeed)<0.4)
        assertTrue("calibration estimate should match the sensorless panel: estimate=${learned.thetaDeg}, panel=${w.theta}", abs(learned.thetaDeg-w.theta) < 2.0)
    }

    @Test fun clearDayTrackingAndNinetyMinutesCloudDeadReckoningStayWithinReferenceLimits() = runBlocking {
        val (w,_,t)=setup(); w.theta=w.target()-3.0; calibrate(w,t)
        val clearEnd=Instant.parse("2026-10-25T11:00:00Z")
        var clearMax=0.0
        val clearErrors=mutableListOf<Double>()
        runTo(w,t,clearEnd) { world,_-> if(SolarMath.position(lat,lon,world.clock.instant()).elevationDeg>20) {
            val error=abs(world.target()-world.theta); clearErrors += error; clearMax=maxOf(clearMax,error)
        } }
        val p95=clearErrors.sorted().getOrNull((0.95*(clearErrors.size-1)).roundToInt()) ?: Double.POSITIVE_INFINITY
        assertTrue("clear-day p95 < 2 deg: $p95", p95 < 2.0)
        assertTrue("clear-day max < 4 deg: $clearMax", clearMax < 4.0)
        val (cloudWorld,_,cloudTracker)=setup()
        cloudWorld.theta=cloudWorld.target()-3.0
        calibrate(cloudWorld,cloudTracker)
        val cloudStart=Instant.parse("2026-10-25T04:00:00Z")
        runTo(cloudWorld,cloudTracker,cloudStart)
        cloudWorld.clouds=true
        var cloudMax=0.0
        runTo(cloudWorld,cloudTracker,cloudStart.plus(Duration.ofMinutes(90))) { world,_->cloudMax=maxOf(cloudMax,abs(world.target()-world.theta))}
        assertTrue("cloud dead-reckoning max < 6 deg: $cloudMax", cloudMax < 6.0)
    }

    @Test fun jitterWindStowsThenCalmReleases() = runBlocking {
        // Deterministic high/low sun displacement represents gust jitter above the 2.5 px threshold.
        val c=FastClock(start); val windWorld=World(c); val motor=FakeMotor(windWorld); var sample=0
        windWorld.theta=windWorld.target()
        val jitterSource=FrameSource {
            val frame=windWorld.frame(); if(!windWorld.wind) frame else {
                sample++; val x=(width/2 + if(sample%2==0) 24 else -24).coerceIn(3,width-4)
                val p=frame.pixels.copyOf(); for(y in 100..104) for(xx in 120..199) { val i=(y*width+xx)*3;p[i]=90.toByte();p[i+1]=140.toByte();p[i+2]=220.toByte() }
                for(y in 100..104) for(xx in x-2..x+2) { val i=(y*width+xx)*3;p[i]=255.toByte();p[i+1]=255.toByte();p[i+2]=255.toByte() }; RgbFrame(width,240,p)
            }
        }
        val controller=TrackerController(motor,jitterSource,c,ControllerTuning(lat,lon,calibrationPause=Duration.ofMinutes(6),jitterFrameSpacing=Duration.ofMillis(1)),Store(),TrackerSafety { _,_->true },sleeper=ControllerSleeper { c.advance(it) })
        controller.setHome(); assertTrue(controller.calibrate().toList().last().complete)
        val windStart=Instant.parse("2026-10-25T04:00:00Z")
        runTo(windWorld,controller,windStart)
        windWorld.wind=true
        runTo(windWorld,controller,windStart.plus(Duration.ofMinutes(6)))
        assertEquals(com.solarrobo.core.contracts.WindRisk.GUSTY,controller.observeSky().value.windRisk)
        assertTrue("stow under 3 deg: panel=${windWorld.theta}, estimated=${controller.observeState().value.estimatedPanelAngleDeg}",abs(windWorld.theta)<3)
        windWorld.wind=false
        runTo(windWorld,controller,Instant.parse("2026-10-25T04:40:00Z"))
        assertTrue("calm release and tracking error <3 deg: risk=${controller.observeSky().value.windRisk}, panel=${windWorld.theta}, target=${windWorld.target()}",abs(windWorld.target()-windWorld.theta)<3)
    }

    @Test fun jamIsDetectedAndMotorStopped() = runBlocking {
        val (w,_,t)=setup();w.theta=w.target();calibrate(w,t);runTo(w,t,start.plus(Duration.ofMinutes(90)))
        w.jam=true
        runTo(w,t,w.clock.instant().plus(Duration.ofMinutes(90)))
        assertTrue(TrackerFault.JAM in t.observeState().value.faults)
    }

    @Test fun firmwareTravelLimitClampsRunawayMoves() = runBlocking {
        val c=FastClock(start);val w=World(c);w.limitMs=3000
        val m=FakeMotor(w)
        repeat(5) { m.move(MoveDirection.FORWARD,2000) }
        assertTrue("net travel within +/-3000 ms: ${w.netMs}",abs(w.netMs)<=3000)
        assertTrue("firmware clamp was observed", m.clamped > 0)
    }
}
