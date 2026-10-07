package com.solarrobo.core.contracts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractsTest {

    @Test
    fun testRoboModeEnumValues() {
        val expected = listOf("NORMAL", "OPTIMIZING", "CONSERVING", "PROTECTING", "FAULT", "SAFE")
        val actual = RoboMode.entries.map { it.name }
        assertEquals(expected, actual)
    }

    @Test
    fun testRoboSnapshotConstruction() {
        val snapshot = RoboSnapshot(
            deviceId = "ROBO-001",
            name = "Solar Robo 1",
            connected = true,
            mode = RoboMode.NORMAL,
            panelAngleDeg = 45.0f,
            targetAngleDeg = 48.0f,
            generationWatts = 320.5f,
            batteryPercent = 95.0f,
            timestamp = 1700000000L
        )
        assertEquals("ROBO-001", snapshot.deviceId)
        assertEquals("Solar Robo 1", snapshot.name)
        assertTrue(snapshot.connected)
        assertEquals(RoboMode.NORMAL, snapshot.mode)
        assertEquals(45.0f, snapshot.panelAngleDeg, 0.001f)
        assertEquals(48.0f, snapshot.targetAngleDeg, 0.001f)
        assertEquals(320.5f, snapshot.generationWatts, 0.001f)
        assertEquals(95.0f, snapshot.batteryPercent, 0.001f)
        assertEquals(1700000000L, snapshot.timestamp)
    }

    @Test
    fun testGridStateEnumValues() {
        val expected = listOf("IMPORTING", "EXPORT_READY", "ISOLATED", "UNKNOWN")
        val actual = GridState.entries.map { it.name }
        assertEquals(expected, actual)
    }

    @Test
    fun testEnergySnapshotConstruction() {
        val energy = EnergySnapshot(
            generatedWatts = 450.0f,
            consumedWatts = 120.0f,
            batteryPercent = 88.0f,
            batteryPowerWatts = 330.0f,
            reservePercent = 20.0f,
            gridState = GridState.EXPORT_READY,
            timestamp = 1700000000L
        )
        assertEquals(450.0f, energy.generatedWatts, 0.001f)
        assertEquals(120.0f, energy.consumedWatts, 0.001f)
        assertEquals(88.0f, energy.batteryPercent, 0.001f)
        assertEquals(330.0f, energy.batteryPowerWatts, 0.001f)
        assertEquals(20.0f, energy.reservePercent, 0.001f)
        assertEquals(GridState.EXPORT_READY, energy.gridState)
        assertEquals(1700000000L, energy.timestamp)
    }

    @Test
    fun testEnergyHistoryPointConstruction() {
        val point = EnergyHistoryPoint(
            timestamp = 1700000000L,
            generatedWh = 2400.0f,
            consumedWh = 1100.0f,
            peakWatts = 480.0f,
            solarEfficiency = 0.94f
        )
        assertEquals(1700000000L, point.timestamp)
        assertEquals(2400.0f, point.generatedWh, 0.001f)
        assertEquals(1100.0f, point.consumedWh, 0.001f)
        assertEquals(480.0f, point.peakWatts, 0.001f)
        assertEquals(0.94f, point.solarEfficiency, 0.001f)
    }

    @Test
    fun testRoboCommandVariants() {
        val move = RoboCommand.MoveToAngle(35.5f)
        assertEquals(35.5f, move.angleDeg, 0.001f)

        val stop: RoboCommand = RoboCommand.StopMotion
        assertTrue(stop is RoboCommand.StopMotion)

        val safe: RoboCommand = RoboCommand.SafePosition
        assertTrue(safe is RoboCommand.SafePosition)
    }

    @Test
    fun testCommandResultConstructionAndDefaults() {
        val before = System.currentTimeMillis()
        val result = CommandResult(
            accepted = true,
            commandId = "CMD-101"
        )
        val after = System.currentTimeMillis()

        assertTrue(result.accepted)
        assertEquals("CMD-101", result.commandId)
        assertNull(result.reason)
        assertTrue(result.timestamp in before..after)

        val rejected = CommandResult(
            accepted = false,
            commandId = "CMD-102",
            reason = "High wind limit",
            timestamp = 1700000000L
        )
        assertFalse(rejected.accepted)
        assertEquals("High wind limit", rejected.reason)
        assertEquals(1700000000L, rejected.timestamp)
    }

    @Test
    fun testSafetyLevelEnumValues() {
        val expected = listOf("NORMAL", "CAUTION", "PROTECTING", "FAULT", "EMERGENCY")
        val actual = SafetyLevel.entries.map { it.name }
        assertEquals(expected, actual)
    }

    @Test
    fun testSafetyEventConstructionAndDefaults() {
        val event = SafetyEvent(
            id = "EVT-01",
            level = SafetyLevel.CAUTION,
            code = "WIND_WARN",
            message = "High wind detected",
            createdAt = 1700000000L
        )
        assertEquals("EVT-01", event.id)
        assertEquals(SafetyLevel.CAUTION, event.level)
        assertEquals("WIND_WARN", event.code)
        assertEquals("High wind detected", event.message)
        assertEquals(1700000000L, event.createdAt)
        assertFalse(event.acknowledged)
    }

    @Test
    fun testSafetyDecisionVariants() {
        val allow: SafetyDecision = SafetyDecision.Allow
        assertTrue(allow is SafetyDecision.Allow)

        val block: SafetyDecision = SafetyDecision.Block("Motor jammed")
        assertTrue(block is SafetyDecision.Block)
        assertEquals("Motor jammed", (block as SafetyDecision.Block).reason)

        val modify: SafetyDecision = SafetyDecision.Modify(
            command = RoboCommand.SafePosition,
            reason = "High wind stow override"
        )
        assertTrue(modify is SafetyDecision.Modify)
        val mod = modify as SafetyDecision.Modify
        assertTrue(mod.command is RoboCommand.SafePosition)
        assertEquals("High wind stow override", mod.reason)
    }

    @Test
    fun testEnvironmentSnapshotConstruction() {
        val env = EnvironmentSnapshot(
            temperatureC = 28.5f,
            humidityPercent = 55.0f,
            lightLux = 95000.0f,
            windSpeedMps = 4.2f,
            rainDetected = false,
            panelTemperatureC = 42.0f,
            timestamp = 1700000000L
        )
        assertEquals(28.5f, env.temperatureC, 0.001f)
        assertEquals(55.0f, env.humidityPercent, 0.001f)
        assertEquals(95000.0f, env.lightLux, 0.001f)
        assertEquals(4.2f, env.windSpeedMps, 0.001f)
        assertFalse(env.rainDetected)
        assertEquals(42.0f, env.panelTemperatureC, 0.001f)
        assertEquals(1700000000L, env.timestamp)
    }

    @Test
    fun testActivityTypeEnumValues() {
        val expected = listOf("MOVEMENT", "ENERGY", "SAFETY", "SYSTEM", "AI", "RECOVERY")
        val actual = ActivityType.entries.map { it.name }
        assertEquals(expected, actual)
    }

    @Test
    fun testActivityEventConstruction() {
        val activity = ActivityEvent(
            id = "ACT-01",
            type = ActivityType.MOVEMENT,
            title = "Track Sun",
            detail = "Panel rotated to 42°",
            timestamp = 1700000000L
        )
        assertEquals("ACT-01", activity.id)
        assertEquals(ActivityType.MOVEMENT, activity.type)
        assertEquals("Track Sun", activity.title)
        assertEquals("Panel rotated to 42°", activity.detail)
        assertEquals(1700000000L, activity.timestamp)
    }

    @Test
    fun testHealthStatusEnumValues() {
        val expected = listOf("HEALTHY", "DEGRADED", "CRITICAL", "OFFLINE")
        val actual = HealthStatus.entries.map { it.name }
        assertEquals(expected, actual)
    }

    @Test
    fun testDeviceHealthAndHealthIssueConstruction() {
        val issue = HealthIssue(
            subsystem = "motor",
            issue = "Elevated friction on azimuth axis",
            severity = HealthStatus.DEGRADED,
            suggestedAction = "Lubricate mechanical joints"
        )
        assertEquals("motor", issue.subsystem)
        assertEquals("Elevated friction on azimuth axis", issue.issue)
        assertEquals(HealthStatus.DEGRADED, issue.severity)
        assertEquals("Lubricate mechanical joints", issue.suggestedAction)

        val health = DeviceHealth(
            overallStatus = HealthStatus.HEALTHY,
            motorStatus = HealthStatus.HEALTHY,
            batteryHealthPercent = 98.0f,
            solarPanelStatus = HealthStatus.HEALTHY,
            sensorsStatus = HealthStatus.HEALTHY,
            cameraStatus = HealthStatus.HEALTHY,
            controllerTemperatureC = 36.5f,
            issues = listOf(issue),
            timestamp = 1700000000L
        )
        assertEquals(HealthStatus.HEALTHY, health.overallStatus)
        assertEquals(HealthStatus.HEALTHY, health.motorStatus)
        assertEquals(98.0f, health.batteryHealthPercent, 0.001f)
        assertEquals(HealthStatus.HEALTHY, health.solarPanelStatus)
        assertEquals(HealthStatus.HEALTHY, health.sensorsStatus)
        assertEquals(HealthStatus.HEALTHY, health.cameraStatus)
        assertEquals(36.5f, health.controllerTemperatureC, 0.001f)
        assertEquals(1, health.issues.size)
        assertEquals("motor", health.issues.first().subsystem)
        assertEquals(1700000000L, health.timestamp)
    }

    @Test
    fun testNotificationPriorityEnumValues() {
        val expected = listOf("LOW", "MEDIUM", "HIGH", "CRITICAL")
        val actual = NotificationPriority.entries.map { it.name }
        assertEquals(expected, actual)
    }

    @Test
    fun testNotificationEventConstructionAndDefaults() {
        val notif = NotificationEvent(
            id = "NOTIF-01",
            title = "High Wind Stow",
            body = "Tracker moved to safe horizontal stow",
            priority = NotificationPriority.HIGH,
            timestamp = 1700000000L
        )
        assertEquals("NOTIF-01", notif.id)
        assertEquals("High Wind Stow", notif.title)
        assertEquals("Tracker moved to safe horizontal stow", notif.body)
        assertEquals(NotificationPriority.HIGH, notif.priority)
        assertEquals(1700000000L, notif.timestamp)
        assertFalse(notif.isRead)
        assertNull(notif.actionDeepLink)

        val notifCustom = NotificationEvent(
            id = "NOTIF-02",
            title = "System Alert",
            body = "Check sensors",
            priority = NotificationPriority.CRITICAL,
            timestamp = 1700000000L,
            isRead = true,
            actionDeepLink = "solarrobo://health"
        )
        assertTrue(notifCustom.isRead)
        assertEquals("solarrobo://health", notifCustom.actionDeepLink)
    }
}
