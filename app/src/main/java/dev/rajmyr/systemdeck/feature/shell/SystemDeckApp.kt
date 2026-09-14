package dev.rajmyr.systemdeck.feature.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.rajmyr.systemdeck.core.alerts.AlertPreferences
import dev.rajmyr.systemdeck.core.format.DeckFormat
import dev.rajmyr.systemdeck.core.model.AppSection
import dev.rajmyr.systemdeck.core.telemetry.SampleFreshness
import dev.rajmyr.systemdeck.feature.alerts.AlertsScreen
import dev.rajmyr.systemdeck.feature.battery.BatteryScreen
import dev.rajmyr.systemdeck.feature.cpu.CpuScreen
import dev.rajmyr.systemdeck.feature.diagnostics.DiagnosticsScreen
import dev.rajmyr.systemdeck.feature.gpu.GpuScreen
import dev.rajmyr.systemdeck.feature.sensors.SensorsScreen
import dev.rajmyr.systemdeck.feature.memory.MemoryScreen
import dev.rajmyr.systemdeck.feature.network.NetworkScreen
import dev.rajmyr.systemdeck.feature.overview.OverviewScreen
import dev.rajmyr.systemdeck.feature.performance.PerformanceScreen
import dev.rajmyr.systemdeck.feature.settings.SettingsScreen
import dev.rajmyr.systemdeck.feature.storage.StorageScreen
import dev.rajmyr.systemdeck.feature.thermal.ThermalScreen
import dev.rajmyr.systemdeck.ui.components.DeckInfoBox
import dev.rajmyr.systemdeck.ui.components.DeckPageHeader
import dev.rajmyr.systemdeck.ui.components.DeckPanel
import dev.rajmyr.systemdeck.ui.components.LocalDeckMetricDescriptions
import dev.rajmyr.systemdeck.ui.theme.DeckAccessibility
import dev.rajmyr.systemdeck.ui.theme.DeckBackground
import dev.rajmyr.systemdeck.ui.theme.DeckAmber
import dev.rajmyr.systemdeck.ui.theme.DeckBlue
import dev.rajmyr.systemdeck.ui.theme.DeckBorder
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import dev.rajmyr.systemdeck.ui.theme.DeckFonts
import dev.rajmyr.systemdeck.ui.theme.DeckGreen
import dev.rajmyr.systemdeck.ui.theme.DeckTertiary
import dev.rajmyr.systemdeck.ui.theme.DeckSurfaceSubtle
import dev.rajmyr.systemdeck.ui.theme.DeckLayout
import dev.rajmyr.systemdeck.ui.theme.DeckHairline
import dev.rajmyr.systemdeck.ui.theme.DeckMotion
import dev.rajmyr.systemdeck.ui.theme.DeckMuted
import dev.rajmyr.systemdeck.ui.theme.DeckSurface
import dev.rajmyr.systemdeck.ui.theme.DeckSurfaceElevated
import dev.rajmyr.systemdeck.ui.theme.DeckText

@Composable
fun SystemDeckApp(
    viewModel: SystemDeckViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DeckBackground,
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            val wide = maxWidth >= DeckLayout.CompactBreakpoint

            CompositionLocalProvider(
                LocalDeckMetricDescriptions provides state.metricDescriptions,
            ) {
                if (wide) {
                    WideShell(
                        state = state,
                        onSectionSelected = viewModel::selectSection,
                        onCompactDensityChanged = viewModel::setCompactDensity,
                        onShowStatusStripChanged = viewModel::setShowStatusStrip,
                        onMetricDescriptionsChanged = viewModel::setMetricDescriptions,
                        onAlertPreferencesChanged = viewModel::setAlertPreferences,
                        onBackgroundAlertsChanged = viewModel::setBackgroundAlertsEnabled,
                        onClearHistory = viewModel::clearHistory,
                    )
                } else {
                    CompactShell(
                        state = state,
                        onSectionSelected = viewModel::selectSection,
                        onCompactDensityChanged = viewModel::setCompactDensity,
                        onShowStatusStripChanged = viewModel::setShowStatusStrip,
                        onMetricDescriptionsChanged = viewModel::setMetricDescriptions,
                        onAlertPreferencesChanged = viewModel::setAlertPreferences,
                        onBackgroundAlertsChanged = viewModel::setBackgroundAlertsEnabled,
                        onClearHistory = viewModel::clearHistory,
                    )
                }
            }
        }
    }
}

