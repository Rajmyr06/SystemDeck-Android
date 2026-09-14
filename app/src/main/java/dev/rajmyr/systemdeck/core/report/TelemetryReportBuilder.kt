package dev.rajmyr.systemdeck.core.report

import dev.rajmyr.systemdeck.core.format.DeckFormat
import dev.rajmyr.systemdeck.core.telemetry.CollectorHealth
import dev.rajmyr.systemdeck.core.telemetry.SampleFreshness
import dev.rajmyr.systemdeck.data.battery.BatterySnapshot
import dev.rajmyr.systemdeck.data.cpu.CpuSnapshot
import dev.rajmyr.systemdeck.data.device.DeviceSnapshot
import dev.rajmyr.systemdeck.data.gpu.GpuSnapshot
import dev.rajmyr.systemdeck.data.memory.MemorySnapshot
import dev.rajmyr.systemdeck.data.network.NetworkSnapshot
import dev.rajmyr.systemdeck.data.sensors.SensorSnapshot
import dev.rajmyr.systemdeck.data.storage.StorageSnapshot
import dev.rajmyr.systemdeck.data.thermal.ThermalSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TelemetryReportBuilder {
    fun build(
        device: DeviceSnapshot,
        cpu: CpuSnapshot,
        memory: MemorySnapshot,
        battery: BatterySnapshot,
        thermal: ThermalSnapshot,
        network: NetworkSnapshot,
        storage: StorageSnapshot,
        gpu: GpuSnapshot,
        sensors: SensorSnapshot,
        collectorHealth: List<CollectorHealth>,
        generatedAtMillis: Long = System.currentTimeMillis(),
    ): String = buildString {
        appendLine("SystemDeck telemetry snapshot")
        appendLine(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(Date(generatedAtMillis)),
        )
        appendLine()

        section("DEVICE") {
            line("Model", "${device.manufacturer} ${device.model}")
            line("Android", "${device.androidVersion} / API ${device.sdk}")
            line("SoC", device.soc)
            line("Kernel", device.kernel)
            line("ABI", device.abi)
        }

        section("CPU") {
            line("Status", cpu.status.name)
            line("Cores", cpu.cores.size.takeIf { it > 0 }?.toString())
            cpu.clusters.forEach { cluster ->
                line(cluster.label, DeckFormat.frequency(cluster.currentMaxKHz).realValue())
            }
        }

        section("MEMORY") {
            line("Used", DeckFormat.kibToGiB(memory.usedKiB).realValue())
            line("Available", DeckFormat.kibToGiB(memory.availableKiB).realValue())
            line("Total", DeckFormat.kibToGiB(memory.totalKiB).realValue())
            if (memory.swapTotalKiB > 0L) {
                line("Swap used", DeckFormat.kibToGiB(memory.swapUsedKiB).realValue())
                line("Swap total", DeckFormat.kibToGiB(memory.swapTotalKiB).realValue())
            }
        }

        section("BATTERY") {
            line("Level", battery.levelPercent?.let { "$it%" })
            line("Status", battery.chargeStatus.realValue())
            line("Power source", battery.pluggedSource.realValue())
            line("Voltage", DeckFormat.voltage(battery.voltageMilliVolts).realValue())
            line("Current", DeckFormat.current(battery.currentNowMicroAmps).realValue())
            line("Power", DeckFormat.power(battery.powerWatts).realValue())
            line("Temperature", DeckFormat.temperature(battery.temperatureCelsius).realValue())
            line("Health", battery.health.realValue())
            line("Cycles", battery.cycleCount?.toString())
        }

        section("THERMAL") {
            line("Status", thermal.systemStatus.realValue())
            line("Headroom", DeckFormat.thermalHeadroom(thermal.headroom).realValue())
            thermal.sensors.forEach { sensor ->
                line(sensor.label, DeckFormat.temperature(sensor.celsius).realValue())
            }
        }

        section("NETWORK") {
            line("Transport", network.transport.realValue())
            line("Interface", network.interfaceName?.realValue())
            line("Receive", DeckFormat.rate(network.rxBytesPerSecond).realValue())
            line("Transmit", DeckFormat.rate(network.txBytesPerSecond).realValue())
            line("IPv4", network.ipv4.takeIf { it.isNotEmpty() }?.joinToString())
        }

        section("STORAGE") {
            line("Used", DeckFormat.bytes(storage.usedBytes).realValue())
            line("Available", DeckFormat.bytes(storage.availableBytes).realValue())
            line("Total", DeckFormat.bytes(storage.totalBytes).realValue())
            if (storage.totalBytes > 0L) line("Usage", "${storage.usagePercent}%")
        }

        section("GPU") {
            line("Device", gpu.deviceLabel.realValue())
            line("OpenGL ES", gpu.glEsVersion?.realValue())
            line("Current", gpu.currentHz?.let { DeckFormat.frequency(it / 1_000L).realValue() })
            line("Maximum", gpu.maximumHz?.let { DeckFormat.frequency(it / 1_000L).realValue() })
            line(
                "Busy",
                gpu.busyPercent?.let { String.format(Locale.US, "%.1f%%", it) },
            )
        }

        section("SENSORS") {
            line("Inventory", sensors.totalSensors.takeIf { it > 0 }?.toString())
            line("Live", sensors.liveSensors.takeIf { it > 0 }?.toString())
            line("Wake-up", sensors.wakeUpSensors.takeIf { it > 0 }?.toString())
        }

        section("COLLECTORS") {
            collectorHealth.forEach { health ->
                val state = when (health.freshness) {
                    SampleFreshness.Fresh -> "FRESH"
                    SampleFreshness.Stale -> "STALE"
                    SampleFreshness.Waiting -> "WAITING"
                }
                line(health.label, state)
            }
        }

        appendLine("Generated locally by SystemDeck.")
    }

    private inline fun StringBuilder.section(
        title: String,
        block: StringBuilder.() -> Unit,
    ) {
        val body = StringBuilder().apply(block).toString().trimEnd()
        if (body.isNotBlank()) {
            appendLine(title)
            appendLine(body)
            appendLine()
        }
    }

    private fun StringBuilder.line(label: String, value: String?) {
        value?.realValue()?.let { appendLine("$label: $it") }
    }

    private fun String.realValue(): String? {
        val cleaned = trim()
        if (
            cleaned.isEmpty() ||
            cleaned == "-" ||
            cleaned == "–" ||
            cleaned == "—" ||
            cleaned.equals("N/A", ignoreCase = true) ||
            cleaned.equals("Unavailable", ignoreCase = true) ||
            cleaned.equals("Waiting", ignoreCase = true) ||
            cleaned.equals("Unknown", ignoreCase = true)
        ) return null
        return cleaned
    }
}
