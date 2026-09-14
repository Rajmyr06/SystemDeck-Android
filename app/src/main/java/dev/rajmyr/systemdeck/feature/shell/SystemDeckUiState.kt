package dev.rajmyr.systemdeck.feature.shell

import dev.rajmyr.systemdeck.core.model.AppSection
import dev.rajmyr.systemdeck.data.device.DeviceSnapshot

data class SystemDeckUiState(
    val selectedSection: AppSection = AppSection.Overview,
    val compactDensity: Boolean = false,
    val device: DeviceSnapshot,
)
