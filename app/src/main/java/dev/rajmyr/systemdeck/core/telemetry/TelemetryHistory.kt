package dev.rajmyr.systemdeck.core.telemetry

import dev.rajmyr.systemdeck.data.battery.BatterySnapshot
import dev.rajmyr.systemdeck.data.cpu.CpuSnapshot
import dev.rajmyr.systemdeck.data.gpu.GpuSnapshot
import dev.rajmyr.systemdeck.data.memory.MemorySnapshot
import dev.rajmyr.systemdeck.data.network.NetworkSnapshot
import dev.rajmyr.systemdeck.data.storage.StorageSnapshot
import dev.rajmyr.systemdeck.data.thermal.ThermalSnapshot
import java.util.ArrayDeque


data class HistoryPoint(
    val sampledAtMillis: Long,
    val value: Double,
)

data class TelemetryHistory(
    val cpuFrequencyMHz: List<HistoryPoint> = emptyList(),
    val memoryUsedPercent: List<HistoryPoint> = emptyList(),
    val networkDownKiBps: List<HistoryPoint> = emptyList(),
    val networkUpKiBps: List<HistoryPoint> = emptyList(),
    val batteryPercent: List<HistoryPoint> = emptyList(),
    val thermalMaxCelsius: List<HistoryPoint> = emptyList(),
    val gpuBusyPercent: List<HistoryPoint> = emptyList(),
    val storageUsedPercent: List<HistoryPoint> = emptyList(),
) {
    val hasData: Boolean
        get() = listOf(
            cpuFrequencyMHz,
            memoryUsedPercent,
            networkDownKiBps,
            networkUpKiBps,
            batteryPercent,
            thermalMaxCelsius,
            gpuBusyPercent,
            storageUsedPercent,
        ).any { it.isNotEmpty() }

    val totalPoints: Int
        get() = cpuFrequencyMHz.size +
            memoryUsedPercent.size +
            networkDownKiBps.size +
            networkUpKiBps.size +
            batteryPercent.size +
            thermalMaxCelsius.size +
            gpuBusyPercent.size +
            storageUsedPercent.size
}

