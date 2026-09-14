package dev.rajmyr.systemdeck.feature.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rajmyr.systemdeck.core.format.DeckFormat
import dev.rajmyr.systemdeck.core.telemetry.TelemetryHistory
import dev.rajmyr.systemdeck.data.battery.BatterySnapshot
import dev.rajmyr.systemdeck.data.cpu.CpuSnapshot
import dev.rajmyr.systemdeck.data.device.DeviceSnapshot
import dev.rajmyr.systemdeck.data.memory.MemorySnapshot
import dev.rajmyr.systemdeck.data.gpu.GpuSnapshot
import dev.rajmyr.systemdeck.data.sensors.SensorSnapshot
import dev.rajmyr.systemdeck.data.network.NetworkSnapshot
import dev.rajmyr.systemdeck.data.storage.StorageSnapshot
import dev.rajmyr.systemdeck.data.thermal.ThermalSnapshot
import dev.rajmyr.systemdeck.ui.components.DeckAdaptiveGrid
import dev.rajmyr.systemdeck.ui.components.DeckDataRow
import dev.rajmyr.systemdeck.ui.components.DeckPageHeader
import dev.rajmyr.systemdeck.ui.components.DeckSectionLabel
import dev.rajmyr.systemdeck.ui.components.DeckPanel
import dev.rajmyr.systemdeck.ui.components.DeckProgress
import dev.rajmyr.systemdeck.ui.components.DeckSummaryCard
import dev.rajmyr.systemdeck.ui.components.DeckSummaryGrid
import dev.rajmyr.systemdeck.ui.components.DeckSummarySpec
import dev.rajmyr.systemdeck.ui.components.DeckSparkline
import dev.rajmyr.systemdeck.ui.components.KeyValueRow
import dev.rajmyr.systemdeck.ui.theme.DeckAmber
import dev.rajmyr.systemdeck.ui.theme.DeckBlue
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import dev.rajmyr.systemdeck.ui.theme.DeckFonts
import dev.rajmyr.systemdeck.ui.theme.DeckGreen
import dev.rajmyr.systemdeck.ui.theme.DeckMuted
import dev.rajmyr.systemdeck.ui.theme.DeckText

