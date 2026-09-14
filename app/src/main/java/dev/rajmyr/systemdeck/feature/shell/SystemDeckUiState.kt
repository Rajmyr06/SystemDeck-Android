package dev.rajmyr.systemdeck.feature.shell

import dev.rajmyr.systemdeck.core.alerts.AlertPreferences
import dev.rajmyr.systemdeck.core.alerts.SystemAlert
import dev.rajmyr.systemdeck.core.model.AppSection
import dev.rajmyr.systemdeck.core.telemetry.CollectorHealth
import dev.rajmyr.systemdeck.core.telemetry.TelemetryHistory
import dev.rajmyr.systemdeck.data.battery.BatterySnapshot
import dev.rajmyr.systemdeck.data.cpu.CpuSnapshot
import dev.rajmyr.systemdeck.data.device.DeviceSnapshot
import dev.rajmyr.systemdeck.data.gpu.GpuSnapshot
import dev.rajmyr.systemdeck.data.memory.MemorySnapshot
import dev.rajmyr.systemdeck.data.network.NetworkSnapshot
import dev.rajmyr.systemdeck.data.sensors.SensorSnapshot
import dev.rajmyr.systemdeck.data.storage.StorageSnapshot
import dev.rajmyr.systemdeck.data.thermal.ThermalSnapshot

data class SystemDeckUiState(
    val selectedSection: AppSection = AppSection.Overview,
    val compactDensity: Boolean = false,
    val showStatusStrip: Boolean = true,
    val metricDescriptions: Boolean = false,
    val backgroundAlertsEnabled: Boolean = false,
    val device: DeviceSnapshot,
    val cpu: CpuSnapshot = CpuSnapshot.Empty,
    val memory: MemorySnapshot = MemorySnapshot.Empty,
    val battery: BatterySnapshot = BatterySnapshot.Empty,
    val thermal: ThermalSnapshot = ThermalSnapshot.Empty,
    val network: NetworkSnapshot = NetworkSnapshot.Empty,
    val storage: StorageSnapshot = StorageSnapshot.Empty,
    val gpu: GpuSnapshot = GpuSnapshot.Empty,
    val sensors: SensorSnapshot = SensorSnapshot.Empty,
    val history: TelemetryHistory = TelemetryHistory(),
    val collectorHealth: List<CollectorHealth> = emptyList(),
    val alertPreferences: AlertPreferences = AlertPreferences(),
    val activeAlerts: List<SystemAlert> = emptyList(),
)
