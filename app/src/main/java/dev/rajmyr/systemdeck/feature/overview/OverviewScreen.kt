package dev.rajmyr.systemdeck.feature.overview

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.rajmyr.systemdeck.data.device.DeviceSnapshot
import dev.rajmyr.systemdeck.ui.theme.DeckBorder
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import dev.rajmyr.systemdeck.ui.theme.DeckMuted
import dev.rajmyr.systemdeck.ui.theme.DeckSurface

@Composable
fun OverviewScreen(
    device: DeviceSnapshot,
    compact: Boolean,
) {
    val gap = if (compact) 10.dp else 14.dp

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Text(
            text = "OVERVIEW",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Phase 5 establishes the application shell. Telemetry collectors remain intentionally disconnected until Phase 6.",
            color = DeckMuted,
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            DeckPanel(
                title = "FOUNDATION",
                modifier = Modifier.weight(1f),
            ) {
                StatusLine("Compose UI", "READY")
                StatusLine("StateFlow", "READY")
                StatusLine("DataStore", "READY")
                StatusLine("Network permission", "NOT REQUESTED")
                StatusLine("Root requirement", "NONE")
            }

            DeckPanel(
                title = "DEVICE",
                modifier = Modifier.weight(1f),
            ) {
                DataLine("MODEL", "${device.manufacturer} ${device.model}")
                DataLine("DEVICE", device.device)
                DataLine("SOC", device.soc)
                DataLine("ANDROID", "${device.androidVersion} / API ${device.sdk}")
                DataLine("CPU", "${device.cpuCores} logical cores")
                DataLine("ABI", device.abi)
            }
        }

        DeckPanel(title = "NEXT / PHASE 6") {
            Text(
                text = "Wire real collectors for CPU frequency/topology, memory, battery, thermal, network, storage, device and sensors. Unsupported metrics must report capability state instead of fabricated values.",
                style = MaterialTheme.typography.bodyMedium,
                color = DeckMuted,
            )
        }
    }
}

@Composable
private fun DeckPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .border(1.dp, DeckBorder, RoundedCornerShape(6.dp))
            .padding(14.dp),
    ) {
        Text(
            text = title,
            color = DeckCyan,
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            content()
        }
    }
}

@Composable
private fun StatusLine(label: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            status,
            color = DeckCyan,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun DataLine(label: String, value: String) {
    Column {
        Text(
            label,
            color = DeckMuted,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