class TelemetryHistoryAccumulator(
    private val retentionMillis: Long = DEFAULT_RETENTION_MILLIS,
    private val maxPointsPerSeries: Int = DEFAULT_MAX_POINTS_PER_SERIES,
) {
    private val cpu = ArrayDeque<HistoryPoint>()
    private val memory = ArrayDeque<HistoryPoint>()
    private val networkDown = ArrayDeque<HistoryPoint>()
    private val networkUp = ArrayDeque<HistoryPoint>()
    private val battery = ArrayDeque<HistoryPoint>()
    private val thermal = ArrayDeque<HistoryPoint>()
    private val gpuBusy = ArrayDeque<HistoryPoint>()
    private val storageUsed = ArrayDeque<HistoryPoint>()

    private var lastCpuSample = 0L
    private var lastMemorySample = 0L
    private var lastNetworkSample = 0L
    private var lastBatterySample = 0L
    private var lastThermalSample = 0L
    private var lastGpuSample = 0L
    private var lastStorageSample = 0L

    fun update(
        cpuSnapshot: CpuSnapshot,
        memorySnapshot: MemorySnapshot,
        batterySnapshot: BatterySnapshot,
        thermalSnapshot: ThermalSnapshot,
        networkSnapshot: NetworkSnapshot,
        gpuSnapshot: GpuSnapshot,
        storageSnapshot: StorageSnapshot,
    ): TelemetryHistory {
        if (isNewSample(cpuSnapshot.sampledAtMillis, lastCpuSample)) {
            cpuSnapshot.clusters
                .mapNotNull { it.currentMaxKHz }
                .maxOrNull()
                ?.let { add(cpu, HistoryPoint(cpuSnapshot.sampledAtMillis, it / 1000.0)) }
            lastCpuSample = cpuSnapshot.sampledAtMillis
        }

        if (isNewSample(memorySnapshot.sampledAtMillis, lastMemorySample)) {
            if (memorySnapshot.totalKiB > 0L) {
                val percent = memorySnapshot.usedKiB.toDouble() / memorySnapshot.totalKiB.toDouble() * 100.0
                add(memory, HistoryPoint(memorySnapshot.sampledAtMillis, percent.coerceIn(0.0, 100.0)))
            }
            lastMemorySample = memorySnapshot.sampledAtMillis
        }

        if (isNewSample(networkSnapshot.sampledAtMillis, lastNetworkSample)) {
            networkSnapshot.rxBytesPerSecond?.let {
                add(networkDown, HistoryPoint(networkSnapshot.sampledAtMillis, it / 1024.0))
            }
            networkSnapshot.txBytesPerSecond?.let {
                add(networkUp, HistoryPoint(networkSnapshot.sampledAtMillis, it / 1024.0))
            }
            lastNetworkSample = networkSnapshot.sampledAtMillis
        }

        if (isNewSample(batterySnapshot.sampledAtMillis, lastBatterySample)) {
            batterySnapshot.levelPercent?.let {
                add(battery, HistoryPoint(batterySnapshot.sampledAtMillis, it.toDouble()))
            }
            lastBatterySample = batterySnapshot.sampledAtMillis
        }

        if (isNewSample(thermalSnapshot.sampledAtMillis, lastThermalSample)) {
            thermalSnapshot.sensors.maxOfOrNull { it.celsius }?.let {
                add(thermal, HistoryPoint(thermalSnapshot.sampledAtMillis, it))
            }
            lastThermalSample = thermalSnapshot.sampledAtMillis
        }

        if (isNewSample(gpuSnapshot.sampledAtMillis, lastGpuSample)) {
            gpuSnapshot.busyPercent?.takeIf { it.isFinite() }?.let {
                add(gpuBusy, HistoryPoint(gpuSnapshot.sampledAtMillis, it.coerceIn(0.0, 100.0)))
            }
            lastGpuSample = gpuSnapshot.sampledAtMillis
        }

        if (isNewSample(storageSnapshot.sampledAtMillis, lastStorageSample)) {
            if (storageSnapshot.totalBytes > 0L) {
                add(
                    storageUsed,
                    HistoryPoint(
                        storageSnapshot.sampledAtMillis,
                        storageSnapshot.usagePercent.toDouble().coerceIn(0.0, 100.0),
                    ),
                )
            }
            lastStorageSample = storageSnapshot.sampledAtMillis
        }

        return snapshot()
    }

    fun clear(): TelemetryHistory {
        cpu.clear()
        memory.clear()
        networkDown.clear()
        networkUp.clear()
        battery.clear()
        thermal.clear()
        gpuBusy.clear()
        storageUsed.clear()

        lastCpuSample = 0L
        lastMemorySample = 0L
        lastNetworkSample = 0L
        lastBatterySample = 0L
        lastThermalSample = 0L
        lastGpuSample = 0L
        lastStorageSample = 0L
        return snapshot()
    }

    private fun isNewSample(sampledAtMillis: Long, previous: Long): Boolean =
        sampledAtMillis > 0L && sampledAtMillis != previous

    private fun add(target: ArrayDeque<HistoryPoint>, point: HistoryPoint) {
        target.addLast(point)
        val cutoff = point.sampledAtMillis - retentionMillis
        while (target.isNotEmpty() && target.first().sampledAtMillis < cutoff) {
            target.removeFirst()
        }
        while (target.size > maxPointsPerSeries) {
            target.removeFirst()
        }
    }

    private fun snapshot() = TelemetryHistory(
        cpuFrequencyMHz = cpu.toList(),
        memoryUsedPercent = memory.toList(),
        networkDownKiBps = networkDown.toList(),
        networkUpKiBps = networkUp.toList(),
        batteryPercent = battery.toList(),
        thermalMaxCelsius = thermal.toList(),
        gpuBusyPercent = gpuBusy.toList(),
        storageUsedPercent = storageUsed.toList(),
    )

    companion object {
        const val DEFAULT_RETENTION_MILLIS = 15 * 60_000L
        const val DEFAULT_MAX_POINTS_PER_SERIES = 900
    }
}
