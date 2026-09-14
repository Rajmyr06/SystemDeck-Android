package dev.rajmyr.systemdeck.feature.thermal

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
import dev.rajmyr.systemdeck.data.thermal.ThermalSnapshot
import dev.rajmyr.systemdeck.ui.components.CollectorBadge
import dev.rajmyr.systemdeck.ui.components.DeckDataRow
import dev.rajmyr.systemdeck.ui.components.DeckInfoBox
import dev.rajmyr.systemdeck.ui.components.DeckPageHeader
import dev.rajmyr.systemdeck.ui.components.DeckSectionLabel
import dev.rajmyr.systemdeck.ui.components.DeckPanel
import dev.rajmyr.systemdeck.ui.components.DeckSummaryCard
import dev.rajmyr.systemdeck.ui.components.DeckSummaryGrid
import dev.rajmyr.systemdeck.ui.components.DeckSummarySpec
import dev.rajmyr.systemdeck.ui.theme.DeckBlue
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import dev.rajmyr.systemdeck.ui.theme.DeckGreen
import dev.rajmyr.systemdeck.ui.theme.DeckMuted
import dev.rajmyr.systemdeck.ui.theme.DeckText

@Composable
fun ThermalScreen(
    thermal: ThermalSnapshot,
    compact: Boolean,
) {
    val gap = if (compact) 9.dp else 12.dp
    val cpu = thermal.sensor("CPU")
    val gpu = thermal.sensor("GPU")
    val battery = thermal.sensor("Battery")

    LazyColumn(verticalArrangement = Arrangement.spacedBy(gap)) {
        item {
            DeckPageHeader(
                title = "Thermal",
                subtitle = "Thermal status and sensor temperatures.",
                trailing = "Live · 5 s",
            )
        }


        item {
            DeckSummaryGrid(
                items = listOf(
                    DeckSummarySpec(
                        label = "Status",
                        value = thermal.systemStatus,
                        supporting = "PowerManager throttling status",
                        code = "SYS",
                        accent = DeckGreen,
                    ),
                    DeckSummarySpec(
                        label = "CPU",
                        value = DeckFormat.temperature(cpu?.celsius),
                        supporting = cpu?.source ?: "Sensor unavailable",
                        code = "CPU",
                        accent = DeckBlue,
                    ),
                    DeckSummarySpec(
                        label = "GPU",
                        value = DeckFormat.temperature(gpu?.celsius),
                        supporting = gpu?.source ?: "Sensor unavailable",
                        code = "GPU",
                        accent = DeckCyan,
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
                    title = "Key temperatures",
                    subtitle = "Selected readable thermal zones.",
                    trailing = "${thermal.sensors.size} signals",
                    code = "TMP",
                    accent = DeckCyan,
                    modifier = Modifier.weight(1.1f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (thermal.sensors.isEmpty()) {
                            Text(
                                thermal.message ?: "No readable thermal zones were found.",
                                color = DeckMuted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            thermal.sensors.forEach { sensor ->
                                DeckDataRow(
                                    label = sensor.label,
                                    description = sensor.source,
                                    value = DeckFormat.temperature(sensor.celsius),
                                    valueColor = DeckText,
                                    accent = when (sensor.label) {
                                        "CPU", "GPU" -> DeckBlue
                                        "Battery", "Charger" -> DeckCyan
                                        else -> DeckGreen
                                    },
                                )
                            }
                        }
                    }
                }

                DeckPanel(
                    title = "Thermal state",
                    subtitle = "Android throttling state.",
                    code = "THM",
                    accent = DeckBlue,
                    modifier = Modifier.weight(0.9f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckDataRow("Status", "Android PowerManager", thermal.systemStatus, valueColor = DeckText, accent = DeckGreen)
                        DeckDataRow("Headroom", "Thermal envelope index", DeckFormat.thermalHeadroom(thermal.headroom), accent = DeckBlue)
                        DeckDataRow("CPU", "Selected CPU zone", DeckFormat.temperature(cpu?.celsius), accent = DeckBlue)
                        DeckDataRow("GPU", "Selected GPU zone", DeckFormat.temperature(gpu?.celsius), accent = DeckCyan)
                        DeckDataRow("Battery", "Selected battery zone", DeckFormat.temperature(battery?.celsius), accent = DeckCyan)
                        CollectorBadge(thermal.status)
                        thermal.message?.let {
                            Text(it, color = DeckMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
