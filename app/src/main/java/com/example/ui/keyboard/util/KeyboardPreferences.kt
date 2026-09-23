package com.example.ui.keyboard.util

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.keyboard.model.KeyboardThemeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KeyboardPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("keypro_prefs", Context.MODE_PRIVATE)

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND_ENABLED, true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC_ENABLED, true))
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val _laptopBarVisible = MutableStateFlow(prefs.getBoolean(KEY_LAPTOP_BAR_VISIBLE, true))
    val laptopBarVisible: StateFlow<Boolean> = _laptopBarVisible.asStateFlow()

    private val _themeType = MutableStateFlow(
        try {
            KeyboardThemeType.valueOf(prefs.getString(KEY_THEME_TYPE, KeyboardThemeType.GBOARD_DARK.name) ?: KeyboardThemeType.GBOARD_DARK.name)
        } catch (_: Exception) {
            KeyboardThemeType.GBOARD_DARK
        }
    )
    val themeType: StateFlow<KeyboardThemeType> = _themeType.asStateFlow()

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun setHapticEnabled(enabled: Boolean) {
        _hapticEnabled.value = enabled
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
    }

    fun setLaptopBarVisible(visible: Boolean) {
        _laptopBarVisible.value = visible
        prefs.edit().putBoolean(KEY_LAPTOP_BAR_VISIBLE, visible).apply()
    }

    fun setThemeType(type: KeyboardThemeType) {
        _themeType.value = type
        prefs.edit().putString(KEY_THEME_TYPE, type.name).apply()
    }

    companion object {
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_LAPTOP_BAR_VISIBLE = "laptop_bar_visible"
        private const val KEY_THEME_TYPE = "theme_type"

        @Volatile
        private var instance: KeyboardPreferences? = null

        fun getInstance(context: Context): KeyboardPreferences {
            return instance ?: synchronized(this) {
                instance ?: KeyboardPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