@Composable
private fun WideShell(
    state: SystemDeckUiState,
    onSectionSelected: (AppSection) -> Unit,
    onCompactDensityChanged: (Boolean) -> Unit,
    onShowStatusStripChanged: (Boolean) -> Unit,
    onMetricDescriptionsChanged: (Boolean) -> Unit,
    onAlertPreferencesChanged: (AlertPreferences) -> Unit,
    onBackgroundAlertsChanged: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            selected = state.selectedSection,
            onSectionSelected = onSectionSelected,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Header(state)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (state.compactDensity) 16.dp else 22.dp,
                        vertical = if (state.compactDensity) 12.dp else 18.dp,
                    ),
            ) {
                Content(
                    state = state,
                    onCompactDensityChanged = onCompactDensityChanged,
                    onShowStatusStripChanged = onShowStatusStripChanged,
                    onMetricDescriptionsChanged = onMetricDescriptionsChanged,
                    onAlertPreferencesChanged = onAlertPreferencesChanged,
                    onBackgroundAlertsChanged = onBackgroundAlertsChanged,
                    onClearHistory = onClearHistory,
                    layoutCompact = false,
                )
            }
            if (state.showStatusStrip) StatusStrip(state)
        }
    }
}

@Composable
private fun CompactShell(
    state: SystemDeckUiState,
    onSectionSelected: (AppSection) -> Unit,
    onCompactDensityChanged: (Boolean) -> Unit,
    onShowStatusStripChanged: (Boolean) -> Unit,
    onMetricDescriptionsChanged: (Boolean) -> Unit,
    onAlertPreferencesChanged: (AlertPreferences) -> Unit,
    onBackgroundAlertsChanged: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Header(state)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items(AppSection.entries) { section ->
                SectionTab(
                    section = section,
                    selected = section == state.selectedSection,
                    onClick = { onSectionSelected(section) },
                    compact = true,
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
        ) {
            Content(
                state = state,
                onCompactDensityChanged = onCompactDensityChanged,
                onShowStatusStripChanged = onShowStatusStripChanged,
                onMetricDescriptionsChanged = onMetricDescriptionsChanged,
                onAlertPreferencesChanged = onAlertPreferencesChanged,
                onBackgroundAlertsChanged = onBackgroundAlertsChanged,
                onClearHistory = onClearHistory,
                layoutCompact = true,
            )
        }
        if (state.showStatusStrip) StatusStrip(state)
    }
}

@Composable
private fun Header(state: SystemDeckUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DeckLayout.TopBarHeight)
            .background(DeckBackground)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "${state.device.manufacturer} ${state.device.model}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = DeckFonts.Ui,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Android ${state.device.androidVersion} · API ${state.device.sdk}",
                color = DeckTertiary,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = DeckFonts.Ui,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(5.dp).background(DeckGreen, CircleShape))
            Text(
                text = "Local",
                color = DeckMuted,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = DeckFonts.Ui,
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DeckHairline),
    )
}

@Composable
private fun Sidebar(
    selected: AppSection,
    onSectionSelected: (AppSection) -> Unit,
) {
    val monitor = listOf(AppSection.Overview, AppSection.Performance, AppSection.Cpu, AppSection.Memory)
    val system = listOf(AppSection.Battery, AppSection.Thermal, AppSection.Network, AppSection.Storage)
    val hardware = listOf(AppSection.Gpu, AppSection.Sensors, AppSection.Device)
    val support = listOf(AppSection.Alerts, AppSection.Diagnostics, AppSection.Settings)

    Column(
        modifier = Modifier
            .width(DeckLayout.SidebarWidth)
            .fillMaxHeight()
            .background(DeckSurface)
            .padding(horizontal = 11.dp, vertical = 15.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "SYSTEMDECK",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DeckFonts.Ui,
            )
            Text(
                text = "ANDROID MONITOR",
                color = DeckTertiary,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = DeckFonts.Ui,
            )
        }

        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            SidebarGroup("Monitor", monitor, selected, onSectionSelected)
            Spacer(Modifier.height(DeckAccessibility.DenseControlGap))
            SidebarGroup("System", system, selected, onSectionSelected)
            Spacer(Modifier.height(DeckAccessibility.DenseControlGap))
            SidebarGroup("Hardware", hardware, selected, onSectionSelected)
            Spacer(Modifier.height(DeckAccessibility.DenseControlGap))
            SidebarGroup("App", support, selected, onSectionSelected)
            Spacer(Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DeckHairline),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(5.dp).background(DeckGreen, CircleShape))
            Text(
                "LOCAL · NO ROOT",
                color = DeckTertiary,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = DeckFonts.Ui,
            )
        }
    }

    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(DeckHairline),
    )
}

@Composable
private fun SidebarGroup(
    title: String,
    sections: List<AppSection>,
    selected: AppSection,
    onSectionSelected: (AppSection) -> Unit,
) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        color = DeckTertiary,
        style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.55.sp),
        fontFamily = DeckFonts.Ui,
    )
    Spacer(Modifier.height(4.dp))
    sections.forEach { section ->
        SectionTab(
            section = section,
            selected = section == selected,
            onClick = { onSectionSelected(section) },
            compact = false,
        )
    }
}

