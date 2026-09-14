package dev.rajmyr.systemdeck.feature.widget

import android.content.Context
import dev.rajmyr.systemdeck.core.format.DeckFormat
import dev.rajmyr.systemdeck.data.battery.BatteryTelemetryCollector
import dev.rajmyr.systemdeck.data.cpu.CpuTelemetryCollector
import dev.rajmyr.systemdeck.data.gpu.GpuTelemetryCollector
import dev.rajmyr.systemdeck.data.memory.MemoryTelemetryCollector
import dev.rajmyr.systemdeck.data.storage.StorageTelemetryCollector
import dev.rajmyr.systemdeck.data.thermal.ThermalTelemetryCollector
import java.util.Locale


data class WidgetTelemetry(
    val cpu: String,
    val cpuMax: String,
    val memoryPercent: Int?,
    val memoryUsed: String,
    val memoryTotal: String,
    val batteryPercent: Int?,
    val batteryState: String,
    val batteryPower: String,
    val batteryVoltage: String,
    val temperatureC: Double?,
    val thermalLabel: String,
    val thermalStatus: String,
    val storagePercent: Int?,
    val storageUsed: String,
    val storageTotal: String,
    val gpuBusy: String,
    val gpuMax: String,
)

object WidgetTelemetryReader {
    fun read(
        context: Context,
        includeExtended: Boolean = false,
    ): WidgetTelemetry {
        val appContext = context.applicationContext
        val cpu = CpuTelemetryCollector().read()
        val memory = MemoryTelemetryCollector().read()
        val battery = BatteryTelemetryCollector(appContext).read()
        val thermal = ThermalTelemetryCollector(appContext).read()

        val fastestCluster = cpu.clusters
            .filter { it.currentMaxKHz != null || it.maxKHz != null }
            .maxByOrNull { it.maxKHz ?: it.currentMaxKHz ?: 0L }
            ?: cpu.clusters.lastOrNull()

        val memoryPercent = if (memory.totalKiB > 0L) {
            ((memory.usedKiB * 100L) / memory.totalKiB)
                .toInt()
                .coerceIn(0, 100)
        } else {
            null
        }

        val hottest = thermal.sensors.maxByOrNull { it.celsius }
        val thermalCelsius = hottest?.celsius ?: battery.temperatureCelsius
        val thermalLabel = when {
            hottest != null -> hottest.label
            battery.temperatureCelsius != null -> "Battery"
            else -> thermal.systemStatus.takeIf { it.isNotBlank() } ?: "Unavailable"
        }

        val storage = if (includeExtended) {
            StorageTelemetryCollector(appContext).read()
        } else {
            null
        }

        val gpu = if (includeExtended) {
            GpuTelemetryCollector(appContext).read()
        } else {
            null
        }

        val storagePercent = storage
            ?.takeIf { it.totalBytes > 0L }
            ?.usagePercent

        val gpuBusy = gpu?.busyPercent
            ?.takeIf { it.isFinite() && it in 0.0..100.0 }
            ?.let { String.format(Locale.US, "%.0f%%", it) }
            ?: "—"

        val gpuMax = gpu?.maximumHz
            ?.takeIf { it > 0L }
            ?.let { DeckFormat.frequency(it / 1_000L) }
            ?: "—"

        return WidgetTelemetry(
            cpu = DeckFormat.frequency(fastestCluster?.currentMaxKHz),
            cpuMax = DeckFormat.frequency(fastestCluster?.maxKHz),
            memoryPercent = memoryPercent,
            memoryUsed = DeckFormat.kibToGiB(memory.usedKiB),
            memoryTotal = DeckFormat.kibToGiB(memory.totalKiB),
            batteryPercent = battery.levelPercent,
            batteryState = battery.chargeStatus,
            batteryPower = DeckFormat.power(battery.powerWatts),
            batteryVoltage = DeckFormat.voltage(battery.voltageMilliVolts),
            temperatureC = thermalCelsius,
            thermalLabel = thermalLabel,
            thermalStatus = thermal.systemStatus.takeIf { it.isNotBlank() } ?: "—",
            storagePercent = storagePercent,
            storageUsed = storage?.let { DeckFormat.bytes(it.usedBytes) } ?: "—",
            storageTotal = storage?.let { DeckFormat.bytes(it.totalBytes) } ?: "—",
            gpuBusy = gpuBusy,
            gpuMax = gpuMax,
        )
    }
}
