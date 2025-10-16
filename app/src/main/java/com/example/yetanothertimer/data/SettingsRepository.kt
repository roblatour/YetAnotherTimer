package com.example.yetanothertimer.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "settings"

private val Context.dataStore by preferencesDataStore(DATASTORE_NAME)

data class StartDuration(val minutes: Int, val seconds: Int)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val minutes: Preferences.Key<Int> = intPreferencesKey("start_minutes")
        val seconds: Preferences.Key<Int> = intPreferencesKey("start_seconds")
        val chime: Preferences.Key<Boolean> = booleanPreferencesKey("chime_enabled")
        val keepScreenOn: Preferences.Key<Boolean> = booleanPreferencesKey("keep_screen_on")
        val helpIconVisible: Preferences.Key<Boolean> = booleanPreferencesKey("help_icon_visible")
    val languageIconVisible: Preferences.Key<Boolean> = booleanPreferencesKey("language_icon_visible")
        val countUpEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("count_up_enabled")
        val languageTag: Preferences.Key<String> = stringPreferencesKey("language_tag")
        val initialized: Preferences.Key<Boolean> = booleanPreferencesKey("initialized")
    }

    val startDurationFlow: Flow<StartDuration> = context.dataStore.data.map { prefs ->
        val m = prefs[Keys.minutes] ?: 2
        val s = (prefs[Keys.seconds] ?: 0).coerceIn(0, 59)
        StartDuration(minutes = m.coerceAtLeast(0), seconds = s)
    }

    val chimeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.chime] ?: true
    }

    val keepScreenOnFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.keepScreenOn] ?: false
    }

    val helpIconVisibleFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.helpIconVisible] ?: true
    }

    val languageIconVisibleFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.languageIconVisible] ?: true
    }

    val countUpEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        // Default to false (Count down)
        prefs[Keys.countUpEnabled] ?: false
    }

    // Language tag in BCP-47 format. On first run, pick best match for system locale.
    val languageTagFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val existing = prefs[Keys.languageTag]
        if (existing != null && existing.isNotBlank()) return@map existing
        // If no stored language, compute from system and do not persist here (read-only flow)
        val sys = Locale.getDefault()
        com.example.yetanothertimer.data.SupportedLanguages.bestMatchFor(sys)
    }

    suspend fun setStartDuration(minutes: Int, seconds: Int) {
        val m = minutes.coerceAtLeast(0)
        val s = seconds.coerceIn(0, 59)
        context.dataStore.edit { prefs ->
            prefs[Keys.minutes] = m
            prefs[Keys.seconds] = s
        }
    }

    suspend fun setChimeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.chime] = enabled
        }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.keepScreenOn] = enabled
        }
    }

    suspend fun setHelpIconVisible(visible: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.helpIconVisible] = visible
        }
    }

    suspend fun setLanguageIconVisible(visible: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.languageIconVisible] = visible
        }
    }

    suspend fun setCountUpEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.countUpEnabled] = enabled
        }
    }

    suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.languageTag] = tag
            prefs[Keys.initialized] = true
        }
    }
}