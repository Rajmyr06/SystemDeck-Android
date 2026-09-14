package dev.rajmyr.systemdeck.feature.battery

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
import dev.rajmyr.systemdeck.data.battery.BatterySnapshot
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
import dev.rajmyr.systemdeck.ui.theme.DeckBlue
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import dev.rajmyr.systemdeck.ui.theme.DeckGreen
import dev.rajmyr.systemdeck.ui.theme.DeckMuted
import dev.rajmyr.systemdeck.ui.theme.DeckText

@Composable
fun BatteryScreen(
    battery: BatterySnapshot,
    compact: Boolean,
) {
    val gap = if (compact) 9.dp else 12.dp
    val level = battery.levelPercent
    val currentAvailable = battery.currentNowMicroAmps != null

    LazyColumn(verticalArrangement = Arrangement.spacedBy(gap)) {
        item {
            DeckPageHeader(
                title = "Battery",
                subtitle = "Charge, power and battery state.",
                trailing = "Live · 5 s",
            )
        }


        item {
            DeckSummaryGrid(
                items = listOf(
                    DeckSummarySpec(
                        label = "Level",
                        value = level?.let { "$it%" } ?: "—",
                        supporting = "${battery.chargeStatus} · ${battery.pluggedSource}",
                        code = "BAT",
                        accent = DeckGreen,
                    ),
                    DeckSummarySpec(
                        label = "Current",
                        value = DeckFormat.power(battery.powerWatts),
                        supporting = if (currentAvailable) {
                            "${DeckFormat.current(battery.currentNowMicroAmps)} at ${DeckFormat.voltage(battery.voltageMilliVolts)}"
                        } else {
                            "Current is not exposed by this device"
                        },
                        code = "PWR",
                        accent = DeckBlue,
                    ),
                    DeckSummarySpec(
                        label = "Health",
                        value = battery.health,
                        supporting = "${DeckFormat.temperature(battery.temperatureCelsius)} · ${battery.cycleCount?.let { "$it cycles" } ?: "cycle count unavailable"}",
                        code = "HLT",
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
                    title = "Charge & electrical",
                    subtitle = "Level, voltage, current and power.",
                    trailing = "BatteryManager",
                    code = "BAT",
                    accent = DeckGreen,
                    modifier = Modifier.weight(1.08f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DeckProgress(
                            fraction = (level ?: 0) / 100f,
                            accent = DeckGreen,
                            height = 5,
                        )
                        DeckDataRow("Level", "Remaining charge", level?.let { "$it%" } ?: "—", valueColor = DeckText, accent = DeckGreen)
                        DeckDataRow("Voltage", "Battery terminal voltage", DeckFormat.voltage(battery.voltageMilliVolts), accent = DeckBlue)
                        DeckDataRow("Current", "Instantaneous signed battery current", DeckFormat.current(battery.currentNowMicroAmps), accent = DeckBlue)
                        DeckDataRow("Power", "Voltage × instantaneous current", DeckFormat.power(battery.powerWatts), accent = DeckCyan)
                        DeckDataRow("Charge", "Reported charge counter", DeckFormat.charge(battery.chargeCounterMicroAh), accent = DeckCyan)
                        DeckDataRow("Time to full", "Android estimate while charging", DeckFormat.duration(battery.chargeTimeRemainingMillis), accent = DeckCyan)
                    }
                }

                DeckPanel(
                    title = "Battery state",
                    subtitle = "Health, source and temperature.",
                    code = "STS",
                    accent = DeckCyan,
                    modifier = Modifier.weight(0.92f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckDataRow("Status", "Current charge state", battery.chargeStatus, valueColor = DeckText, accent = DeckGreen)
                        DeckDataRow("Source", "External power source", battery.pluggedSource, accent = DeckBlue)
                        DeckDataRow("Health", "Android health classification", battery.health, accent = DeckCyan)
                        DeckDataRow("Temperature", "Battery-reported temperature", DeckFormat.temperature(battery.temperatureCelsius), accent = DeckCyan)
                        DeckDataRow("Cycles", "Charge cycle count on Android 14+", battery.cycleCount?.toString() ?: "—", accent = DeckBlue)
                        DeckDataRow("Chemistry", "Battery technology", battery.technology ?: "—", accent = DeckBlue)
                        CollectorBadge(battery.status)
                        battery.message?.let {
                            Text(it, color = DeckMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
