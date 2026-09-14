package dev.rajmyr.systemdeck.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.systemDeckDataStore by preferencesDataStore(name = "systemdeck_preferences")

class AppPreferencesRepository(
    private val context: Context,
) {
    private companion object {
        val CompactDensity = booleanPreferencesKey("compact_density")
    }

    val compactDensity: Flow<Boolean> = context.systemDeckDataStore.data.map { preferences ->
        preferences[CompactDensity] ?: false
    }

    suspend fun setCompactDensity(enabled: Boolean) {
        context.systemDeckDataStore.edit { preferences ->
            preferences[CompactDensity] = enabled
        }
    }
}
