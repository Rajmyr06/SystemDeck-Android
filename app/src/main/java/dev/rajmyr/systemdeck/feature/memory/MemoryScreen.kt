package dev.rajmyr.systemdeck.feature.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rajmyr.systemdeck.core.format.DeckFormat
import dev.rajmyr.systemdeck.data.memory.MemorySnapshot
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
import dev.rajmyr.systemdeck.ui.theme.DeckGreen
import dev.rajmyr.systemdeck.ui.theme.DeckMuted
import dev.rajmyr.systemdeck.ui.theme.DeckText

@Composable
fun MemoryScreen(
    memory: MemorySnapshot,
    compact: Boolean,
) {
    val gap = if (compact) 9.dp else 12.dp
    val usedPercent = DeckFormat.percent(memory.usedKiB, memory.totalKiB)
    val swapPercent = DeckFormat.percent(memory.swapUsedKiB, memory.swapTotalKiB)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(gap)) {
        item {
            DeckPageHeader(
                title = "Memory",
                subtitle = "Physical memory and swap.",
                trailing = "Live · 1 s",
            )
        }


        item {
            DeckSummaryGrid(
                items = listOf(
                    DeckSummarySpec(
                        label = "Used",
                        value = DeckFormat.kibToGiB(memory.usedKiB),
                        supporting = "$usedPercent% of ${DeckFormat.kibToGiB(memory.totalKiB)}",
                        code = "RAM",
                        accent = DeckCyan,
                    ),
                    DeckSummarySpec(
                        label = "Available",
                        value = DeckFormat.kibToGiB(memory.availableKiB),
                        supporting = "Available to apps plus reclaimable cache",
                        code = "AVL",
                        accent = DeckBlue,
                    ),
                    DeckSummarySpec(
                        label = "Swap",
                        value = DeckFormat.kibToGiB(memory.swapUsedKiB),
                        supporting = "$swapPercent% of ${DeckFormat.kibToGiB(memory.swapTotalKiB)}",
                        code = "SWP",
                        accent = DeckBlue,
                    ),
                ),
                gap = gap,
                minItemWidth = 220.dp,
                maxColumns = 3,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                DeckPanel(
                    title = "Physical memory",
                    subtitle = "Used, available and cached memory.",
                    trailing = "$usedPercent% used",
                    code = "RAM",
                    modifier = Modifier.weight(1.15f),
                    accent = DeckCyan,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DeckProgress(
                            fraction = if (memory.totalKiB > 0) memory.usedKiB.toFloat() / memory.totalKiB.toFloat() else 0f,
                            accent = DeckCyan,
                            height = 5,
                        )
                        DeckDataRow("Total", "Physical RAM visible to Android", DeckFormat.kibToGiB(memory.totalKiB), accent = DeckCyan)
                        DeckDataRow("Used", "MemTotal − MemAvailable", DeckFormat.kibToGiB(memory.usedKiB), valueColor = DeckText, accent = DeckCyan)
                        DeckDataRow("Available", "Apps + reclaimable cache", DeckFormat.kibToGiB(memory.availableKiB), accent = DeckBlue)
                        DeckDataRow("Free", "Completely unused memory", DeckFormat.kibToGiB(memory.freeKiB), accent = DeckBlue)
                        DeckDataRow("Cached", "Kernel page cache", DeckFormat.kibToGiB(memory.cachedKiB), accent = DeckBlue)
                        DeckDataRow("Buffers", "Kernel buffer allocations", DeckFormat.kibToGiB(memory.buffersKiB), accent = DeckBlue)
                        DeckDataRow("Slab", "Kernel object cache", DeckFormat.kibToGiB(memory.slabKiB), accent = DeckBlue)
                    }
                }

                Column(
                    modifier = Modifier.weight(0.85f),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    DeckPanel(
                        title = "Swap",
                        subtitle = "Configured swap space.",
                        trailing = "$swapPercent% used",
                        code = "SWP",
                        accent = DeckBlue,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DeckProgress(
                                fraction = if (memory.swapTotalKiB > 0) memory.swapUsedKiB.toFloat() / memory.swapTotalKiB.toFloat() else 0f,
                                accent = DeckBlue,
                                height = 5,
                            )
                            DeckDataRow("Total", "Configured swap", DeckFormat.kibToGiB(memory.swapTotalKiB), accent = DeckBlue)
                            DeckDataRow("Used", "Currently allocated", DeckFormat.kibToGiB(memory.swapUsedKiB), valueColor = DeckText, accent = DeckBlue)
                            DeckDataRow("Free", "Remaining swap capacity", DeckFormat.kibToGiB(memory.swapFreeKiB), accent = DeckBlue)
                        }
                    }
                }
            }
        }

    }
}