@Composable
private fun SectionTab(
    section: AppSection,
    selected: Boolean,
    onClick: () -> Unit,
    compact: Boolean,
) {
    val isExperimental = section.phase == "experimental"
    val isFuture = section.phase == "next" || section.phase == "later"
    val shape = RoundedCornerShape(8.dp)
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) DeckSurfaceElevated else Color.Transparent,
        animationSpec = tween(DeckMotion.StandardMillis),
        label = "sectionBackground",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) DeckBorder.copy(alpha = 0.42f) else Color.Transparent,
        animationSpec = tween(DeckMotion.StandardMillis),
        label = "sectionBorder",
    )
    val baseModifier = if (compact) {
        Modifier.widthIn(min = 88.dp)
    } else {
        Modifier.fillMaxWidth()
    }
    val navigationState = when {
        isExperimental -> "Experimental"
        isFuture -> if (section.phase == "next") "Coming soon" else "Planned for later"
        selected -> "Selected"
        else -> "Available"
    }

    Row(
        modifier = baseModifier
            .heightIn(min = DeckAccessibility.MinTouchTarget)
            .semantics {
                this.selected = selected
                stateDescription = navigationState
            }
            .background(backgroundColor, shape)
            .border(1.dp, borderColor, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (compact) 10.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(14.dp)
                    .background(
                        if (selected) DeckBlue else Color.Transparent,
                        RoundedCornerShape(1.dp),
                    ),
            )
            Text(
                text = section.label.uppercase(),
                fontFamily = DeckFonts.Ui,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    selected -> DeckText
                    isFuture -> DeckTertiary
                    else -> DeckMuted
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    letterSpacing = 0.35.sp,
                ),
            )
        }

        if (isExperimental) {
            Box(Modifier.size(5.dp).background(DeckAmber, CircleShape))
        }
    }
}

@Composable
private fun Content(
    state: SystemDeckUiState,
    onCompactDensityChanged: (Boolean) -> Unit,
    onShowStatusStripChanged: (Boolean) -> Unit,
    onMetricDescriptionsChanged: (Boolean) -> Unit,
    onAlertPreferencesChanged: (AlertPreferences) -> Unit,
    onBackgroundAlertsChanged: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    layoutCompact: Boolean,
) {
    val compactLayout = layoutCompact || state.compactDensity
    when (state.selectedSection) {
        AppSection.Overview -> OverviewScreen(
            device = state.device,
            cpu = state.cpu,
            memory = state.memory,
            battery = state.battery,
            thermal = state.thermal,
            network = state.network,
            storage = state.storage,
            gpu = state.gpu,
            sensors = state.sensors,
            history = state.history,
            compact = compactLayout,
        )

        AppSection.Performance -> PerformanceScreen(
            cpu = state.cpu,
            memory = state.memory,
            battery = state.battery,
            thermal = state.thermal,
            compact = compactLayout,
        )

        AppSection.Cpu -> CpuScreen(
            cpu = state.cpu,
            compact = compactLayout,
        )

        AppSection.Memory -> MemoryScreen(
            memory = state.memory,
            compact = compactLayout,
        )

        AppSection.Battery -> BatteryScreen(
            battery = state.battery,
            compact = compactLayout,
        )

        AppSection.Thermal -> ThermalScreen(
            thermal = state.thermal,
            compact = compactLayout,
        )

        AppSection.Network -> NetworkScreen(
            network = state.network,
            compact = compactLayout,
        )

        AppSection.Storage -> StorageScreen(
            storage = state.storage,
            compact = compactLayout,
        )

        AppSection.Gpu -> GpuScreen(
            gpu = state.gpu,
            compact = compactLayout,
        )

        AppSection.Sensors -> SensorsScreen(
            sensors = state.sensors,
            compact = compactLayout,
        )

        AppSection.Device -> DeviceScreen(state)
        AppSection.Alerts -> AlertsScreen(
            preferences = state.alertPreferences,
            activeAlerts = state.activeAlerts,
            backgroundAlertsEnabled = state.backgroundAlertsEnabled,
            onPreferencesChanged = onAlertPreferencesChanged,
            onBackgroundAlertsChanged = onBackgroundAlertsChanged,
        )
        AppSection.Diagnostics -> DiagnosticsScreen(
            device = state.device,
            cpu = state.cpu,
            memory = state.memory,
            battery = state.battery,
            thermal = state.thermal,
            network = state.network,
            storage = state.storage,
            gpu = state.gpu,
            sensors = state.sensors,
            history = state.history,
            collectorHealth = state.collectorHealth,
            onClearHistory = onClearHistory,
        )
        AppSection.Settings -> SettingsScreen(
            compactDensity = state.compactDensity,
            showStatusStrip = state.showStatusStrip,
            metricDescriptions = state.metricDescriptions,
            backgroundAlertsEnabled = state.backgroundAlertsEnabled,
            onCompactDensityChanged = onCompactDensityChanged,
            onShowStatusStripChanged = onShowStatusStripChanged,
            onMetricDescriptionsChanged = onMetricDescriptionsChanged,
        )

        else -> PendingCollectorScreen(state.selectedSection)
    }
}

