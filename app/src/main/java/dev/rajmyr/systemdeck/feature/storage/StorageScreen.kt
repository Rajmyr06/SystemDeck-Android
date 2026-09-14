package dev.rajmyr.systemdeck.feature.storage

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
import dev.rajmyr.systemdeck.data.storage.StorageSnapshot
import dev.rajmyr.systemdeck.ui.components.CollectorBadge
import dev.rajmyr.systemdeck.ui.components.DeckDataRow
import dev.rajmyr.systemdeck.ui.components.DeckInfoBox
import dev.rajmyr.systemdeck.ui.components.DeckPageHeader
import dev.rajmyr.systemdeck.ui.components.DeckSectionLabel
import dev.rajmyr.systemdeck.ui.components.DeckPanel
import dev.rajmyr.systemdeck.ui.components.DeckProgress
import dev.rajmyr.systemdeck.ui.components.DeckSummaryCard
import dev.rajmyr.systemdeck.ui.components.DeckSummaryGrid
import dev.rajmyr.systemdeck.ui.components.DeckSummarySpec
import dev.rajmyr.systemdeck.ui.components.KeyValueRow
import dev.rajmyr.systemdeck.ui.theme.DeckBlue
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import dev.rajmyr.systemdeck.ui.theme.DeckGreen
import dev.rajmyr.systemdeck.ui.theme.DeckMuted

@Composable
fun StorageScreen(
    storage: StorageSnapshot,
    compact: Boolean,
) {
    val gap = if (compact) 9.dp else 12.dp
    val usageFraction = if (storage.totalBytes > 0L) {
        storage.usedBytes.toFloat() / storage.totalBytes.toFloat()
    } else 0f

    LazyColumn(verticalArrangement = Arrangement.spacedBy(gap)) {
        item {
            DeckPageHeader(
                title = "Storage",
                subtitle = "Writable internal storage.",
                trailing = "Live · 30 s",
            )
        }


        item {
            DeckSummaryGrid(
                items = listOf(
                    DeckSummarySpec(
                        label = "Used",
                        value = if (storage.totalBytes > 0L) "${storage.usagePercent}%" else "—",
                        supporting = DeckFormat.bytes(storage.usedBytes),
                        code = "USE",
                        accent = DeckBlue,
                    ),
                    DeckSummarySpec(
                        label = "Available",
                        value = DeckFormat.bytes(storage.availableBytes),
                        supporting = "Free for apps and user data",
                        code = "FREE",
                        accent = DeckGreen,
                    ),
                    DeckSummarySpec(
                        label = "Capacity",
                        value = DeckFormat.bytes(storage.totalBytes),
                        supporting = "Internal data filesystem",
                        code = "DISK",
                        accent = DeckCyan,
                    ),
                    DeckSummarySpec(
                        label = "Shared storage",
                        value = storage.sharedStorageState.replaceFirstChar { it.uppercase() },
                        supporting = if (storage.sharedStorageEmulated) "Emulated storage" else "Physical storage",
                        code = "MNT",
                        accent = DeckBlue,
                    ),
                ),
                gap = gap,
                minItemWidth = 220.dp,
                maxColumns = 4,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                DeckPanel(
                    title = "Internal storage",
                    subtitle = "Apps and user data.",
                    trailing = if (storage.totalBytes > 0L) "${storage.usagePercent}% used" else "Waiting for sample",
                    code = "SSD",
                    accent = DeckBlue,
                    modifier = Modifier.weight(1.15f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        DeckProgress(
                            fraction = usageFraction,
                            accent = DeckBlue,
                            height = 6,
                        )
                        DeckDataRow(
                            label = "Used",
                            description = "Allocated on the data filesystem",
                            value = DeckFormat.bytes(storage.usedBytes),
                            accent = DeckBlue,
                        )
                        DeckDataRow(
                            label = "Available",
                            description = "Space currently available",
                            value = DeckFormat.bytes(storage.availableBytes),
                            accent = DeckGreen,
                        )
                        DeckDataRow(
                            label = "Total",
                            description = "Filesystem capacity reported by StatFs",
                            value = DeckFormat.bytes(storage.totalBytes),
                            accent = DeckCyan,
                        )
                    }
                }

                DeckPanel(
                    title = "Volume context",
                    subtitle = "Volume details.",
                    code = "VOL",
                    accent = DeckCyan,
                    modifier = Modifier.weight(0.85f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        KeyValueRow("Source", "StatFs")
                        KeyValueRow("Shared state", storage.sharedStorageState)
                        KeyValueRow("Emulated", if (storage.sharedStorageEmulated) "Yes" else "No")
                        KeyValueRow("Removable", if (storage.sharedStorageRemovable) "Yes" else "No")
                        KeyValueRow("Cadence", "30 s")
                    }
                }
            }
        }
    }
}
