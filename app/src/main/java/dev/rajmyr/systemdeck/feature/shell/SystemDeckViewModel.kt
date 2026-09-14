package dev.rajmyr.systemdeck.feature.shell

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.rajmyr.systemdeck.core.model.AppSection
import dev.rajmyr.systemdeck.core.preferences.AppPreferencesRepository
import dev.rajmyr.systemdeck.data.device.DeviceInfoProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SystemDeckViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val preferences = AppPreferencesRepository(application.applicationContext)
    private val device = DeviceInfoProvider().read()
    private val selectedSection = MutableStateFlow(AppSection.Overview)

    val uiState = combine(
        selectedSection,
        preferences.compactDensity,
    ) { section, compact ->
        SystemDeckUiState(
            selectedSection = section,
            compactDensity = compact,
            device = device,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SystemDeckUiState(device = device),
    )

    fun selectSection(section: AppSection) {
        selectedSection.value = section
    }

    fun setCompactDensity(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setCompactDensity(enabled)
        }
    }
}
