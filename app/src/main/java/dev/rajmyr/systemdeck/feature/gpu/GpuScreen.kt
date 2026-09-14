package dev.rajmyr.systemdeck.feature.gpu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rajmyr.systemdeck.data.gpu.GpuSnapshot
import dev.rajmyr.systemdeck.ui.components.DeckDataRow
import dev.rajmyr.systemdeck.ui.components.DeckPageHeader
import dev.rajmyr.systemdeck.ui.components.DeckPanel
import dev.rajmyr.systemdeck.ui.components.DeckProgress
import dev.rajmyr.systemdeck.ui.components.DeckSummaryGrid
import dev.rajmyr.systemdeck.ui.components.DeckSummarySpec
import dev.rajmyr.systemdeck.ui.theme.DeckAmber
import dev.rajmyr.systemdeck.ui.theme.DeckBlue
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import java.util.Locale

@Composable
fun GpuScreen(
    gpu: GpuSnapshot,
    compact: Boolean,
) {
    val gap = if (compact) 9.dp else 12.dp
    val busy = gpu.busyPercent
    val hasClock = listOf(gpu.currentHz, gpu.minimumHz, gpu.maximumHz).any { (it ?: 0L) > 0L }
    val hasClockProgress = (gpu.currentHz ?: 0L) > 0L && (gpu.maximumHz ?: 0L) > 0L
    val hasBusy = busy != null && busy.isFinite()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(gap)) {
        item {
            DeckPageHeader(
                title = "GPU",
                subtitle = "",
                trailing = "Experimental",
            )
        }

        item {
            DeckSummaryGrid(
                items = listOf(
                    DeckSummarySpec(
                        label = "GPU",
                        value = gpu.deviceLabel,
                        accent = DeckBlue,
                    ),
                    DeckSummarySpec(
                        label = "Clock",
                        value = formatHz(gpu.currentHz),
                        accent = DeckCyan,
                    ),
                    DeckSummarySpec(
                        label = "Max clock",
                        value = formatHz(gpu.maximumHz),
                        accent = DeckBlue,
                    ),
                    DeckSummarySpec(
                        label = "Busy",
                        value = busy?.let { String.format(Locale.US, "%.1f%%", it) } ?: "—",
                        accent = DeckAmber,
                    ),
                ),
                gap = gap,
                minItemWidth = 220.dp,
                maxColumns = 4,
            )
        }

        if (hasClock || hasBusy) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    if (hasClock) {
                        DeckPanel(
                            title = "Clock",
                            accent = DeckCyan,
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                if (hasClockProgress) {
                                    DeckProgress(
                                        fraction = (gpu.currentHz!!.toFloat() / gpu.maximumHz!!.toFloat())
                                            .coerceIn(0f, 1f),
                                        accent = DeckCyan,
                                        height = 5,
                                    )
                                }
                                DeckDataRow("Current", "", formatHz(gpu.currentHz), accent = DeckCyan)
                                DeckDataRow("Minimum", "", formatHz(gpu.minimumHz), accent = DeckBlue)
                                DeckDataRow("Maximum", "", formatHz(gpu.maximumHz), accent = DeckBlue)
                            }
                        }
                    }

                    if (hasBusy) {
                        DeckPanel(
                            title = "Busy",
                            trailing = "Experimental",
                            accent = DeckAmber,
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                DeckProgress(
                                    fraction = (busy!! / 100.0).toFloat().coerceIn(0f, 1f),
                                    accent = DeckAmber,
                                    height = 5,
                                )
                                DeckDataRow(
                                    label = "Busy",
                                    description = "",
                                    value = String.format(Locale.US, "%.1f%%", busy!!),
                                    accent = DeckAmber,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatHz(hz: Long?): String {
    if (hz == null || hz <= 0L) return "—"
    return if (hz >= 1_000_000_000L) {
        String.format(Locale.US, "%.2f GHz", hz / 1_000_000_000.0)
    } else {
        String.format(Locale.US, "%.0f MHz", hz / 1_000_000.0)
    }
}
