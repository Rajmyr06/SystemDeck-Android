package dev.rajmyr.systemdeck.core.alerts

import dev.rajmyr.systemdeck.core.model.CollectorStatus
import dev.rajmyr.systemdeck.data.battery.BatterySnapshot
import dev.rajmyr.systemdeck.data.memory.MemorySnapshot
import dev.rajmyr.systemdeck.data.storage.StorageSnapshot
import dev.rajmyr.systemdeck.data.thermal.ThermalSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertEvaluatorTest {
    private val evaluator = AlertEvaluator()

    @Test
    fun masterSwitchOff_producesNoAlerts() {
        val alerts = evaluator.evaluate(
            preferences = AlertPreferences(enabled = false),
            memory = memory(95),
            battery = battery(level = 5, status = "Discharging"),
            thermal = thermal(statusCode = 4, status = "Critical"),
            storage = storage(98),
        )
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun batteryRule_ignoresChargingBattery() {
        val alerts = evaluator.evaluate(
            preferences = AlertPreferences(enabled = true, batteryLowPercent = 20),
            memory = MemorySnapshot.Empty,
            battery = battery(level = 10, status = "Charging"),
            thermal = ThermalSnapshot.Empty,
            storage = StorageSnapshot.Empty,
        )
        assertTrue(alerts.none { it.id == "battery_low" })
    }

    @Test
    fun validatedThresholds_emitExpectedIds() {
        val alerts = evaluator.evaluate(
            preferences = AlertPreferences(
                enabled = true,
                memoryHighPercent = 85,
                batteryLowPercent = 20,
                thermalStatusThreshold = 2,
                storageHighPercent = 90,
            ),
            memory = memory(90),
            battery = battery(level = 15, status = "Discharging"),
            thermal = thermal(statusCode = 3, status = "Severe"),
            storage = storage(95),
        )
        assertEquals(
            setOf("memory_high", "battery_low", "thermal_status", "storage_high"),
            alerts.map { it.id }.toSet(),
        )
    }

    private fun memory(usedPercent: Int): MemorySnapshot {
        val total = 100_000L
        val available = total - (total * usedPercent / 100L)
        return MemorySnapshot(
            status = CollectorStatus.Ok,
            totalKiB = total,
            availableKiB = available,
            freeKiB = available,
            cachedKiB = 0L,
            buffersKiB = 0L,
            slabKiB = 0L,
            swapTotalKiB = 0L,
            swapFreeKiB = 0L,
            sampledAtMillis = 1L,
        )
    }

    private fun battery(level: Int, status: String) = BatterySnapshot(
        status = CollectorStatus.Ok,
        levelPercent = level,
        chargeStatus = status,
        pluggedSource = "None",
        voltageMilliVolts = 4_000,
        currentNowMicroAmps = null,
        currentAverageMicroAmps = null,
        chargeCounterMicroAh = null,
        cycleCount = null,
        temperatureCelsius = 30.0,
        health = "Good",
        technology = null,
        chargeTimeRemainingMillis = null,
        sampledAtMillis = 1L,
    )

    private fun thermal(statusCode: Int, status: String) = ThermalSnapshot(
        status = CollectorStatus.Ok,
        systemStatus = status,
        systemStatusCode = statusCode,
        headroom = null,
        sensors = emptyList(),
        sampledAtMillis = 1L,
    )

    private fun storage(percent: Int) = StorageSnapshot(
        status = CollectorStatus.Ok,
        totalBytes = 100L,
        usedBytes = percent.toLong(),
        availableBytes = (100 - percent).toLong(),
        usagePercent = percent,
        dataPath = "/data",
        sharedStorageState = "mounted",
        sharedStorageEmulated = true,
        sharedStorageRemovable = false,
        sampledAtMillis = 1L,
    )
}
