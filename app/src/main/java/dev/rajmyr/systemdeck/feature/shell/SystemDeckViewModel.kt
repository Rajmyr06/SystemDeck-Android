package dev.rajmyr.systemdeck.feature.shell

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.rajmyr.systemdeck.core.alerts.AlertEvaluator
import dev.rajmyr.systemdeck.core.alerts.AlertPreferences
import dev.rajmyr.systemdeck.core.alerts.BackgroundAlertScheduler
import dev.rajmyr.systemdeck.core.alerts.LocalNotificationPublisher
import dev.rajmyr.systemdeck.core.model.AppSection
import dev.rajmyr.systemdeck.core.preferences.AppPreferencesRepository
import dev.rajmyr.systemdeck.core.telemetry.CollectorHealth
import dev.rajmyr.systemdeck.core.telemetry.TelemetryHistory
import dev.rajmyr.systemdeck.core.telemetry.TelemetryHistoryAccumulator
import dev.rajmyr.systemdeck.core.telemetry.collectorHealth
import dev.rajmyr.systemdeck.data.battery.BatterySnapshot
import dev.rajmyr.systemdeck.data.battery.BatteryTelemetryCollector
import dev.rajmyr.systemdeck.data.cpu.CpuSnapshot
import dev.rajmyr.systemdeck.data.cpu.CpuTelemetryCollector
import dev.rajmyr.systemdeck.data.device.DeviceInfoProvider
import dev.rajmyr.systemdeck.data.gpu.GpuSnapshot
import dev.rajmyr.systemdeck.data.gpu.GpuTelemetryCollector
import dev.rajmyr.systemdeck.data.memory.MemorySnapshot
import dev.rajmyr.systemdeck.data.memory.MemoryTelemetryCollector
import dev.rajmyr.systemdeck.data.network.NetworkSnapshot
import dev.rajmyr.systemdeck.data.network.NetworkTelemetryCollector
import dev.rajmyr.systemdeck.data.sensors.SensorSnapshot
import dev.rajmyr.systemdeck.data.sensors.SensorTelemetryCollector
import dev.rajmyr.systemdeck.data.storage.StorageSnapshot
import dev.rajmyr.systemdeck.data.storage.StorageTelemetryCollector
import dev.rajmyr.systemdeck.data.thermal.ThermalSnapshot
import dev.rajmyr.systemdeck.data.thermal.ThermalTelemetryCollector
import dev.rajmyr.systemdeck.feature.widget.SystemDeckWidgetProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SystemDeckViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private data class CoreTelemetry(
        val cpu: CpuSnapshot,
        val memory: MemorySnapshot,
        val battery: BatterySnapshot,
        val thermal: ThermalSnapshot,
    )

    private data class IoTelemetry(
        val network: NetworkSnapshot,
        val storage: StorageSnapshot,
    )

    private data class HardwareTelemetry(
        val gpu: GpuSnapshot,
        val sensors: SensorSnapshot,
    )

    private data class LiveTelemetry(
        val core: CoreTelemetry,
        val io: IoTelemetry,
        val hardware: HardwareTelemetry,
    )

    private data class UiPreferences(
        val compactDensity: Boolean,
        val showStatusStrip: Boolean,
        val metricDescriptions: Boolean,
        val backgroundAlertsEnabled: Boolean,
        val alertPreferences: AlertPreferences,
    )

    private val appContext = application.applicationContext
    private val preferences = AppPreferencesRepository(appContext)
    private val device = DeviceInfoProvider().read()
    private val cpuCollector = CpuTelemetryCollector()
    private val memoryCollector = MemoryTelemetryCollector()
    private val batteryCollector = BatteryTelemetryCollector(appContext)
    private val thermalCollector = ThermalTelemetryCollector(appContext)
    private val networkCollector = NetworkTelemetryCollector(appContext)
    private val storageCollector = StorageTelemetryCollector(appContext)
    private val gpuCollector = GpuTelemetryCollector(appContext)
    private val sensorCollector = SensorTelemetryCollector(appContext)
    private val selectedSection = MutableStateFlow(AppSection.Overview)

    private val uiPreferences = combine(
        preferences.compactDensity,
        preferences.showStatusStrip,
        preferences.metricDescriptions,
        preferences.backgroundAlertsEnabled,
        preferences.alertPreferences,
    ) { compactDensity, showStatusStrip, metricDescriptions, backgroundAlertsEnabled, alertPreferences ->
        UiPreferences(
            compactDensity = compactDensity,
            showStatusStrip = showStatusStrip,
            metricDescriptions = metricDescriptions,
            backgroundAlertsEnabled = backgroundAlertsEnabled,
            alertPreferences = alertPreferences,
        )
    }

    private val historyAccumulator = TelemetryHistoryAccumulator()
    private val historyState = MutableStateFlow(TelemetryHistory())
    private val alertEvaluator = AlertEvaluator()
    private val notificationPublisher = LocalNotificationPublisher(appContext)

    private val coreTelemetry = combine(
        cpuCollector.observe().withRecovery(),
        memoryCollector.observe().withRecovery(),
        batteryCollector.observe().withRecovery(),
        thermalCollector.observe().withRecovery(),
    ) { cpu, memory, battery, thermal ->
        CoreTelemetry(cpu, memory, battery, thermal)
    }

    private val ioTelemetry = combine(
        networkCollector.observe().withRecovery(),
        storageCollector.observe().withRecovery(),
    ) { network, storage ->
        IoTelemetry(network, storage)
    }

    private val hardwareTelemetry = combine(
        gpuCollector.observe().withRecovery(),
        sensorCollector.observe().withRecovery(),
    ) { gpu, sensors ->
        HardwareTelemetry(gpu, sensors)
    }

    private val liveTelemetry = combine(
        coreTelemetry,
        ioTelemetry,
        hardwareTelemetry,
    ) { core, io, hardware ->
        LiveTelemetry(core, io, hardware)
    }.onEach { live ->
        historyState.value = historyAccumulator.update(
            cpuSnapshot = live.core.cpu,
            memorySnapshot = live.core.memory,
            batterySnapshot = live.core.battery,
            thermalSnapshot = live.core.thermal,
            networkSnapshot = live.io.network,
            gpuSnapshot = live.hardware.gpu,
            storageSnapshot = live.io.storage,
        )

        SystemDeckWidgetProvider.publish(
            context = appContext,
            cpu = live.core.cpu,
            memory = live.core.memory,
            battery = live.core.battery,
            thermal = live.core.thermal,
        )
    }

    private val baseUiState = combine(
        selectedSection,
        liveTelemetry,
        historyState,
        uiPreferences,
    ) { section, live, history, uiPreferences ->
        val now = System.currentTimeMillis()
        val alerts = alertEvaluator.evaluate(
            preferences = uiPreferences.alertPreferences,
            memory = live.core.memory,
            battery = live.core.battery,
            thermal = live.core.thermal,
            storage = live.io.storage,
        )

        SystemDeckUiState(
            selectedSection = section,
            compactDensity = uiPreferences.compactDensity,
            showStatusStrip = uiPreferences.showStatusStrip,
            metricDescriptions = uiPreferences.metricDescriptions,
            backgroundAlertsEnabled = uiPreferences.backgroundAlertsEnabled,
            device = device,
            cpu = live.core.cpu,
            memory = live.core.memory,
            battery = live.core.battery,
            thermal = live.core.thermal,
            network = live.io.network,
            storage = live.io.storage,
            gpu = live.hardware.gpu,
            sensors = live.hardware.sensors,
            history = history,
            collectorHealth = buildCollectorHealth(live, now),
            alertPreferences = uiPreferences.alertPreferences,
            activeAlerts = alerts,
        )
    }.onEach { state ->
        notificationPublisher.sync(
            alerts = state.activeAlerts,
            enabled = state.alertPreferences.enabled,
        )
    }

    init {
        viewModelScope.launch {
            combine(
                preferences.backgroundAlertsEnabled,
                preferences.alertPreferences,
            ) { backgroundEnabled, alertPreferences ->
                backgroundEnabled && alertPreferences.enabled
            }
                .distinctUntilChanged()
                .collect { enabled ->
                    BackgroundAlertScheduler.setEnabled(appContext, enabled)
                }
        }
    }

    val uiState = baseUiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = SystemDeckUiState(device = device),
    )

    fun selectSection(section: AppSection) {
        selectedSection.value = section
    }

    fun setCompactDensity(enabled: Boolean) = launchPreferenceUpdate {
        preferences.setCompactDensity(enabled)
    }

    fun setShowStatusStrip(enabled: Boolean) = launchPreferenceUpdate {
        preferences.setShowStatusStrip(enabled)
    }

    fun setMetricDescriptions(enabled: Boolean) = launchPreferenceUpdate {
        preferences.setMetricDescriptions(enabled)
    }

    fun setBackgroundAlertsEnabled(enabled: Boolean) = launchPreferenceUpdate {
        preferences.setBackgroundAlertsEnabled(enabled)
    }

    fun clearHistory() {
        historyState.value = historyAccumulator.clear()
    }

    fun setAlertPreferences(value: AlertPreferences) = launchPreferenceUpdate {
        preferences.setAlertPreferences(value)
    }

    fun setAlertsEnabled(enabled: Boolean) = launchPreferenceUpdate {
        preferences.setAlertsEnabled(enabled)
    }

    fun setMemoryRuleEnabled(enabled: Boolean) = launchPreferenceUpdate {
        preferences.setMemoryRuleEnabled(enabled)
    }

    fun setMemoryHighPercent(value: Int) = launchPreferenceUpdate {
        preferences.setMemoryHighPercent(value)
    }

    fun setBatteryRuleEnabled(enabled: Boolean) = launchPreferenceUpdate {
        preferences.setBatteryRuleEnabled(enabled)
    }

    fun setBatteryLowPercent(value: Int) = launchPreferenceUpdate {
        preferences.setBatteryLowPercent(value)
    }

    fun setThermalRuleEnabled(enabled: Boolean) = launchPreferenceUpdate {
        preferences.setThermalRuleEnabled(enabled)
    }

    fun setThermalStatusThreshold(value: Int) = launchPreferenceUpdate {
        preferences.setThermalStatusThreshold(value)
    }

    fun setStorageRuleEnabled(enabled: Boolean) = launchPreferenceUpdate {
        preferences.setStorageRuleEnabled(enabled)
    }

    fun setStorageHighPercent(value: Int) = launchPreferenceUpdate {
        preferences.setStorageHighPercent(value)
    }

    private fun launchPreferenceUpdate(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun buildCollectorHealth(live: LiveTelemetry, nowMillis: Long): List<CollectorHealth> = listOf(
        collectorHealth("cpu", "CPU", live.core.cpu.status, live.core.cpu.sampledAtMillis, 1_000L, nowMillis, live.core.cpu.message),
        collectorHealth("memory", "Memory", live.core.memory.status, live.core.memory.sampledAtMillis, 1_000L, nowMillis, live.core.memory.message),
        collectorHealth("network", "Network", live.io.network.status, live.io.network.sampledAtMillis, 1_000L, nowMillis, live.io.network.message),
        collectorHealth("gpu", "GPU", live.hardware.gpu.status, live.hardware.gpu.sampledAtMillis, 1_000L, nowMillis, live.hardware.gpu.message),
        collectorHealth("sensors", "Sensors", live.hardware.sensors.status, live.hardware.sensors.sampledAtMillis, 1_000L, nowMillis, live.hardware.sensors.message),
        collectorHealth("battery", "Battery", live.core.battery.status, live.core.battery.sampledAtMillis, 5_000L, nowMillis, live.core.battery.message),
        collectorHealth("thermal", "Thermal", live.core.thermal.status, live.core.thermal.sampledAtMillis, 5_000L, nowMillis, live.core.thermal.message),
        collectorHealth("storage", "Storage", live.io.storage.status, live.io.storage.sampledAtMillis, 30_000L, nowMillis, live.io.storage.message),
    )

    private fun <T> Flow<T>.withRecovery(): Flow<T> = retryWhen { _, attempt ->
        val backoff = (1_000L * (attempt + 1L)).coerceAtMost(10_000L)
        delay(backoff)
        true
    }
}
