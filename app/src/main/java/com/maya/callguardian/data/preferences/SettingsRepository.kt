package com.maya.callguardian.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "maya_security_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_LOCAL_ONLY_PROCESSING = booleanPreferencesKey("local_only_processing")
        val KEY_HISTORY_RETENTION = booleanPreferencesKey("history_retention_enabled")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_AUDIO_INSPECTION_ENABLED = booleanPreferencesKey("audio_inspection_enabled")
        val KEY_SENSITIVITY_THRESHOLD = intPreferencesKey("scam_sensitivity_threshold")
    }

    val isLocalOnlyProcessing: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_LOCAL_ONLY_PROCESSING] ?: true // Local by default for privacy!
    }

    val isHistoryRetentionEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_HISTORY_RETENTION] ?: true
    }

    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val isAudioInspectionEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUDIO_INSPECTION_ENABLED] ?: true
    }

    val sensitivityThreshold: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_SENSITIVITY_THRESHOLD] ?: 60
    }

    suspend fun setLocalOnlyProcessing(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LOCAL_ONLY_PROCESSING] = enabled }
    }

    suspend fun setHistoryRetentionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HISTORY_RETENTION] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setAudioInspectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUDIO_INSPECTION_ENABLED] = enabled }
    }

    suspend fun setSensitivityThreshold(threshold: Int) {
        context.dataStore.edit { it[KEY_SENSITIVITY_THRESHOLD] = threshold }
    }
}
