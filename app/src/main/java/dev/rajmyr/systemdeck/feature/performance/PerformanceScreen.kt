package dev.rajmyr.systemdeck.feature.performance

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
import dev.rajmyr.systemdeck.data.battery.BatterySnapshot
import dev.rajmyr.systemdeck.data.cpu.CpuSnapshot
import dev.rajmyr.systemdeck.data.memory.MemorySnapshot
import dev.rajmyr.systemdeck.data.thermal.ThermalSnapshot
import dev.rajmyr.systemdeck.ui.components.DeckDataRow
import dev.rajmyr.systemdeck.ui.components.DeckInfoBox
import dev.rajmyr.systemdeck.ui.components.DeckPageHeader
import dev.rajmyr.systemdeck.ui.components.DeckSectionLabel
import dev.rajmyr.systemdeck.ui.components.DeckPanel
import dev.rajmyr.systemdeck.ui.components.DeckProgress
import dev.rajmyr.systemdeck.ui.components.DeckSummaryCard
import dev.rajmyr.systemdeck.ui.components.DeckSummaryGrid
import dev.rajmyr.systemdeck.ui.components.DeckSummarySpec
import dev.rajmyr.systemdeck.ui.theme.DeckBlue
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import dev.rajmyr.systemdeck.ui.theme.DeckFonts
import dev.rajmyr.systemdeck.ui.theme.DeckGreen
import dev.rajmyr.systemdeck.ui.theme.DeckMuted
import dev.rajmyr.systemdeck.ui.theme.DeckText

@Composable
fun PerformanceScreen(
    cpu: CpuSnapshot,
    memory: MemorySnapshot,
    battery: BatterySnapshot,
    thermal: ThermalSnapshot,
    compact: Boolean,
) {
    val gap = if (compact) 9.dp else 12.dp
    val fastestActive = cpu.clusters.maxByOrNull { it.currentMaxKHz ?: 0L }
    val usedPercent = DeckFormat.percent(memory.usedKiB, memory.totalKiB)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(gap)) {
        item {
            DeckPageHeader(
                title = "Performance",
                subtitle = "CPU, memory, power and thermal state.",
                trailing = "Local only",
            )
        }


        item {
            DeckSummaryGrid(
                items = listOf(
                    DeckSummarySpec(
                        label = "CPU clock",
                        value = DeckFormat.frequency(fastestActive?.currentMaxKHz),
                        supporting = "${fastestActive?.label?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "CPU"} · ceiling ${DeckFormat.frequency(fastestActive?.maxKHz)}",
                        code = "CPU",
                        accent = DeckBlue,
                    ),
                    DeckSummarySpec(
                        label = "Memory",
                        value = "$usedPercent%",
                        supporting = "${DeckFormat.kibToGiB(memory.usedKiB)} of ${DeckFormat.kibToGiB(memory.totalKiB)} physical RAM",
                        code = "MEM",
                        accent = DeckCyan,
                    ),
                    DeckSummarySpec(
                        label = "Thermal",
                        value = thermal.systemStatus,
                        supporting = "CPU ${DeckFormat.temperature(thermal.sensor("CPU")?.celsius)} · GPU ${DeckFormat.temperature(thermal.sensor("GPU")?.celsius)}",
                        code = "THM",
                        accent = DeckGreen,
                    ),
                ),
                gap = gap,
                minItemWidth = 220.dp,
                maxColumns = 3,
            )
        }

        item {
            DeckPanel(
                title = "CPU frequency",
                subtitle = "Current clock vs configured ceiling.",
                trailing = "Frequency · not utilization",
                code = "CPU",
                accent = DeckBlue,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (cpu.clusters.isEmpty()) {
                        Text(
                            cpu.message ?: "CPU frequency data is unavailable.",
                            color = DeckMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        cpu.clusters.forEach { cluster ->
                            val max = cluster.maxKHz ?: 0L
                            val current = cluster.currentMaxKHz ?: 0L
                            val ratio = if (max > 0) (current.toDouble() / max.toDouble()).coerceIn(0.0, 1.0) else 0.0
                            val accent = if (cluster.label == "PRIME") DeckBlue else DeckCyan

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Column(modifier = Modifier.weight(0.29f)) {
                                    Text(
                                        "${cluster.label.lowercase().replaceFirstChar { it.uppercase() }} cluster",
                                        color = DeckText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        cluster.cores.joinToString(" · ") { "CPU${it.index}" },
                                        color = DeckMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = DeckFonts.Mono,
                                    )
                                }
                                DeckProgress(
                                    fraction = ratio.toFloat(),
                                    modifier = Modifier.weight(0.51f),
                                    accent = accent,
                                    height = 5,
                                )
                                Column(
                                    modifier = Modifier.weight(0.20f),
                                    horizontalAlignment = Alignment.End,
                                ) {
                                    Text(
                                        DeckFormat.frequency(cluster.currentMaxKHz),
                                        color = DeckText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = DeckFonts.Mono,
                                    )
                                    Text(
                                        "of ${DeckFormat.frequency(cluster.maxKHz)}",
                                        color = DeckMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
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
                    title = "Memory footprint",
                    subtitle = "Physical RAM usage.",
                    trailing = "$usedPercent% used",
                    code = "MEM",
                    modifier = Modifier.weight(1.05f),
                    accent = DeckCyan,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DeckProgress(
                            fraction = if (memory.totalKiB > 0) memory.usedKiB.toFloat() / memory.totalKiB.toFloat() else 0f,
                            accent = DeckCyan,
                            height = 5,
                        )
                        DeckDataRow("Used", "MemTotal − MemAvailable", DeckFormat.kibToGiB(memory.usedKiB), valueColor = DeckText, accent = DeckCyan)
                        DeckDataRow("Available", "Apps + reclaimable cache", DeckFormat.kibToGiB(memory.availableKiB), accent = DeckBlue)
                        DeckDataRow("Cached", "Kernel page cache", DeckFormat.kibToGiB(memory.cachedKiB), accent = DeckBlue)
                        DeckDataRow("Swap", "Currently allocated", "${DeckFormat.kibToGiB(memory.swapUsedKiB)} / ${DeckFormat.kibToGiB(memory.swapTotalKiB)}", accent = DeckBlue)
                    }
                }

                DeckPanel(
                    title = "Power & thermal context",
                    subtitle = "Battery and thermal context.",
                    code = "CTX",
                    modifier = Modifier.weight(0.95f),
                    accent = DeckGreen,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        DeckDataRow("Battery", "${battery.chargeStatus} · ${battery.pluggedSource}", battery.levelPercent?.let { "$it%" } ?: "—", valueColor = DeckText, accent = DeckGreen)
                        DeckDataRow("Power", "Signed battery electrical flow", DeckFormat.power(battery.powerWatts), accent = DeckBlue)
                        DeckDataRow("Thermal", "PowerManager throttling state", thermal.systemStatus, valueColor = DeckText, accent = DeckGreen)
                        DeckDataRow("CPU temp", thermal.sensor("CPU")?.source ?: "Sensor unavailable", DeckFormat.temperature(thermal.sensor("CPU")?.celsius), accent = DeckBlue)
                        DeckDataRow("GPU temp", thermal.sensor("GPU")?.source ?: "Sensor unavailable", DeckFormat.temperature(thermal.sensor("GPU")?.celsius), accent = DeckCyan)
                    }
                }
            }
        }
    }
}
