package com.thelast.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class ThemeMode(val storageValue: String) {
    System("system"),
    Warm("warm"),
    Night("night");

    companion object {
        fun fromStorageValue(value: String?): ThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: System
        }
    }
}

class SettingsStore(private val context: Context) {
    val useDynamicColor: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[USE_DYNAMIC_COLOR] ?: true
    }

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { preferences ->
        ThemeMode.fromStorageValue(preferences[THEME_MODE])
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[USE_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.storageValue
        }
    }

    private companion object {
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
