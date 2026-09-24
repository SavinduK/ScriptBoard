package com.example.ui.keyboard.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.model.KeyboardThemeType
import com.example.ui.keyboard.model.KeyboardThemes
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

    private val _holdForSymbolsEnabled = MutableStateFlow(prefs.getBoolean(KEY_HOLD_FOR_SYMBOLS, true))
    val holdForSymbolsEnabled: StateFlow<Boolean> = _holdForSymbolsEnabled.asStateFlow()

    private val _themeType = MutableStateFlow(
        try {
            KeyboardThemeType.valueOf(prefs.getString(KEY_THEME_TYPE, KeyboardThemeType.GBOARD_DARK.name) ?: KeyboardThemeType.GBOARD_DARK.name)
        } catch (_: Exception) {
            KeyboardThemeType.GBOARD_DARK
        }
    )
    val themeType: StateFlow<KeyboardThemeType> = _themeType.asStateFlow()

    // Custom RGB theme colors
    private val _customBgColor = MutableStateFlow(prefs.getInt(KEY_CUSTOM_BG, 0xFF1B263B.toInt()))
    val customBgColor: StateFlow<Int> = _customBgColor.asStateFlow()

    private val _customKeyBgColor = MutableStateFlow(prefs.getInt(KEY_CUSTOM_KEY_BG, 0xFF2E3D59.toInt()))
    val customKeyBgColor: StateFlow<Int> = _customKeyBgColor.asStateFlow()

    private val _customTextColor = MutableStateFlow(prefs.getInt(KEY_CUSTOM_TEXT, 0xFFE0E1DD.toInt()))
    val customTextColor: StateFlow<Int> = _customTextColor.asStateFlow()

    private val _customAccentColor = MutableStateFlow(prefs.getInt(KEY_CUSTOM_ACCENT, 0xFF00B4D8.toInt()))
    val customAccentColor: StateFlow<Int> = _customAccentColor.asStateFlow()

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

    fun setHoldForSymbolsEnabled(enabled: Boolean) {
        _holdForSymbolsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_HOLD_FOR_SYMBOLS, enabled).apply()
    }

    fun setThemeType(type: KeyboardThemeType) {
        _themeType.value = type
        prefs.edit().putString(KEY_THEME_TYPE, type.name).apply()
    }

    fun setCustomThemeColors(bg: Int, keyBg: Int, text: Int, accent: Int) {
        _customBgColor.value = bg
        _customKeyBgColor.value = keyBg
        _customTextColor.value = text
        _customAccentColor.value = accent
        prefs.edit()
            .putInt(KEY_CUSTOM_BG, bg)
            .putInt(KEY_CUSTOM_KEY_BG, keyBg)
            .putInt(KEY_CUSTOM_TEXT, text)
            .putInt(KEY_CUSTOM_ACCENT, accent)
            .apply()
    }

    fun getCustomKeyboardColors(): KeyboardColors {
        return KeyboardThemes.buildCustomTheme(
            background = Color(_customBgColor.value),
            letterKeyBackground = Color(_customKeyBgColor.value),
            letterKeyTextColor = Color(_customTextColor.value),
            enterKeyBackground = Color(_customAccentColor.value)
        )
    }

    companion object {
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_LAPTOP_BAR_VISIBLE = "laptop_bar_visible"
        private const val KEY_HOLD_FOR_SYMBOLS = "hold_for_symbols"
        private const val KEY_THEME_TYPE = "theme_type"
        private const val KEY_CUSTOM_BG = "custom_bg"
        private const val KEY_CUSTOM_KEY_BG = "custom_key_bg"
        private const val KEY_CUSTOM_TEXT = "custom_text"
        private const val KEY_CUSTOM_ACCENT = "custom_accent"

        @Volatile
        private var instance: KeyboardPreferences? = null

        fun getInstance(context: Context): KeyboardPreferences {
            return instance ?: synchronized(this) {
                instance ?: KeyboardPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
