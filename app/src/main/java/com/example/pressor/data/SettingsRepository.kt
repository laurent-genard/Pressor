package com.example.pressor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class PressorSettings(
    val holdDuration: Long = 1000L,
    val breakDuration: Long = 500L,
    val runLimit: Long = 0L,
    val targetX: Int = 0,
    val targetY: Int = 0
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.dataStore)

    private object PreferencesKeys {
        val HOLD_DURATION = longPreferencesKey("hold_duration")
        val BREAK_DURATION = longPreferencesKey("break_duration")
        val RUN_LIMIT = longPreferencesKey("run_limit")
        val TARGET_X = intPreferencesKey("target_x")
        val TARGET_Y = intPreferencesKey("target_y")
    }

    val settingsFlow: Flow<PressorSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            PressorSettings(
                holdDuration = preferences[PreferencesKeys.HOLD_DURATION] ?: 1000L,
                breakDuration = preferences[PreferencesKeys.BREAK_DURATION] ?: 500L,
                runLimit = preferences[PreferencesKeys.RUN_LIMIT] ?: 0L,
                targetX = preferences[PreferencesKeys.TARGET_X] ?: 0,
                targetY = preferences[PreferencesKeys.TARGET_Y] ?: 0
            )
        }

    suspend fun updateSettings(settings: PressorSettings) {
        require(PressorSettingsInput.isValid(settings)) {
            "Settings contain values outside their supported ranges"
        }

        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HOLD_DURATION] = settings.holdDuration
            preferences[PreferencesKeys.BREAK_DURATION] = settings.breakDuration
            preferences[PreferencesKeys.RUN_LIMIT] = settings.runLimit
            preferences[PreferencesKeys.TARGET_X] = settings.targetX
            preferences[PreferencesKeys.TARGET_Y] = settings.targetY
        }
    }

    suspend fun updateHoldDuration(duration: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HOLD_DURATION] = duration
        }
    }

    suspend fun updateBreakDuration(duration: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BREAK_DURATION] = duration
        }
    }

    suspend fun updateRunLimit(limit: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.RUN_LIMIT] = limit
        }
    }

    suspend fun updateTargetPosition(x: Int, y: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TARGET_X] = x
            preferences[PreferencesKeys.TARGET_Y] = y
        }
    }
}
