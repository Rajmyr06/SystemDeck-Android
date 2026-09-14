package dev.rajmyr.systemdeck.feature.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.rajmyr.systemdeck.core.report.TelemetryExportManager
import dev.rajmyr.systemdeck.core.report.TelemetryReportBuilder
import dev.rajmyr.systemdeck.core.report.TelemetrySessionCsvBuilder
import dev.rajmyr.systemdeck.core.telemetry.CollectorHealth
import dev.rajmyr.systemdeck.core.telemetry.SampleFreshness
import dev.rajmyr.systemdeck.core.telemetry.TelemetryHistory
import dev.rajmyr.systemdeck.core.telemetry.statistics
import dev.rajmyr.systemdeck.data.battery.BatterySnapshot
import dev.rajmyr.systemdeck.data.cpu.CpuSnapshot
import dev.rajmyr.systemdeck.data.device.DeviceSnapshot
import dev.rajmyr.systemdeck.data.memory.MemorySnapshot
import dev.rajmyr.systemdeck.data.gpu.GpuSnapshot
import dev.rajmyr.systemdeck.data.sensors.SensorSnapshot
import dev.rajmyr.systemdeck.data.network.NetworkSnapshot
import dev.rajmyr.systemdeck.data.storage.StorageSnapshot
import dev.rajmyr.systemdeck.data.thermal.ThermalSnapshot
import dev.rajmyr.systemdeck.ui.components.DeckDataRow
import dev.rajmyr.systemdeck.ui.components.DeckInfoBox
import dev.rajmyr.systemdeck.ui.components.DeckPageHeader
import dev.rajmyr.systemdeck.ui.components.DeckPanel
import dev.rajmyr.systemdeck.ui.components.DeckSummaryCard
import dev.rajmyr.systemdeck.ui.theme.DeckAmber
import dev.rajmyr.systemdeck.ui.theme.DeckBlue
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import dev.rajmyr.systemdeck.ui.theme.DeckGreen

