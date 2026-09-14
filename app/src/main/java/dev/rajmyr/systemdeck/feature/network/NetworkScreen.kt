package dev.rajmyr.systemdeck.feature.network

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
import dev.rajmyr.systemdeck.data.network.NetworkSnapshot
import dev.rajmyr.systemdeck.ui.components.CollectorBadge
import dev.rajmyr.systemdeck.ui.components.DeckDataRow
import dev.rajmyr.systemdeck.ui.components.DeckInfoBox
import dev.rajmyr.systemdeck.ui.components.DeckPageHeader
import dev.rajmyr.systemdeck.ui.components.DeckSectionLabel
import dev.rajmyr.systemdeck.ui.components.DeckPanel
import dev.rajmyr.systemdeck.ui.components.DeckSummaryCard
import dev.rajmyr.systemdeck.ui.components.DeckSummaryGrid
import dev.rajmyr.systemdeck.ui.components.DeckSummarySpec
import dev.rajmyr.systemdeck.ui.components.KeyValueRow
import dev.rajmyr.systemdeck.ui.theme.DeckBlue
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import dev.rajmyr.systemdeck.ui.theme.DeckGreen
import dev.rajmyr.systemdeck.ui.theme.DeckMuted
import dev.rajmyr.systemdeck.ui.theme.DeckText

@Composable
fun NetworkScreen(
    network: NetworkSnapshot,
    compact: Boolean,
) {
    val gap = if (compact) 9.dp else 12.dp
    val validation = when (network.validated) {
        true -> "Validated"
        false -> if (network.connected) "Not validated" else "Offline"
        null -> "Unknown"
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(gap)) {
        item {
            DeckPageHeader(
                title = "Network",
                subtitle = "Connection, traffic and link state.",
                trailing = if (network.connected) "Live · 1 s" else "Offline",
            )
        }


        item {
            DeckSummaryGrid(
                items = listOf(
                    DeckSummarySpec(
                        label = "Network",
                        value = network.transport,
                        supporting = network.interfaceName ?: "No active interface",
                        code = "NET",
                        accent = DeckBlue,
                    ),
                    DeckSummarySpec(
                        label = "Download",
                        value = DeckFormat.rate(network.rxBytesPerSecond),
                        supporting = "Device traffic delta",
                        code = "RX",
                        accent = DeckCyan,
                    ),
                    DeckSummarySpec(
                        label = "Upload",
                        value = DeckFormat.rate(network.txBytesPerSecond),
                        supporting = "Device traffic delta",
                        code = "TX",
                        accent = DeckBlue,
                    ),
                    DeckSummarySpec(
                        label = "Internet",
                        value = validation,
                        supporting = when (network.metered) {
                            true -> "Metered network"
                            false -> "Unmetered network"
                            null -> "Metering unknown"
                        },
                        code = "WAN",
                        accent = if (network.validated == true) DeckGreen else DeckBlue,
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
                    title = "Live transfer",
                    subtitle = "Current device traffic.",
                    trailing = "1 s",
                    code = "I/O",
                    accent = DeckCyan,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        DeckDataRow(
                            label = "Down",
                            description = "Bytes received during latest sample",
                            value = DeckFormat.rate(network.rxBytesPerSecond),
                            accent = DeckCyan,
                        )
                        DeckDataRow(
                            label = "Up",
                            description = "Bytes transmitted during latest sample",
                            value = DeckFormat.rate(network.txBytesPerSecond),
                            accent = DeckBlue,
                        )
                        DeckDataRow(
                            label = "RX total",
                            description = "Device traffic since boot",
                            value = DeckFormat.bytes(network.totalRxBytes),
                            accent = DeckCyan,
                        )
                        DeckDataRow(
                            label = "TX total",
                            description = "Device traffic since boot",
                            value = DeckFormat.bytes(network.totalTxBytes),
                            accent = DeckBlue,
                        )
                    }
                }

                DeckPanel(
                    title = "Connection",
                    subtitle = "Active Android network.",
                    trailing = network.transport,
                    code = "LAN",
                    accent = DeckBlue,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        KeyValueRow("Interface", network.interfaceName ?: "—")
                        KeyValueRow("Validated", validation)
                        KeyValueRow(
                            "Metering",
                            when (network.metered) {
                                true -> "Metered"
                                false -> "Unmetered"
                                null -> "Unknown"
                            },
                        )
                        KeyValueRow("IPv4", network.ipv4.firstOrNull() ?: "—")
                        KeyValueRow("IPv6", network.ipv6.firstOrNull() ?: "—")
                        KeyValueRow("DNS", network.dnsServers.take(2).joinToString(" · ").ifBlank { "—" })
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                DeckPanel(
                    title = "Link capacity",
                    subtitle = "Android link estimate.",
                    code = "CAP",
                    accent = DeckBlue,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        DeckDataRow(
                            label = "Downlink",
                            description = "NetworkCapabilities estimate",
                            value = DeckFormat.bandwidthKbps(network.estimatedDownKbps),
                            accent = DeckCyan,
                        )
                        DeckDataRow(
                            label = "Uplink",
                            description = "NetworkCapabilities estimate",
                            value = DeckFormat.bandwidthKbps(network.estimatedUpKbps),
                            accent = DeckBlue,
                        )
                    }
                }

                DeckPanel(
                    title = "Status",
                    accent = DeckGreen,
                    modifier = Modifier.weight(1f),
                ) {
                    CollectorBadge(network.status)
                }
            }
        }
    }
}
