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

    // Downloadable Emoji / Symbol Packs Support (e.g. Runic symbols, Egyptian Hieroglyphs)
    private val _installedEmojiPacks = MutableStateFlow(loadInstalledEmojiPacks())
    val installedEmojiPacks: StateFlow<Set<String>> = _installedEmojiPacks.asStateFlow()

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

    // Constant GitHub Language Pack Repository Configuration (SavinduK/ScriptBoard)
    private val _githubRepoOwner = MutableStateFlow(prefs.getString(KEY_GITHUB_REPO_OWNER, DEFAULT_REPO_OWNER) ?: DEFAULT_REPO_OWNER)
    val githubRepoOwner: StateFlow<String> = _githubRepoOwner.asStateFlow()

    private val _githubRepoName = MutableStateFlow(prefs.getString(KEY_GITHUB_REPO_NAME, DEFAULT_REPO_NAME) ?: DEFAULT_REPO_NAME)
    val githubRepoName: StateFlow<String> = _githubRepoName.asStateFlow()

    private val _githubRepoBranch = MutableStateFlow(prefs.getString(KEY_GITHUB_REPO_BRANCH, DEFAULT_REPO_BRANCH) ?: DEFAULT_REPO_BRANCH)
    val githubRepoBranch: StateFlow<String> = _githubRepoBranch.asStateFlow()

    // Clipboard Auto-Deletion (Retention: 7, 14, 28 days)
    private val _autoDeleteClipboardEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_DELETE_CLIPBOARD, false))
    val autoDeleteClipboardEnabled: StateFlow<Boolean> = _autoDeleteClipboardEnabled.asStateFlow()

    private val _clipboardRetentionDays = MutableStateFlow(prefs.getInt(KEY_CLIPBOARD_RETENTION_DAYS, 7))
    val clipboardRetentionDays: StateFlow<Int> = _clipboardRetentionDays.asStateFlow()

    // Cross-Device Clipboard Sync Settings
    private val _clipboardSyncEnabled = MutableStateFlow(prefs.getBoolean(KEY_CLIPBOARD_SYNC_ENABLED, true))
    val clipboardSyncEnabled: StateFlow<Boolean> = _clipboardSyncEnabled.asStateFlow()

    private val _clipboardSyncImages = MutableStateFlow(prefs.getBoolean(KEY_CLIPBOARD_SYNC_IMAGES, true))
    val clipboardSyncImages: StateFlow<Boolean> = _clipboardSyncImages.asStateFlow()

    private val _persistentSyncServiceEnabled = MutableStateFlow(prefs.getBoolean(KEY_PERSISTENT_SYNC_SERVICE, true))
    val persistentSyncServiceEnabled: StateFlow<Boolean> = _persistentSyncServiceEnabled.asStateFlow()

    // Tablet / Screen Keyboard Scale (0.75f to 1.4f, default 1.0f)
    private val _tabletKeyboardScale = MutableStateFlow(prefs.getFloat(KEY_TABLET_KEYBOARD_SCALE, 1.0f))
    val tabletKeyboardScale: StateFlow<Float> = _tabletKeyboardScale.asStateFlow()

    // Hide keyboard when physical/hardware keyboard is connected (Wired USB or Bluetooth)
    private val _hideOnPhysicalKeyboard = MutableStateFlow(prefs.getBoolean(KEY_HIDE_ON_PHYSICAL_KEYBOARD, true))
    val hideOnPhysicalKeyboard: StateFlow<Boolean> = _hideOnPhysicalKeyboard.asStateFlow()

    private val _bluetoothSyncEnabled = MutableStateFlow(prefs.getBoolean(KEY_BLUETOOTH_SYNC_ENABLED, true))
    val bluetoothSyncEnabled: StateFlow<Boolean> = _bluetoothSyncEnabled.asStateFlow()

    private val _wifiSyncEnabled = MutableStateFlow(prefs.getBoolean(KEY_WIFI_SYNC_ENABLED, true))
    val wifiSyncEnabled: StateFlow<Boolean> = _wifiSyncEnabled.asStateFlow()

    private val _localDeviceName = MutableStateFlow(
        prefs.getString(KEY_LOCAL_DEVICE_NAME, android.os.Build.MODEL ?: "Android Device")
            ?: (android.os.Build.MODEL ?: "Android Device")
    )
    val localDeviceName: StateFlow<String> = _localDeviceName.asStateFlow()

    fun getLocalDeviceId(): String {
        var id = prefs.getString(KEY_LOCAL_DEVICE_ID, null)
        if (id.isNullOrBlank()) {
            id = java.util.UUID.randomUUID().toString().take(12)
            prefs.edit().putString(KEY_LOCAL_DEVICE_ID, id).apply()
        }
        return id
    }

    fun getLocalSharedSecret(): String {
        var secret = prefs.getString(KEY_LOCAL_SHARED_SECRET, null)
        if (secret.isNullOrBlank()) {
            // Generate a secure random 32-character hex secret
            val randomBytes = ByteArray(16)
            java.security.SecureRandom().nextBytes(randomBytes)
            secret = randomBytes.joinToString("") { "%02x".format(it) }
            prefs.edit().putString(KEY_LOCAL_SHARED_SECRET, secret).apply()
        }
        return secret
    }

    fun getLocalBluetoothMac(): String {
        return try {
            val addr = android.bluetooth.BluetoothAdapter.getDefaultAdapter()?.address
            if (!addr.isNullOrBlank() && addr != "02:00:00:00:00:00") {
                addr
            } else {
                val id = getLocalDeviceId()
                "02:00:${id.take(2)}:${id.drop(2).take(2)}:${id.drop(4).take(2)}:${id.drop(6).take(2)}"
            }
        } catch (_: Exception) {
            "02:00:00:00:00:00"
        }
    }

    fun setAutoDeleteClipboardEnabled(enabled: Boolean) {
        _autoDeleteClipboardEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_DELETE_CLIPBOARD, enabled).apply()
    }

    fun setClipboardRetentionDays(days: Int) {
        val validDays = if (days in listOf(7, 14, 28)) days else 7
        _clipboardRetentionDays.value = validDays
        prefs.edit().putInt(KEY_CLIPBOARD_RETENTION_DAYS, validDays).apply()
    }

    fun setClipboardSyncEnabled(enabled: Boolean) {
        _clipboardSyncEnabled.value = enabled
        prefs.edit().putBoolean(KEY_CLIPBOARD_SYNC_ENABLED, enabled).apply()
    }

    fun setClipboardSyncImages(enabled: Boolean) {
        _clipboardSyncImages.value = enabled
        prefs.edit().putBoolean(KEY_CLIPBOARD_SYNC_IMAGES, enabled).apply()
    }

    fun setPersistentSyncServiceEnabled(enabled: Boolean) {
        _persistentSyncServiceEnabled.value = enabled
        prefs.edit().putBoolean(KEY_PERSISTENT_SYNC_SERVICE, enabled).apply()
    }

    fun setTabletKeyboardScale(scale: Float) {
        val clamped = scale.coerceIn(0.75f, 1.4f)
        _tabletKeyboardScale.value = clamped
        prefs.edit().putFloat(KEY_TABLET_KEYBOARD_SCALE, clamped).apply()
    }

    fun setHideOnPhysicalKeyboard(enabled: Boolean) {
        _hideOnPhysicalKeyboard.value = enabled
        prefs.edit().putBoolean(KEY_HIDE_ON_PHYSICAL_KEYBOARD, enabled).apply()
    }

    fun setBluetoothSyncEnabled(enabled: Boolean) {
        _bluetoothSyncEnabled.value = enabled
        prefs.edit().putBoolean(KEY_BLUETOOTH_SYNC_ENABLED, enabled).apply()
    }

    fun setWifiSyncEnabled(enabled: Boolean) {
        _wifiSyncEnabled.value = enabled
        prefs.edit().putBoolean(KEY_WIFI_SYNC_ENABLED, enabled).apply()
    }

    fun setLocalDeviceName(name: String) {
        val trimmed = name.trim().ifEmpty { android.os.Build.MODEL ?: "Android Device" }
        _localDeviceName.value = trimmed
        prefs.edit().putString(KEY_LOCAL_DEVICE_NAME, trimmed).apply()
    }

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

    private fun loadInstalledEmojiPacks(): Set<String> {
        val saved = prefs.getString(KEY_INSTALLED_EMOJI_PACKS, null)
        return if (!saved.isNullOrBlank()) {
            saved.split(",").filter { it.isNotBlank() }.toSet()
        } else {
            emptySet()
        }
    }

    fun installEmojiPack(packId: String) {
        val current = _installedEmojiPacks.value.toMutableSet()
        current.add(packId)
        _installedEmojiPacks.value = current
        prefs.edit().putString(KEY_INSTALLED_EMOJI_PACKS, current.joinToString(",")).apply()
    }

    fun uninstallEmojiPack(packId: String) {
        val current = _installedEmojiPacks.value.toMutableSet()
        current.remove(packId)
        _installedEmojiPacks.value = current
        prefs.edit().putString(KEY_INSTALLED_EMOJI_PACKS, current.joinToString(",")).apply()
    }

    fun isEmojiPackInstalled(packId: String): Boolean {
        return _installedEmojiPacks.value.contains(packId)
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
        private const val KEY_INSTALLED_EMOJI_PACKS = "installed_emoji_packs"
        private const val KEY_CURRENT_LANGUAGE = "current_language"
        private const val KEY_NUMBER_ROW_ENABLED = "number_row_enabled"
        private const val KEY_GITHUB_REPO_OWNER = "github_repo_owner"
        private const val KEY_GITHUB_REPO_NAME = "github_repo_name"
        private const val KEY_GITHUB_REPO_BRANCH = "github_repo_branch"

        private const val KEY_AUTO_DELETE_CLIPBOARD = "clipboard_auto_delete_enabled"
        private const val KEY_CLIPBOARD_RETENTION_DAYS = "clipboard_retention_days"
        private const val KEY_CLIPBOARD_SYNC_ENABLED = "clipboard_sync_enabled"
        private const val KEY_CLIPBOARD_SYNC_IMAGES = "clipboard_sync_images"
        private const val KEY_LOCAL_DEVICE_ID = "local_device_id"
        private const val KEY_LOCAL_DEVICE_NAME = "local_device_name"
        private const val KEY_LOCAL_SHARED_SECRET = "local_shared_secret"
        private const val KEY_PERSISTENT_SYNC_SERVICE = "persistent_sync_service"
        private const val KEY_TABLET_KEYBOARD_SCALE = "tablet_keyboard_scale"
        private const val KEY_HIDE_ON_PHYSICAL_KEYBOARD = "hide_on_physical_keyboard"
        private const val KEY_BLUETOOTH_SYNC_ENABLED = "bluetooth_sync_enabled"
        private const val KEY_WIFI_SYNC_ENABLED = "wifi_sync_enabled"

        const val DEFAULT_REPO_OWNER = "SavinduK"
        const val DEFAULT_REPO_NAME = "ScriptBoard"
        const val DEFAULT_REPO_BRANCH = "main"

        @Volatile
        private var instance: KeyboardPreferences? = null

        fun getInstance(context: Context): KeyboardPreferences {
            return instance ?: synchronized(this) {
                instance ?: KeyboardPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
