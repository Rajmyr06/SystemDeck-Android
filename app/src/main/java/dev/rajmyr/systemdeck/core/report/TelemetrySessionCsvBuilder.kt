package dev.rajmyr.systemdeck.core.report

import dev.rajmyr.systemdeck.core.telemetry.HistoryPoint
import dev.rajmyr.systemdeck.core.telemetry.TelemetryHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TelemetrySessionCsvBuilder {
    fun build(history: TelemetryHistory): String = buildString {
        appendLine("timestamp,metric,value,unit")

        appendSeries(history.cpuFrequencyMHz, "cpu_frequency", "MHz")
        appendSeries(history.memoryUsedPercent, "memory_used", "%")
        appendSeries(history.networkDownKiBps, "network_receive", "KiB/s")
        appendSeries(history.networkUpKiBps, "network_transmit", "KiB/s")
        appendSeries(history.batteryPercent, "battery", "%")
        appendSeries(history.thermalMaxCelsius, "thermal_max", "C")
        appendSeries(history.gpuBusyPercent, "gpu_busy_experimental", "%")
        appendSeries(history.storageUsedPercent, "storage_used", "%")
    }

    private fun StringBuilder.appendSeries(
        points: List<HistoryPoint>,
        metric: String,
        unit: String,
    ) {
        points.forEach { point ->
            val timestamp = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS",
                Locale.US,
            ).format(Date(point.sampledAtMillis))

            append(timestamp)
            append(',')
            append(metric)
            append(',')
            append(String.format(Locale.US, "%.3f", point.value))
            append(',')
            append(unit)
            appendLine()
        }
    }
}
