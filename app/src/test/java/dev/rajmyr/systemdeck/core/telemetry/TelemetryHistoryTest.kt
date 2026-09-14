package dev.rajmyr.systemdeck.core.telemetry

import dev.rajmyr.systemdeck.core.model.CollectorStatus
import dev.rajmyr.systemdeck.data.battery.BatterySnapshot
import dev.rajmyr.systemdeck.data.cpu.CpuSnapshot
import dev.rajmyr.systemdeck.data.gpu.GpuSnapshot
import dev.rajmyr.systemdeck.data.memory.MemorySnapshot
import dev.rajmyr.systemdeck.data.network.NetworkSnapshot
import dev.rajmyr.systemdeck.data.storage.StorageSnapshot
import dev.rajmyr.systemdeck.data.thermal.ThermalSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryHistoryTest {
    @Test
    fun retention_and_maxPoints_areEnforced() {
        val accumulator = TelemetryHistoryAccumulator(
            retentionMillis = 10_000L,
            maxPointsPerSeries = 2,
        )

        val h1 = accumulator.update(memorySnapshot = memory(1_000L, 20), cpuSnapshot = CpuSnapshot.Empty, batterySnapshot = BatterySnapshot.Empty, thermalSnapshot = ThermalSnapshot.Empty, networkSnapshot = NetworkSnapshot.Empty, gpuSnapshot = GpuSnapshot.Empty, storageSnapshot = StorageSnapshot.Empty)
        val h2 = accumulator.update(memorySnapshot = memory(2_000L, 40), cpuSnapshot = CpuSnapshot.Empty, batterySnapshot = BatterySnapshot.Empty, thermalSnapshot = ThermalSnapshot.Empty, networkSnapshot = NetworkSnapshot.Empty, gpuSnapshot = GpuSnapshot.Empty, storageSnapshot = StorageSnapshot.Empty)
        val h3 = accumulator.update(memorySnapshot = memory(3_000L, 60), cpuSnapshot = CpuSnapshot.Empty, batterySnapshot = BatterySnapshot.Empty, thermalSnapshot = ThermalSnapshot.Empty, networkSnapshot = NetworkSnapshot.Empty, gpuSnapshot = GpuSnapshot.Empty, storageSnapshot = StorageSnapshot.Empty)

        assertEquals(1, h1.memoryUsedPercent.size)
        assertEquals(2, h2.memoryUsedPercent.size)
        assertEquals(2, h3.memoryUsedPercent.size)
        assertEquals(40.0, h3.memoryUsedPercent.first().value, 0.001)
        assertEquals(60.0, h3.memoryUsedPercent.last().value, 0.001)
    }

    @Test
    fun clear_removesAllSeries() {
        val accumulator = TelemetryHistoryAccumulator()
        val history = accumulator.update(memorySnapshot = memory(1_000L, 50), cpuSnapshot = CpuSnapshot.Empty, batterySnapshot = BatterySnapshot.Empty, thermalSnapshot = ThermalSnapshot.Empty, networkSnapshot = NetworkSnapshot.Empty, gpuSnapshot = GpuSnapshot.Empty, storageSnapshot = StorageSnapshot.Empty)
        assertTrue(history.hasData)
        val cleared = accumulator.clear()
        assertFalse(cleared.hasData)
        assertEquals(0, cleared.totalPoints)
    }

    @Test
    fun statistics_ignoreNonFiniteValues() {
        val stats = listOf(
            HistoryPoint(1L, 10.0),
            HistoryPoint(2L, Double.NaN),
            HistoryPoint(3L, 30.0),
        ).statistics()!!

        assertEquals(2, stats.count)
        assertEquals(20.0, stats.average, 0.001)
        assertEquals(10.0, stats.minimum, 0.001)
        assertEquals(30.0, stats.maximum, 0.001)
    }

    private fun memory(sampledAt: Long, usedPercent: Int): MemorySnapshot {
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
            sampledAtMillis = sampledAt,
        )
    }
}
