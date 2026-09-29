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

    private val _autoSuggestEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_SUGGEST_ENABLED, true))
    val autoSuggestEnabled: StateFlow<Boolean> = _autoSuggestEnabled.asStateFlow()

    private val _recentEmojis = MutableStateFlow(loadRecentEmojis())
    val recentEmojis: StateFlow<List<String>> = _recentEmojis.asStateFlow()

    private val _keyFontSize = MutableStateFlow(prefs.getFloat(KEY_KEY_FONT_SIZE, 24f))
    val keyFontSize: StateFlow<Float> = _keyFontSize.asStateFlow()

    private val _themeType = MutableStateFlow(
        try {
            KeyboardThemeType.valueOf(prefs.getString(KEY_THEME_TYPE, KeyboardThemeType.GBOARD_DARK.name) ?: KeyboardThemeType.GBOARD_DARK.name)
        } catch (_: Exception) {
            KeyboardThemeType.GBOARD_DARK
        }
    )
    val themeType: StateFlow<KeyboardThemeType> = _themeType.asStateFlow()

    // Multi-Language Package Support (e.g. English, Sinhala, etc.)
    private val _installedLanguages = MutableStateFlow(loadInstalledLanguages())
    val installedLanguages: StateFlow<Set<String>> = _installedLanguages.asStateFlow()

    private val _currentLanguage = MutableStateFlow(prefs.getString(KEY_CURRENT_LANGUAGE, "en") ?: "en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // Optional dedicated number row
    private val _numberRowEnabled = MutableStateFlow(prefs.getBoolean(KEY_NUMBER_ROW_ENABLED, false))
    val numberRowEnabled: StateFlow<Boolean> = _numberRowEnabled.asStateFlow()

    // Custom RGB theme colors
    private val _customBgColor = MutableStateFlow(prefs.getInt(KEY_CUSTOM_BG, 0xFF1B263B.toInt()))
    val customBgColor: StateFlow<Int> = _customBgColor.asStateFlow()

    private val _customKeyBgColor = MutableStateFlow(prefs.getInt(KEY_CUSTOM_KEY_BG, 0xFF2E3D59.toInt()))
    val customKeyBgColor: StateFlow<Int> = _customKeyBgColor.asStateFlow()

    private val _customTextColor = MutableStateFlow(prefs.getInt(KEY_CUSTOM_TEXT, 0xFFE0E1DD.toInt()))
    val customTextColor: StateFlow<Int> = _customTextColor.asStateFlow()

    private val _customAccentColor = MutableStateFlow(prefs.getInt(KEY_CUSTOM_ACCENT, 0xFF00B4D8.toInt()))
    val customAccentColor: StateFlow<Int> = _customAccentColor.asStateFlow()

    // GitHub Remote Language Pack Repository Configuration
    private val _githubRepoOwner = MutableStateFlow(prefs.getString(KEY_GITHUB_REPO_OWNER, "savindukahandagamage2") ?: "savindukahandagamage2")
    val githubRepoOwner: StateFlow<String> = _githubRepoOwner.asStateFlow()

    private val _githubRepoName = MutableStateFlow(prefs.getString(KEY_GITHUB_REPO_NAME, "KeyPro") ?: "KeyPro")
    val githubRepoName: StateFlow<String> = _githubRepoName.asStateFlow()

    private val _githubRepoBranch = MutableStateFlow(prefs.getString(KEY_GITHUB_REPO_BRANCH, "main") ?: "main")
    val githubRepoBranch: StateFlow<String> = _githubRepoBranch.asStateFlow()

    fun setGithubRepoConfig(owner: String, repo: String, branch: String = "main") {
        val cleanOwner = owner.trim()
        val cleanRepo = repo.trim()
        val cleanBranch = branch.trim().ifEmpty { "main" }
        _githubRepoOwner.value = cleanOwner
        _githubRepoName.value = cleanRepo
        _githubRepoBranch.value = cleanBranch
        prefs.edit()
            .putString(KEY_GITHUB_REPO_OWNER, cleanOwner)
            .putString(KEY_GITHUB_REPO_NAME, cleanRepo)
            .putString(KEY_GITHUB_REPO_BRANCH, cleanBranch)
            .apply()
    }

    fun getGithubRawBaseUrl(): String {
        return "https://raw.githubusercontent.com/${_githubRepoOwner.value}/${_githubRepoName.value}/${_githubRepoBranch.value}/languages/"
    }

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

    fun setAutoSuggestEnabled(enabled: Boolean) {
        _autoSuggestEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_SUGGEST_ENABLED, enabled).apply()
    }

    private fun loadRecentEmojis(): List<String> {
        val saved = prefs.getString(KEY_RECENT_EMOJIS, null)
        return if (!saved.isNullOrBlank()) {
            saved.split(",").filter { it.isNotBlank() }
        } else {
            listOf("😊", "😂", "❤️", "👍", "🔥", "🎉", "✨", "🙌")
        }
    }

    fun addRecentEmoji(emoji: String) {
        if (emoji.isBlank()) return
        val current = _recentEmojis.value.toMutableList()
        current.remove(emoji)
        current.add(0, emoji)
        val capped = current.take(30)
        _recentEmojis.value = capped
        prefs.edit().putString(KEY_RECENT_EMOJIS, capped.joinToString(",")).apply()
    }

    private fun loadInstalledLanguages(): Set<String> {
        val saved = prefs.getString(KEY_INSTALLED_LANGUAGES, null)
        return if (!saved.isNullOrBlank()) {
            saved.split(",").filter { it.isNotBlank() }.toSet()
        } else {
            setOf("en") // English built-in
        }
    }

    fun installLanguage(langId: String) {
        val current = _installedLanguages.value.toMutableSet()
        current.add(langId)
        _installedLanguages.value = current
        prefs.edit().putString(KEY_INSTALLED_LANGUAGES, current.joinToString(",")).apply()
    }

    fun uninstallLanguage(langId: String) {
        if (langId == "en") return // English is built-in
        val current = _installedLanguages.value.toMutableSet()
        current.remove(langId)
        _installedLanguages.value = current
        prefs.edit().putString(KEY_INSTALLED_LANGUAGES, current.joinToString(",")).apply()
        if (_currentLanguage.value == langId) {
            setCurrentLanguage("en")
        }
    }

    fun isLanguageInstalled(langId: String): Boolean {
        return langId == "en" || _installedLanguages.value.contains(langId)
    }

    fun setCurrentLanguage(langId: String) {
        _currentLanguage.value = langId
        prefs.edit().putString(KEY_CURRENT_LANGUAGE, langId).apply()
    }

    fun cycleLanguage(): String {
        val list = _installedLanguages.value.toList().ifEmpty { listOf("en") }
        val currentIndex = list.indexOf(_currentLanguage.value)
        val nextIndex = if (currentIndex != -1 && currentIndex + 1 < list.size) currentIndex + 1 else 0
        val nextLang = list[nextIndex]
        setCurrentLanguage(nextLang)
        return nextLang
    }

    fun setNumberRowEnabled(enabled: Boolean) {
        _numberRowEnabled.value = enabled
        prefs.edit().putBoolean(KEY_NUMBER_ROW_ENABLED, enabled).apply()
    }

    fun setKeyFontSize(size: Float) {
        _keyFontSize.value = size
        prefs.edit().putFloat(KEY_KEY_FONT_SIZE, size).apply()
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
        private const val KEY_AUTO_SUGGEST_ENABLED = "auto_suggest_enabled"
        private const val KEY_RECENT_EMOJIS = "recent_emojis"
        private const val KEY_KEY_FONT_SIZE = "key_character_font_size"
        private const val KEY_THEME_TYPE = "theme_type"
        private const val KEY_CUSTOM_BG = "custom_bg"
        private const val KEY_CUSTOM_KEY_BG = "custom_key_bg"
        private const val KEY_CUSTOM_TEXT = "custom_text"
        private const val KEY_CUSTOM_ACCENT = "custom_accent"
        private const val KEY_INSTALLED_LANGUAGES = "installed_languages"
        private const val KEY_CURRENT_LANGUAGE = "current_language"
        private const val KEY_NUMBER_ROW_ENABLED = "number_row_enabled"
        private const val KEY_GITHUB_REPO_OWNER = "github_repo_owner"
        private const val KEY_GITHUB_REPO_NAME = "github_repo_name"
        private const val KEY_GITHUB_REPO_BRANCH = "github_repo_branch"

        @Volatile
        private var instance: KeyboardPreferences? = null

        fun getInstance(context: Context): KeyboardPreferences {
            return instance ?: synchronized(this) {
                instance ?: KeyboardPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
