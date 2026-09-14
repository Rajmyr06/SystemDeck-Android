package dev.rajmyr.systemdeck.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.rajmyr.systemdeck.core.alerts.AlertPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.systemDeckDataStore by preferencesDataStore(name = "systemdeck_preferences")

class AppPreferencesRepository(
    private val context: Context,
) {
    private companion object {
        val CompactDensity = booleanPreferencesKey("compact_density")
        val ShowStatusStrip = booleanPreferencesKey("show_status_strip")
        val MetricDescriptions = booleanPreferencesKey("metric_descriptions_v2")
        val BackgroundAlertsEnabled = booleanPreferencesKey("background_alerts_enabled")

        val AlertsEnabled = booleanPreferencesKey("alerts_enabled")
        val MemoryRuleEnabled = booleanPreferencesKey("alert_memory_enabled")
        val MemoryHighPercent = intPreferencesKey("alert_memory_high_percent")
        val BatteryRuleEnabled = booleanPreferencesKey("alert_battery_enabled")
        val BatteryLowPercent = intPreferencesKey("alert_battery_low_percent")
        val ThermalRuleEnabled = booleanPreferencesKey("alert_thermal_enabled")
        val ThermalStatusThreshold = intPreferencesKey("alert_thermal_status_threshold")
        val StorageRuleEnabled = booleanPreferencesKey("alert_storage_enabled")
        val StorageHighPercent = intPreferencesKey("alert_storage_high_percent")
    }

    val compactDensity: Flow<Boolean> = context.systemDeckDataStore.data.map { preferences ->
        preferences[CompactDensity] ?: false
    }

    val showStatusStrip: Flow<Boolean> = context.systemDeckDataStore.data.map { preferences ->
        preferences[ShowStatusStrip] ?: true
    }

    val metricDescriptions: Flow<Boolean> = context.systemDeckDataStore.data.map { preferences ->
        preferences[MetricDescriptions] ?: false
    }

    val backgroundAlertsEnabled: Flow<Boolean> = context.systemDeckDataStore.data.map { preferences ->
        preferences[BackgroundAlertsEnabled] ?: false
    }

    val alertPreferences: Flow<AlertPreferences> = context.systemDeckDataStore.data.map { preferences ->
        AlertPreferences(
            enabled = preferences[AlertsEnabled] ?: false,
            memoryRuleEnabled = preferences[MemoryRuleEnabled] ?: true,
            memoryHighPercent = (preferences[MemoryHighPercent] ?: 85).coerceIn(70, 98),
            batteryRuleEnabled = preferences[BatteryRuleEnabled] ?: true,
            batteryLowPercent = (preferences[BatteryLowPercent] ?: 20).coerceIn(5, 50),
            thermalRuleEnabled = preferences[ThermalRuleEnabled] ?: true,
            thermalStatusThreshold = (preferences[ThermalStatusThreshold] ?: 2).coerceIn(1, 6),
            storageRuleEnabled = preferences[StorageRuleEnabled] ?: true,
            storageHighPercent = (preferences[StorageHighPercent] ?: 90).coerceIn(70, 99),
        )
    }

    suspend fun setCompactDensity(enabled: Boolean) {
        context.systemDeckDataStore.edit { preferences ->
            preferences[CompactDensity] = enabled
        }
    }

    suspend fun setShowStatusStrip(enabled: Boolean) {
        context.systemDeckDataStore.edit { preferences ->
            preferences[ShowStatusStrip] = enabled
        }
    }

    suspend fun setMetricDescriptions(enabled: Boolean) {
        context.systemDeckDataStore.edit { preferences ->
            preferences[MetricDescriptions] = enabled
        }
    }

    suspend fun setBackgroundAlertsEnabled(enabled: Boolean) {
        context.systemDeckDataStore.edit { preferences ->
            preferences[BackgroundAlertsEnabled] = enabled
        }
    }


    suspend fun setAlertPreferences(value: AlertPreferences) {
        context.systemDeckDataStore.edit { preferences ->
            preferences[AlertsEnabled] = value.enabled
            preferences[MemoryRuleEnabled] = value.memoryRuleEnabled
            preferences[MemoryHighPercent] = value.memoryHighPercent.coerceIn(70, 98)
            preferences[BatteryRuleEnabled] = value.batteryRuleEnabled
            preferences[BatteryLowPercent] = value.batteryLowPercent.coerceIn(5, 50)
            preferences[ThermalRuleEnabled] = value.thermalRuleEnabled
            preferences[ThermalStatusThreshold] = value.thermalStatusThreshold.coerceIn(1, 6)
            preferences[StorageRuleEnabled] = value.storageRuleEnabled
            preferences[StorageHighPercent] = value.storageHighPercent.coerceIn(70, 99)
        }
    }

    suspend fun setAlertsEnabled(enabled: Boolean) { context.systemDeckDataStore.edit { it[AlertsEnabled] = enabled } }
    suspend fun setMemoryRuleEnabled(enabled: Boolean) { context.systemDeckDataStore.edit { it[MemoryRuleEnabled] = enabled } }
    suspend fun setMemoryHighPercent(value: Int) { context.systemDeckDataStore.edit { it[MemoryHighPercent] = value.coerceIn(70, 98) } }
    suspend fun setBatteryRuleEnabled(enabled: Boolean) { context.systemDeckDataStore.edit { it[BatteryRuleEnabled] = enabled } }
    suspend fun setBatteryLowPercent(value: Int) { context.systemDeckDataStore.edit { it[BatteryLowPercent] = value.coerceIn(5, 50) } }
    suspend fun setThermalRuleEnabled(enabled: Boolean) { context.systemDeckDataStore.edit { it[ThermalRuleEnabled] = enabled } }
    suspend fun setThermalStatusThreshold(value: Int) { context.systemDeckDataStore.edit { it[ThermalStatusThreshold] = value.coerceIn(1, 6) } }
    suspend fun setStorageRuleEnabled(enabled: Boolean) { context.systemDeckDataStore.edit { it[StorageRuleEnabled] = enabled } }
    suspend fun setStorageHighPercent(value: Int) { context.systemDeckDataStore.edit { it[StorageHighPercent] = value.coerceIn(70, 99) } }

}