@Composable
fun OverviewScreen(
    device: DeviceSnapshot,
    cpu: CpuSnapshot,
    memory: MemorySnapshot,
    battery: BatterySnapshot,
    thermal: ThermalSnapshot,
    network: NetworkSnapshot,
    storage: StorageSnapshot,
    gpu: GpuSnapshot,
    sensors: SensorSnapshot,
    history: TelemetryHistory,
    compact: Boolean,
) {
    val gap = if (compact) 9.dp else 12.dp
    val fastestActive = cpu.clusters.maxByOrNull { it.currentMaxKHz ?: 0L }
    val memoryPercent = DeckFormat.percent(memory.usedKiB, memory.totalKiB)
    val cpuTemp = thermal.sensor("CPU")

    LazyColumn(verticalArrangement = Arrangement.spacedBy(gap)) {
        item {
            DeckPageHeader(
                title = "Overview",
                subtitle = "Live system state.",
                trailing = "Live",
            )
        }


        item {
            DeckSummaryGrid(
                items = listOf(
                    DeckSummarySpec(
                        label = "CPU clock",
                        value = DeckFormat.frequency(fastestActive?.currentMaxKHz),
                        supporting = buildString {
                            append(fastestActive?.label?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "CPU")
                            fastestActive?.maxKHz?.let { append(" · ceiling ${DeckFormat.frequency(it)}") }
                        },
                        code = "CPU",
                        accent = DeckBlue,
                    ),
                    DeckSummarySpec(
                        label = "Memory",
                        value = "$memoryPercent%",
                        supporting = "${DeckFormat.kibToGiB(memory.usedKiB)} of ${DeckFormat.kibToGiB(memory.totalKiB)} physical RAM",
                        code = "MEM",
                        accent = DeckCyan,
                    ),
                    DeckSummarySpec(
                        label = "Battery",
                        value = battery.levelPercent?.let { "$it%" } ?: "—",
                        supporting = "${battery.chargeStatus} · ${DeckFormat.temperature(battery.temperatureCelsius)}",
                        code = "BAT",
                        accent = DeckGreen,
                    ),
                    DeckSummarySpec(
                        label = "Thermal",
                        value = thermal.systemStatus,
                        supporting = "CPU ${DeckFormat.temperature(cpuTemp?.celsius)}",
                        code = "THM",
                        accent = DeckBlue,
                    ),
                ),
                gap = gap,
                minItemWidth = 220.dp,
                maxColumns = 4,
            )
        }

        item {
            DeckAdaptiveGrid(
                itemCount = 3,
                minItemWidth = 260.dp,
                maxColumns = 3,
                gap = gap,
            ) { index, itemModifier ->
                when (index) {
                    0 -> HistoryCard(
                        title = "CPU history",
                        value = DeckFormat.frequency(fastestActive?.currentMaxKHz),
                        supporting = "Fastest active cluster · last ${history.cpuFrequencyMHz.size} samples",
                        values = history.cpuFrequencyMHz.map { it.value },
                        accent = DeckBlue,
                        modifier = itemModifier,
                    )
                    1 -> HistoryCard(
                        title = "Memory history",
                        value = "$memoryPercent%",
                        supporting = "Used physical RAM · last ${history.memoryUsedPercent.size} samples",
                        values = history.memoryUsedPercent.map { it.value },
                        accent = DeckCyan,
                        modifier = itemModifier,
                    )
                    else -> HistoryCard(
                        title = "Network receive",
                        value = DeckFormat.rateCompact(network.rxBytesPerSecond),
                        supporting = "Device-wide TrafficStats delta · last ${history.networkDownKiBps.size} samples",
                        values = history.networkDownKiBps.map { it.value },
                        accent = DeckGreen,
                        modifier = itemModifier,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.Top,
            ) {
                DeckPanel(
                    title = "CPU frequencies",
                    subtitle = "Current clock by cluster.",
                    trailing = "sysfs · 1 s",
                    code = "CPU",
                    modifier = Modifier.weight(1.08f),
                    accent = DeckBlue,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        if (cpu.clusters.isEmpty()) {
                            Text(
                                cpu.message ?: "CPU frequency data is not available yet.",
                                color = DeckMuted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            cpu.clusters.forEach { cluster ->
                                val max = cluster.maxKHz ?: 0L
                                val current = cluster.currentMaxKHz ?: 0L
                                val accent = if (cluster.label == "PRIME") DeckBlue else DeckCyan

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom,
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "${cluster.label.lowercase().replaceFirstChar { it.uppercase() }} cluster",
                                                color = DeckText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                            )
                                            Text(
                                                text = cluster.cores.joinToString(" · ") { "CPU${it.index}" },
                                                color = DeckMuted,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = DeckFonts.Mono,
                                            )
                                        }
                                        Text(
                                            text = "${DeckFormat.frequency(cluster.currentMaxKHz)}  /  ${DeckFormat.frequency(cluster.maxKHz)}",
                                            color = DeckText,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = DeckFonts.Mono,
                                        )
                                    }
                                    DeckProgress(
                                        fraction = if (max > 0) current.toFloat() / max.toFloat() else 0f,
                                        accent = accent,
                                        height = 5,
                                    )
                                }
                            }
                        }
                    }
                }

                DeckPanel(
                    title = "Memory",
                    subtitle = "Physical RAM and swap.",
                    trailing = "$memoryPercent% used",
                    code = "MEM",
                    modifier = Modifier.weight(0.92f),
                    accent = DeckCyan,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DeckProgress(
                            fraction = if (memory.totalKiB > 0) memory.usedKiB.toFloat() / memory.totalKiB.toFloat() else 0f,
                            accent = DeckCyan,
                            height = 5,
                        )
                        DeckDataRow("Used", "Total minus MemAvailable", DeckFormat.kibToGiB(memory.usedKiB), valueColor = DeckText, accent = DeckCyan)
                        DeckDataRow("Available", "Apps + reclaimable cache", DeckFormat.kibToGiB(memory.availableKiB), accent = DeckBlue)
                        DeckDataRow("Cached", "Kernel page cache", DeckFormat.kibToGiB(memory.cachedKiB), accent = DeckBlue)
                        DeckDataRow("Swap", "Currently allocated", "${DeckFormat.kibToGiB(memory.swapUsedKiB)} / ${DeckFormat.kibToGiB(memory.swapTotalKiB)}", accent = DeckBlue)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.Top,
            ) {
                DeckPanel(
                    title = "Battery",
                    subtitle = "Level, power and charge state.",
                    trailing = "5 s",
                    code = "BAT",
                    accent = DeckGreen,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        DeckDataRow("Status", "Current charge state", battery.chargeStatus, valueColor = DeckText, accent = DeckGreen)
                        DeckDataRow("Voltage", "Battery terminal voltage", DeckFormat.voltage(battery.voltageMilliVolts), accent = DeckBlue)
                        DeckDataRow("Current", "Signed instantaneous flow", DeckFormat.current(battery.currentNowMicroAmps), accent = DeckBlue)
                        DeckDataRow("Power", "Voltage × current", DeckFormat.power(battery.powerWatts), accent = DeckCyan)
                    }
                }

                DeckPanel(
                    title = "Thermal",
                    subtitle = "System state and key temperatures.",
                    trailing = "5 s",
                    code = "THM",
                    accent = DeckBlue,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        DeckDataRow("System", "PowerManager status", thermal.systemStatus, valueColor = DeckText, accent = DeckGreen)
                        DeckDataRow("CPU", thermal.sensor("CPU")?.source ?: "Sensor unavailable", DeckFormat.temperature(thermal.sensor("CPU")?.celsius), accent = DeckBlue)
                        DeckDataRow("GPU", thermal.sensor("GPU")?.source ?: "Sensor unavailable", DeckFormat.temperature(thermal.sensor("GPU")?.celsius), accent = DeckCyan)
                        DeckDataRow("Battery", thermal.sensor("Battery")?.source ?: "Sensor unavailable", DeckFormat.temperature(thermal.sensor("Battery")?.celsius), accent = DeckCyan)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.Top,
            ) {
                DeckPanel(
                    title = "Network",
                    subtitle = "Active connection and traffic.",
                    trailing = network.transport,
                    code = "NET",
                    accent = DeckBlue,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        DeckDataRow("Down", "Latest 1 s receive rate", DeckFormat.rate(network.rxBytesPerSecond), accent = DeckCyan)
                        DeckDataRow("Up", "Latest 1 s transmit rate", DeckFormat.rate(network.txBytesPerSecond), accent = DeckBlue)
                        DeckDataRow("Interface", "Active LinkProperties interface", network.interfaceName ?: "—", accent = DeckBlue)
                        DeckDataRow("IPv4", "Local address", network.ipv4.firstOrNull() ?: "—", accent = DeckCyan)
                    }
                }

                DeckPanel(
                    title = "Storage",
                    subtitle = "Apps and user-data volume.",
                    trailing = if (storage.totalBytes > 0L) "${storage.usagePercent}% used" else "Waiting",
                    code = "SSD",
                    accent = DeckCyan,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DeckProgress(
                            fraction = if (storage.totalBytes > 0L) storage.usedBytes.toFloat() / storage.totalBytes.toFloat() else 0f,
                            accent = DeckBlue,
                            height = 5,
                        )
                        DeckDataRow("Used", "Allocated on data volume", DeckFormat.bytes(storage.usedBytes), accent = DeckBlue)
                        DeckDataRow("Available", "Free for apps and files", DeckFormat.bytes(storage.availableBytes), accent = DeckGreen)
                        DeckDataRow("Capacity", "StatFs total", DeckFormat.bytes(storage.totalBytes), accent = DeckCyan)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.Top,
            ) {
                DeckPanel(
                    title = "GPU",
                    subtitle = "Readable KGSL signals.",
                    trailing = "Experimental",
                    code = "GPU",
                    accent = DeckAmber,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        DeckDataRow("Device", "GPU telemetry source", gpu.deviceLabel, accent = DeckBlue)
                        DeckDataRow("Clock", "Current readable frequency", formatGpuHz(gpu.currentHz), accent = DeckCyan)
                        DeckDataRow("Ceiling", "Configured maximum", formatGpuHz(gpu.maximumHz), accent = DeckBlue)
                        DeckDataRow("Busy", "Experimental KGSL ratio", gpu.busyPercent?.let { String.format(java.util.Locale.US, "%.1f%%", it) } ?: "—", accent = DeckAmber)
                    }
                }

                DeckPanel(
                    title = "Sensors",
                    subtitle = "Inventory and live channels.",
                    trailing = "${sensors.liveSensors} live",
                    code = "SNS",
                    accent = DeckGreen,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        DeckDataRow("Inventory", "Hardware sensor entries", sensors.totalSensors.toString(), accent = DeckBlue)
                        DeckDataRow("Live", "Channels delivering values", sensors.liveSensors.toString(), accent = DeckGreen)
                        DeckDataRow("Wake-up", "Can wake application processor", sensors.wakeUpSensors.toString(), accent = DeckCyan)
                        DeckDataRow("Dynamic", "Can attach at runtime", sensors.dynamicSensors.toString(), accent = DeckBlue)
                    }
                }
            }
        }

        item {
            DeckPanel(
                title = "This device",
                subtitle = "Hardware and Android identity.",
                trailing = "Read only",
                code = "DEV",
                accent = DeckBlue,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap * 2),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        KeyValueRow("Model", "${device.manufacturer} ${device.model}")
                        KeyValueRow("Codename", device.device)
                        KeyValueRow("SoC", device.soc)
                        KeyValueRow("ABI", device.abi)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        KeyValueRow("Android", "${device.androidVersion} · API ${device.sdk}")
                        KeyValueRow("Logical cores", "${device.cpuCores}")
                        KeyValueRow("Cluster layout", cpu.clusters.joinToString(" + ") { it.cores.size.toString() }.ifBlank { "—" })
                        KeyValueRow("Access", "No root required")
                    }
                }
            }
        }
    }
}


@Composable
private fun HistoryCard(
    title: String,
    value: String,
    supporting: String,
    values: List<Double>,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    DeckPanel(
        title = title,
        subtitle = supporting,
        trailing = value,
        modifier = modifier,
        accent = accent,
    ) {
        if (values.size >= 2) {
            DeckSparkline(values = values, accent = accent)
        } else {
            Text(
                text = "Collecting…",
                color = DeckMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun formatGpuHz(hz: Long?): String {
    if (hz == null || hz <= 0L) return "—"
    return if (hz >= 1_000_000_000L) {
        String.format(java.util.Locale.US, "%.2f GHz", hz / 1_000_000_000.0)
    } else {
        String.format(java.util.Locale.US, "%.0f MHz", hz / 1_000_000.0)
    }
}
