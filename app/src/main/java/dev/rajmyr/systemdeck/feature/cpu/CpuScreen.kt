package dev.rajmyr.systemdeck.feature.cpu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rajmyr.systemdeck.core.format.DeckFormat
import dev.rajmyr.systemdeck.data.cpu.CpuCoreSnapshot
import dev.rajmyr.systemdeck.data.cpu.CpuSnapshot
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
fun CpuScreen(
    cpu: CpuSnapshot,
    compact: Boolean,
) {
    val gap = if (compact) 9.dp else 12.dp
    val onlineCount = cpu.cores.count { it.online }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(gap)) {
        item {
            DeckPageHeader(
                title = "CPU",
                subtitle = "Core topology and live clock frequency.",
                trailing = "Live · 1 s",
            )
        }


        item {
            DeckSummaryGrid(
                items = listOf(
                    DeckSummarySpec(
                        label = "Logical cores",
                        value = cpu.cores.size.toString(),
                        supporting = "$onlineCount online",
                        code = "CPU",
                        accent = DeckBlue,
                    ),
                    DeckSummarySpec(
                        label = "Clusters",
                        value = "${cpu.clusters.size} clusters",
                        supporting = cpu.clusters.joinToString(" + ") { it.cores.size.toString() } + " · " +
                            cpu.clusters.joinToString(" · ") { it.label.lowercase() },
                        code = "TOP",
                        accent = DeckCyan,
                    ),
                    DeckSummarySpec(
                        label = "Collector",
                        value = if (cpu.status.name == "Ok") "Active" else cpu.status.name,
                        supporting = "Frequency data available without root",
                        code = "SYS",
                        accent = if (cpu.status.name == "Ok") DeckGreen else DeckBlue,
                    ),
                ),
                gap = gap,
                minItemWidth = 220.dp,
                maxColumns = 3,
            )
        }

        item {
            DeckPanel(
                title = "CPU clusters",
                subtitle = "Current clock vs configured maximum.",
                trailing = "Frequency · not load",
                code = "CPU",
                accent = DeckBlue,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (cpu.clusters.isEmpty()) {
                        Text(
                            text = cpu.message ?: "No readable CPU clusters.",
                            color = DeckMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        cpu.clusters.forEach { cluster ->
                            val current = cluster.currentMaxKHz
                            val max = cluster.maxKHz
                            val fraction = if (current != null && max != null && max > 0) {
                                current.toFloat() / max.toFloat()
                            } else 0f
                            val accent = if (cluster.label == "PRIME") DeckBlue else DeckCyan

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            "${cluster.label.lowercase().replaceFirstChar { it.uppercase() }} cluster",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = DeckText,
                                        )
                                        Text(
                                            cluster.cores.joinToString(" · ") { "CPU${it.index}" },
                                            color = DeckMuted,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = DeckFonts.Mono,
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            DeckFormat.frequency(current),
                                            color = DeckText,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = DeckFonts.Mono,
                                        )
                                        Text(
                                            "max ${DeckFormat.frequency(max)}",
                                            color = DeckMuted,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                                DeckProgress(
                                    fraction = fraction,
                                    accent = accent,
                                    height = 5,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            DeckPanel(
                title = "Per-core frequency",
                subtitle = "Clock, range, governor and online state.",
                trailing = "$onlineCount / ${cpu.cores.size} online",
                code = "CORE",
                accent = DeckCyan,
            ) {
                if (cpu.cores.isEmpty()) {
                    Text(
                        cpu.message ?: "Per-core frequency data is unavailable.",
                        color = DeckMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else if (compact) {
                    CpuCompactList(cpu.cores)
                } else {
                    CpuTable(cpu.cores)
                }
            }
        }
    }
}


@Composable
private fun CpuCompactList(cores: List<CpuCoreSnapshot>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cores.forEach { core ->
            DeckInfoBox(
                title = "CPU${core.index} · ${core.clusterLabel.lowercase().replaceFirstChar { it.uppercase() }}",
                message = buildString {
                    append("Current ${DeckFormat.frequency(core.currentKHz)}")
                    append(" · range ${DeckFormat.frequency(core.minKHz)} – ${DeckFormat.frequency(core.maxKHz)}")
                    append(" · ${core.governor ?: "governor unavailable"}")
                    append(if (core.online) " · online" else " · offline")
                },
                accent = if (core.clusterLabel == "PRIME") DeckBlue else DeckCyan,
            )
        }
    }
}

@Composable
private fun CpuTable(cores: List<CpuCoreSnapshot>) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        CpuTableHeader()
        cores.forEach { core -> CpuTableRow(core) }
    }
}

@Composable
private fun CpuTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TableCell("Core", 0.55f, DeckMuted)
        TableCell("Cluster", 1.0f, DeckMuted)
        TableCell("Current", 0.9f, DeckMuted)
        TableCell("Configured range", 1.45f, DeckMuted)
        TableCell("Governor", 0.9f, DeckMuted)
        TableCell("State", 0.72f, DeckMuted)
    }
}

@Composable
private fun CpuTableRow(core: CpuCoreSnapshot) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TableCell("CPU${core.index}", 0.55f, DeckText, mono = true)
        TableCell(core.clusterLabel.lowercase().replaceFirstChar { it.uppercase() }, 1.0f, if (core.clusterLabel == "PRIME") DeckBlue else DeckCyan)
        TableCell(DeckFormat.frequency(core.currentKHz), 0.9f, DeckText, mono = true)
        TableCell("${DeckFormat.frequency(core.minKHz)} – ${DeckFormat.frequency(core.maxKHz)}", 1.45f, DeckText, mono = true)
        TableCell(core.governor.orEmpty(), 0.9f, DeckMuted, mono = true)
        TableCell(if (core.online) "Online" else "Offline", 0.72f, if (core.online) DeckGreen else DeckMuted)
    }
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    color: Color,
    mono: Boolean = false,
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        color = color,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = if (mono) DeckFonts.Mono else DeckFonts.Ui,
        maxLines = 1,
    )
}
