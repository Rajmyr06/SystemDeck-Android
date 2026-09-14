package dev.rajmyr.systemdeck.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rajmyr.systemdeck.BuildConfig
import dev.rajmyr.systemdeck.ui.components.DeckPageHeader
import dev.rajmyr.systemdeck.ui.components.DeckPanel
import dev.rajmyr.systemdeck.ui.components.KeyValueRow
import dev.rajmyr.systemdeck.ui.components.LocalDeckMetricDescriptions
import dev.rajmyr.systemdeck.ui.theme.DeckBlue
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import dev.rajmyr.systemdeck.ui.theme.DeckGreen
import dev.rajmyr.systemdeck.ui.theme.DeckMuted
import dev.rajmyr.systemdeck.ui.theme.DeckText

@Composable
fun SettingsScreen(
    compactDensity: Boolean,
    showStatusStrip: Boolean,
    metricDescriptions: Boolean,
    backgroundAlertsEnabled: Boolean,
    onCompactDensityChanged: (Boolean) -> Unit,
    onShowStatusStripChanged: (Boolean) -> Unit,
    onMetricDescriptionsChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DeckPageHeader(
            title = "Settings",
            subtitle = "Local display preferences and telemetry policy.",
            trailing = "Local",
        )

        DeckPanel(
            title = "Interface",
            subtitle = "Change density without changing collector behavior.",
            accent = DeckBlue,
            code = "UI",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingToggleRow(
                    title = "Compact density",
                    supporting = "Reduces page padding and inter-card spacing on the tablet.",
                    checked = compactDensity,
                    onCheckedChange = onCompactDensityChanged,
                )
                SettingToggleRow(
                    title = "Status strip",
                    supporting = "Shows collector freshness plus key live values at the bottom.",
                    checked = showStatusStrip,
                    onCheckedChange = onShowStatusStripChanged,
                )
                SettingToggleRow(
                    title = "Metric explanations",
                    supporting = "Shows source/meaning text under telemetry labels. Disable for a denser view.",
                    checked = metricDescriptions,
                    onCheckedChange = onMetricDescriptionsChanged,
                )
            }
        }

        DeckPanel(
            title = "Sampling",
            subtitle = "Current foreground collector cadence. These values are intentionally fixed for this release.",
            accent = DeckCyan,
            code = "HZ",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                KeyValueRow("CPU / memory", "1 s")
                KeyValueRow("Network / GPU", "1 s")
                KeyValueRow("Sensors snapshot", "1 s")
                KeyValueRow("Battery / thermal", "5 s")
                KeyValueRow("Storage", "30 s")
                KeyValueRow("History retention", "15 min")
            }
        }

        DeckPanel(
            title = "Background work",
            subtitle = "SystemDeck avoids a permanent foreground service.",
            accent = DeckGreen,
            code = "BG",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                KeyValueRow("Widget snapshot", "WorkManager ≥15 min")
                KeyValueRow("Threshold checks", if (backgroundAlertsEnabled) "Enabled ≥15 min" else "Off")
                KeyValueRow("Persistent service", "None")
                if (LocalDeckMetricDescriptions.current) {
                    Text(
                        text = "Android and HyperOS may defer periodic background work. Background checks are best-effort, not real-time.",
                        color = DeckMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        DeckPanel(
            title = "Privacy",
            subtitle = "No account, analytics pipeline, remote database or root access.",
            accent = DeckBlue,
            code = "SEC",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                KeyValueRow("Build", BuildConfig.VERSION_NAME)
                KeyValueRow("Telemetry processing", "On device")
                KeyValueRow("Cloud upload", "None")
                KeyValueRow("Analytics", "None")
                KeyValueRow("Root", "Not required")
                KeyValueRow("Broad storage access", "Not required")
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = DeckText,
                style = MaterialTheme.typography.titleMedium,
            )
            if (LocalDeckMetricDescriptions.current) {
                Text(
                    text = supporting,
                    color = DeckMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