@Composable
fun DiagnosticsScreen(
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
    collectorHealth: List<CollectorHealth>,
    onClearHistory: () -> Unit,
) {
    val context = LocalContext.current
    val readableFrequency = cpu.cores.count { it.currentKHz != null }
    val readableGovernor = cpu.cores.count { it.governor != null }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item {
            DeckPageHeader(
                title = "Diagnostics",
                subtitle = "Collector health and data availability.",
                trailing = "Live",
            )
        }


        item {
            DeckPanel(
                title = "Export & session",
                subtitle = "Share real snapshot data from app cache; no storage permission is requested.",
                code = "OUT",
                accent = DeckBlue,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val report = TelemetryReportBuilder.build(
                                device = device,
                                cpu = cpu,
                                memory = memory,
                                battery = battery,
                                thermal = thermal,
                                network = network,
                                storage = storage,
                                gpu = gpu,
                                sensors = sensors,
                                collectorHealth = collectorHealth,
                            )
                            val uri = TelemetryExportManager.reportUri(context, report)
                            val sendIntent = TelemetryExportManager.shareIntent(
                                uri = uri,
                                mimeType = "text/plain",
                                subject = "SystemDeck telemetry snapshot",
                            )
                            context.startActivity(
                                android.content.Intent.createChooser(sendIntent, "Share snapshot"),
                            )
                        },
                    ) {
                        Text("Share snapshot")
                    }

                    TextButton(
                        enabled = history.hasData,
                        onClick = {
                            val csv = TelemetrySessionCsvBuilder.build(history)
                            val uri = TelemetryExportManager.sessionCsvUri(context, csv)
                            val sendIntent = TelemetryExportManager.shareIntent(
                                uri = uri,
                                mimeType = "text/csv",
                                subject = "SystemDeck session history",
                            )
                            context.startActivity(
                                android.content.Intent.createChooser(sendIntent, "Share session history"),
                            )
                        },
                    ) {
                        Text("Share session")
                    }

                    TextButton(
                        enabled = history.hasData,
                        onClick = onClearHistory,
                    ) {
                        Text("Clear session")
                    }
                }
            }
        }

        item {
            val fresh = collectorHealth.count { it.freshness == SampleFreshness.Fresh }
            val stale = collectorHealth.count { it.freshness == SampleFreshness.Stale }
            val waiting = collectorHealth.count { it.freshness == SampleFreshness.Waiting }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                DeckSummaryCard(
                    label = "Fresh",
                    value = fresh.toString(),
                    modifier = Modifier.weight(1f),
                    supporting = "Within expected cadence",
                    code = "OK",
                    accent = DeckGreen,
                )
                DeckSummaryCard(
                    label = "Stale",
                    value = stale.toString(),
                    modifier = Modifier.weight(1f),
                    supporting = "Outside expected cadence",
                    code = "AGE",
                    accent = if (stale > 0) DeckAmber else DeckGreen,
                )
                DeckSummaryCard(
                    label = "Starting",
                    value = waiting.toString(),
                    modifier = Modifier.weight(1f),
                    supporting = "Waiting for first sample",
                    code = "WAIT",
                    accent = DeckBlue,
                )
            }
        }

        item {
            DeckPanel(
                title = "Session buffer",
                subtitle = "Rolling in-memory history used by trends and CSV export.",
                code = "HIST",
                accent = DeckCyan,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    DeckDataRow("Retention", "Time-based rolling window", "15 min", accent = DeckCyan)
                    DeckDataRow("Series", "CPU · memory · RX/TX · battery · thermal · GPU · storage", "8", accent = DeckBlue)
                    DeckDataRow("Points", "Currently retained across all series", history.totalPoints.toString(), accent = DeckGreen)
                    DeckDataRow("Persistence", "Cleared when the app process restarts", "MEMORY ONLY", accent = DeckBlue)
                }
            }
        }

        item {
            val cpuStats = history.cpuFrequencyMHz.statistics()
            val memoryStats = history.memoryUsedPercent.statistics()
            val networkDownStats = history.networkDownKiBps.statistics()
            val thermalStats = history.thermalMaxCelsius.statistics()
            val gpuStats = history.gpuBusyPercent.statistics()

            DeckPanel(
                title = "Session statistics",
                subtitle = "Derived only from samples currently retained in the 15-minute in-memory buffer.",
                code = "STAT",
                accent = DeckGreen,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    cpuStats?.let {
                        DeckDataRow(
                            "CPU frequency",
                            "Fastest-cluster frequency range",
                            String.format(java.util.Locale.US, "%.0f–%.0f MHz · avg %.0f", it.minimum, it.maximum, it.average),
                            accent = DeckBlue,
                        )
                    }
                    memoryStats?.let {
                        DeckDataRow(
                            "Memory used",
                            "Physical-memory used percentage",
                            String.format(java.util.Locale.US, "avg %.1f%% · peak %.1f%%", it.average, it.maximum),
                            accent = DeckCyan,
                        )
                    }
                    networkDownStats?.let {
                        DeckDataRow(
                            "Network RX",
                            "Device TrafficStats receive rate",
                            String.format(java.util.Locale.US, "avg %.1f KiB/s · peak %.1f", it.average, it.maximum),
                            accent = DeckBlue,
                        )
                    }
                    thermalStats?.let {
                        DeckDataRow(
                            "Thermal max",
                            "Hottest canonical thermal signal",
                            String.format(java.util.Locale.US, "avg %.1f°C · peak %.1f°C", it.average, it.maximum),
                            accent = DeckAmber,
                        )
                    }
                    gpuStats?.let {
                        DeckDataRow(
                            "GPU busy",
                            "Experimental KGSL ratio",
                            String.format(java.util.Locale.US, "avg %.1f%% · peak %.1f%%", it.average, it.maximum),
                            accent = DeckAmber,
                        )
                    }
                    if (listOf(cpuStats, memoryStats, networkDownStats, thermalStats, gpuStats).all { it == null }) {
                        Text("Collecting enough session data…")
                    }
                }
            }
        }

        item {
            DeckPanel(
                title = "Collector freshness",
                subtitle = "Freshness against each collector cadence.",
                code = "AGE",
                accent = DeckBlue,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    collectorHealth.forEach { health ->
                        val freshness = when (health.freshness) {
                            SampleFreshness.Fresh -> "FRESH"
                            SampleFreshness.Stale -> "STALE"
                            SampleFreshness.Waiting -> "WAITING"
                        }
                        val age = health.ageMillis?.let { millis ->
                            if (millis < 1_000L) "<1 s" else String.format(java.util.Locale.US, "%.1f s", millis / 1000.0)
                        } ?: "—"
                        DeckDataRow(
                            health.label,
                            "${health.status.name.uppercase()} · age $age · target ${health.expectedIntervalMillis / 1000}s",
                            freshness,
                            accent = when (health.freshness) {
                                SampleFreshness.Fresh -> DeckGreen
                                SampleFreshness.Stale -> DeckAmber
                                SampleFreshness.Waiting -> DeckBlue
                            },
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                DeckSummaryCard(
                    label = "CPU",
                    value = cpu.status.name.uppercase(),
                    modifier = Modifier.weight(1f),
                    supporting = "$readableFrequency / ${cpu.cores.size} frequency nodes",
                    code = "CPU",
                    accent = if (cpu.status.name == "Ok") DeckGreen else DeckBlue,
                )
                DeckSummaryCard(
                    label = "Memory",
                    value = memory.status.name.uppercase(),
                    modifier = Modifier.weight(1f),
                    supporting = if (memory.totalKiB > 0) "/proc/meminfo readable" else "MemTotal unavailable",
                    code = "MEM",
                    accent = if (memory.status.name == "Ok") DeckGreen else DeckBlue,
                )
                DeckSummaryCard(
                    label = "Battery",
                    value = battery.status.name.uppercase(),
                    modifier = Modifier.weight(1f),
                    supporting = if (battery.currentNowMicroAmps != null) "Electrical current available" else "Basic state only",
                    code = "BAT",
                    accent = if (battery.status.name == "Ok") DeckGreen else DeckBlue,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                DeckSummaryCard(
                    label = "Thermal",
                    value = thermal.status.name.uppercase(),
                    modifier = Modifier.weight(1f),
                    supporting = "${thermal.sensors.size} canonical sensor signals",
                    code = "THM",
                    accent = if (thermal.status.name == "Ok") DeckGreen else DeckBlue,
                )
                DeckSummaryCard(
                    label = "Network",
                    value = network.status.name.uppercase(),
                    modifier = Modifier.weight(1f),
                    supporting = if (network.connected) "${network.transport} · ${network.interfaceName ?: "interface unknown"}" else "No active network",
                    code = "NET",
                    accent = if (network.status.name == "Ok") DeckGreen else DeckBlue,
                )
                DeckSummaryCard(
                    label = "Storage",
                    value = storage.status.name.uppercase(),
                    modifier = Modifier.weight(1f),
                    supporting = if (storage.totalBytes > 0L) "StatFs data volume readable" else "Capacity unavailable",
                    code = "SSD",
                    accent = if (storage.status.name == "Ok") DeckGreen else DeckBlue,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                DeckSummaryCard(
                    label = "Sensors",
                    value = sensors.status.name.uppercase(),
                    modifier = Modifier.weight(1f),
                    supporting = "${sensors.totalSensors} inventory · ${sensors.liveSensors} live",
                    code = "SNS",
                    accent = if (sensors.status.name == "Ok") DeckGreen else DeckBlue,
                )
                DeckSummaryCard(
                    label = "GPU",
                    value = gpu.status.name.uppercase(),
                    modifier = Modifier.weight(1f),
                    supporting = "${gpu.readableSources.size} readable KGSL sources",
                    code = "GPU",
                    accent = if (gpu.status.name == "Ok") DeckAmber else DeckBlue,
                )
                DeckSummaryCard(
                    label = "Access",
                    value = "NO ROOT",
                    modifier = Modifier.weight(1f),
                    supporting = "Stock Android · device-local only",
                    code = "SEC",
                    accent = DeckGreen,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                DeckPanel(
                    title = "CPU & memory",
                    subtitle = "High-frequency foreground collectors.",
                    code = "SYS",
                    modifier = Modifier.weight(1f),
                    accent = DeckBlue,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        DeckDataRow("CPU freq", "scaling_cur_freq readable", "$readableFrequency / ${cpu.cores.size}", accent = DeckBlue)
                        DeckDataRow("Governor", "scaling_governor readable", "$readableGovernor / ${cpu.cores.size}", accent = DeckBlue)
                        DeckDataRow("MemTotal", "Physical RAM", if (memory.totalKiB > 0) "READABLE" else "UNAVAILABLE", accent = DeckCyan)
                        DeckDataRow("MemAvailable", "Reclaim-aware availability", if (memory.availableKiB > 0) "READABLE" else "UNAVAILABLE", accent = DeckCyan)
                        DeckDataRow("Cadence", "CPU + memory", "1 s", accent = DeckGreen)
                    }
                }

                DeckPanel(
                    title = "Battery & thermal",
                    subtitle = "Lower-cadence power and temperature collectors.",
                    code = "PWR",
                    modifier = Modifier.weight(1f),
                    accent = DeckCyan,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        DeckDataRow("Battery", "ACTION_BATTERY_CHANGED", battery.levelPercent?.let { "READABLE" } ?: "UNAVAILABLE", accent = DeckGreen)
                        DeckDataRow("Current", "BATTERY_PROPERTY_CURRENT_NOW", if (battery.currentNowMicroAmps != null) "READABLE" else "UNAVAILABLE", accent = DeckBlue)
                        DeckDataRow("Cycles", "Android 14 EXTRA_CYCLE_COUNT", if (battery.cycleCount != null) "READABLE" else "UNAVAILABLE", accent = DeckBlue)
                        DeckDataRow("Thermal", "PowerManager status", thermal.systemStatus, accent = DeckGreen)
                        DeckDataRow("Zones", "/sys/class/thermal canonical set", thermal.sensors.size.toString(), accent = DeckCyan)
                        DeckDataRow("Cadence", "Battery + thermal", "5 s", accent = DeckGreen)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                DeckPanel(
                    title = "Network",
                    subtitle = "Android network state plus device TrafficStats.",
                    code = "NET",
                    modifier = Modifier.weight(1f),
                    accent = DeckBlue,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        DeckDataRow("Active", "ConnectivityManager", if (network.connected) "YES" else "NO", accent = DeckGreen)
                        DeckDataRow("Transport", "NetworkCapabilities", network.transport, accent = DeckBlue)
                        DeckDataRow("Interface", "LinkProperties", network.interfaceName ?: "UNAVAILABLE", accent = DeckBlue)
                        DeckDataRow("Traffic", "TrafficStats totals", if (network.totalRxBytes != null && network.totalTxBytes != null) "READABLE" else "UNAVAILABLE", accent = DeckCyan)
                        DeckDataRow("Cadence", "Network transfer sample", "1 s", accent = DeckGreen)
                    }
                }

                DeckPanel(
                    title = "Storage",
                    subtitle = "App-safe internal volume capacity via StatFs.",
                    code = "SSD",
                    modifier = Modifier.weight(1f),
                    accent = DeckCyan,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        DeckDataRow("Capacity", "Internal data filesystem", if (storage.totalBytes > 0L) "READABLE" else "UNAVAILABLE", accent = DeckGreen)
                        DeckDataRow("Shared", "External storage state", storage.sharedStorageState, accent = DeckBlue)
                        DeckDataRow("Emulated", "Android shared storage", if (storage.sharedStorageEmulated) "YES" else "NO", accent = DeckBlue)
                        DeckDataRow("Permission", "Broad file access", "NOT REQUIRED", accent = DeckGreen)
                        DeckDataRow("Cadence", "Storage capacity sample", "30 s", accent = DeckGreen)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                DeckPanel(
                    title = "Sensors",
                    subtitle = "SensorManager inventory and curated live channels.",
                    code = "SNS",
                    modifier = Modifier.weight(1f),
                    accent = DeckGreen,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        DeckDataRow("Inventory", "TYPE_ALL entries", sensors.totalSensors.toString(), accent = DeckBlue)
                        DeckDataRow("Live", "Registered low-risk channels", sensors.liveSensors.toString(), accent = DeckGreen)
                        DeckDataRow("Wake-up", "Wake-up capable sensors", sensors.wakeUpSensors.toString(), accent = DeckCyan)
                        DeckDataRow("Permission", "Step channels remain inventory-only", "NONE ADDED", accent = DeckGreen)
                        DeckDataRow("Cadence", "Published snapshot", "1 s", accent = DeckGreen)
                    }
                }

                DeckPanel(
                    title = "GPU",
                    subtitle = "Vendor-specific Qualcomm KGSL capability probe.",
                    code = "GPU",
                    modifier = Modifier.weight(1f),
                    accent = DeckAmber,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        DeckDataRow("KGSL", "/sys/class/kgsl/kgsl-3d0", if (gpu.status.name != "Unavailable") "PRESENT" else "UNAVAILABLE", accent = DeckAmber)
                        DeckDataRow("Clock", "Current frequency", if (gpu.currentHz != null) "READABLE" else "UNAVAILABLE", accent = DeckBlue)
                        DeckDataRow("Ceiling", "Maximum frequency", if (gpu.maximumHz != null) "READABLE" else "UNAVAILABLE", accent = DeckBlue)
                        DeckDataRow("gpubusy", "Experimental busy ratio", if (gpu.busyPercent != null) "READABLE" else "UNAVAILABLE", accent = DeckAmber)
                        DeckDataRow("Cadence", "GPU probe", "1 s", accent = DeckGreen)
                    }
                }
            }
        }
    }
}