@Composable
private fun DeviceScreen(state: SystemDeckUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DeckPageHeader(
            title = "Device",
            subtitle = "Hardware and Android identity.",
            trailing = "READ ONLY",
        )
        DeckPanel(
            title = "Device identity",
            subtitle = "System-reported values.",
            code = "DEV",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DeviceRow("Manufacturer", state.device.manufacturer)
                DeviceRow("Model", state.device.model)
                DeviceRow("Codename", state.device.device)
                DeviceRow("SoC", state.device.soc)
                DeviceRow("Android", "${state.device.androidVersion} / API ${state.device.sdk}")
                DeviceRow("Kernel", state.device.kernel)
                DeviceRow("ABI", state.device.abi)
                DeviceRow("Logical cores", state.device.cpuCores.toString())
            }
        }
    }
}

@Composable
private fun DeviceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = DeckMuted, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            color = DeckText,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = DeckFonts.Mono,
        )
    }
}

@Composable
private fun PendingCollectorScreen(section: AppSection) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DeckPageHeader(
            title = section.label.replaceFirstChar { it.uppercase() },
            subtitle = "Not available on this build.",
            trailing = "Pending",
        )
        DeckInfoBox(
            title = "Unavailable",
            message = "SystemDeck shows this page only after a real Android data source is validated.",
            accent = DeckBlue,
        )
    }
}

@Composable
private fun StatusStrip(state: SystemDeckUiState) {
    val fastest = state.cpu.clusters.maxByOrNull { it.currentMaxKHz ?: 0L }
    val memoryPercent = DeckFormat.percent(state.memory.usedKiB, state.memory.totalKiB)
    val staleCount = state.collectorHealth.count { it.freshness == SampleFreshness.Stale }
    val waitingCount = state.collectorHealth.count { it.freshness == SampleFreshness.Waiting }
    val healthColor = if (staleCount > 0) DeckAmber else DeckGreen
    val healthText = if (staleCount > 0) "$staleCount stale" else if (waitingCount > 0) "Starting" else "Live"

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DeckHairline),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(DeckLayout.StatusBarHeight)
                .background(DeckBackground)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(Modifier.size(5.dp).background(healthColor, CircleShape))
                Text(healthText, color = DeckMuted, style = MaterialTheme.typography.bodySmall)
            }
            val hasCpu = (fastest?.currentMaxKHz ?: 0L) > 0L
            val hasMemory = state.memory.totalKiB > 0L
            val hasBattery = state.battery.levelPercent != null
            val hasThermal = state.thermal.systemStatus.isNotBlank() &&
                !state.thermal.systemStatus.equals("Unavailable", ignoreCase = true) &&
                !state.thermal.systemStatus.equals("Waiting", ignoreCase = true)
            val networkParts = buildList {
                state.network.rxBytesPerSecond?.let { add("↓${DeckFormat.rateCompact(it)}") }
                state.network.txBytesPerSecond?.let { add("↑${DeckFormat.rateCompact(it)}") }
            }

            if (hasCpu || hasMemory || hasBattery || hasThermal || networkParts.isNotEmpty() || state.activeAlerts.isNotEmpty()) {
                StatusDivider()
            }
            if (hasCpu) StatusToken("CPU", DeckFormat.frequency(fastest?.currentMaxKHz))
            if (hasMemory) StatusToken("MEM", "$memoryPercent%")
            state.battery.levelPercent?.let { StatusToken("BAT", "$it%") }
            if (hasThermal) StatusToken("THM", state.thermal.systemStatus)
            if (networkParts.isNotEmpty()) StatusToken("NET", networkParts.joinToString(" "))
            if (state.activeAlerts.isNotEmpty()) {
                StatusToken("ALT", state.activeAlerts.size.toString())
            }
        }
    }
}

@Composable
private fun StatusToken(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = DeckTertiary,
            fontFamily = DeckFonts.Ui,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = value,
            color = DeckMuted,
            fontFamily = DeckFonts.Mono,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun StatusDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(12.dp)
            .background(DeckHairline),
    )
}

