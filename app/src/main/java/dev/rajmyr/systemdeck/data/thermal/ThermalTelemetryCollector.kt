package dev.rajmyr.systemdeck.data.thermal

import android.content.Context
import android.os.Build
import android.os.PowerManager
import dev.rajmyr.systemdeck.core.model.CollectorStatus
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class ThermalTelemetryCollector(
    context: Context,
) {
    private val powerManager = context.applicationContext.getSystemService(PowerManager::class.java)

    fun observe(intervalMillis: Long = 5_000L): Flow<ThermalSnapshot> = flow {
        while (currentCoroutineContext().isActive) {
            emit(read())
            delay(intervalMillis)
        }
    }.flowOn(Dispatchers.IO)

    fun read(): ThermalSnapshot {
        val statusCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { powerManager.currentThermalStatus }.getOrDefault(PowerManager.THERMAL_STATUS_NONE)
        } else {
            PowerManager.THERMAL_STATUS_NONE
        }

        val headroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { powerManager.getThermalHeadroom(0) }
                .getOrNull()
                ?.takeUnless { it.isNaN() || it.isInfinite() }
                ?.takeIf { it >= 0f }
        } else null

        val raw = readRawZones()
        val sensors = buildList {
            pick(raw, "CPU", listOf("cpu_therm", "cpuss-1-usr", "cpuss-0-usr", "cpu-0-0-usr"))?.let(::add)
            pick(raw, "GPU", listOf("gpuss-0-usr", "gpuss-1-usr", "gpuss-max-step"))?.let(::add)
            pick(raw, "Battery", listOf("battery", "bms", "batt_slave_temp", "batt_therm"))?.let(::add)
            pick(raw, "Wi-Fi", listOf("wifi_therm", "cwlan-usr"))?.let(::add)
            pick(raw, "Charger", listOf("charger_therm0"))?.let(::add)
            pick(raw, "Memory", listOf("ddr-usr"))?.let(::add)
            pick(raw, "NPU", listOf("npu-usr"))?.let(::add)
            pick(raw, "Backlight", listOf("backlight_therm"))?.let(::add)
        }

        val collectorStatus = when {
            sensors.isNotEmpty() -> CollectorStatus.Ok
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> CollectorStatus.Degraded
            else -> CollectorStatus.Unavailable
        }

        return ThermalSnapshot(
            status = collectorStatus,
            systemStatus = thermalStatusLabel(statusCode),
            systemStatusCode = statusCode,
            headroom = headroom,
            sensors = sensors,
            sampledAtMillis = System.currentTimeMillis(),
            message = when (collectorStatus) {
                CollectorStatus.Ok -> null
                CollectorStatus.Degraded -> "Android thermal status is available, but readable sensor zones were not found."
                CollectorStatus.Unavailable -> "Thermal telemetry is unavailable."
            },
        )
    }

    private fun readRawZones(): Map<String, Double> {
        val root = File("/sys/class/thermal")
        return root.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory && it.name.startsWith("thermal_zone") }
            ?.mapNotNull { zone ->
                val type = readText(File(zone, "type")) ?: return@mapNotNull null
                val raw = readText(File(zone, "temp"))?.toLongOrNull() ?: return@mapNotNull null
                normalizeTemperature(type, raw)?.let { type to it }
            }
            ?.toMap()
            .orEmpty()
    }

    private fun pick(
        raw: Map<String, Double>,
        label: String,
        candidates: List<String>,
    ): ThermalSensorSnapshot? {
        candidates.forEach { source ->
            raw[source]?.let { value ->
                return ThermalSensorSnapshot(label = label, source = source, celsius = value)
            }
        }
        return null
    }

    private fun normalizeTemperature(type: String, raw: Long): Double? {
        val value = when {
            type == "batt_therm" && raw in 100..1000 -> raw / 10.0
            kotlin.math.abs(raw) >= 1000 -> raw / 1000.0
            else -> raw.toDouble()
        }
        return value.takeIf { it in -20.0..130.0 }
    }

    private fun readText(file: File): String? = runCatching {
        file.readText().trim().takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun thermalStatusLabel(status: Int): String = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "Normal"
        PowerManager.THERMAL_STATUS_LIGHT -> "Light throttling"
        PowerManager.THERMAL_STATUS_MODERATE -> "Moderate throttling"
        PowerManager.THERMAL_STATUS_SEVERE -> "Severe throttling"
        PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
        else -> "Unknown"
    }
}
