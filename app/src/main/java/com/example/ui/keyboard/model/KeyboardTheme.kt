package com.example.ui.keyboard.model

import androidx.compose.ui.graphics.Color

enum class KeyboardThemeType(val displayName: String) {
    GBOARD_DARK("Gboard Dark"),
    AMOLED_BLACK("Pitch Black"),
    GBOARD_LIGHT("Gboard Light"),
    CYBER_NAVY("Cyber Navy")
}

data class KeyboardColors(
    val background: Color,
    val toolbarBackground: Color,
    val toolbarIconTint: Color,
    val toolbarIconActiveTint: Color,
    val letterKeyBackground: Color,
    val letterKeyTextColor: Color,
    val letterKeySecondaryTextColor: Color,
    val functionKeyBackground: Color,
    val functionKeyTextColor: Color,
    val enterKeyBackground: Color,
    val enterKeyTextColor: Color,
    val laptopRowBackground: Color,
    val laptopKeyBackground: Color,
    val laptopKeyTextColor: Color,
    val laptopKeyActiveBackground: Color,
    val laptopKeyActiveTextColor: Color,
    val popupBackground: Color,
    val popupTextColor: Color,
    val chipBackground: Color,
    val chipTextColor: Color,
    val isDark: Boolean
)

object KeyboardThemes {
    val GboardDark = KeyboardColors(
        background = Color(0xFF1E1F24),
        toolbarBackground = Color(0xFF191A1E),
        toolbarIconTint = Color(0xFFC7C7CF),
        toolbarIconActiveTint = Color(0xFF8AB4F8),
        letterKeyBackground = Color(0xFF33353D),
        letterKeyTextColor = Color(0xFFE8EAED),
        letterKeySecondaryTextColor = Color(0xFF9AA0A6),
        functionKeyBackground = Color(0xFF282A30),
        functionKeyTextColor = Color(0xFFE8EAED),
        enterKeyBackground = Color(0xFF8AB4F8),
        enterKeyTextColor = Color(0xFF041E49),
        laptopRowBackground = Color(0xFF151619),
        laptopKeyBackground = Color(0xFF25272E),
        laptopKeyTextColor = Color(0xFFD3E3FD),
        laptopKeyActiveBackground = Color(0xFF8AB4F8),
        laptopKeyActiveTextColor = Color(0xFF041E49),
        popupBackground = Color(0xFF2D2F36),
        popupTextColor = Color(0xFFFFFFFF),
        chipBackground = Color(0xFF2C2E35),
        chipTextColor = Color(0xFFE8EAED),
        isDark = true
    )

    val AmoledBlack = KeyboardColors(
        background = Color(0xFF000000),
        toolbarBackground = Color(0xFF050505),
        toolbarIconTint = Color(0xFFE0E0E6),
        toolbarIconActiveTint = Color(0xFF64B5F6),
        letterKeyBackground = Color(0xFF1C1C1F),
        letterKeyTextColor = Color(0xFFFFFFFF),
        letterKeySecondaryTextColor = Color(0xFF888894),
        functionKeyBackground = Color(0xFF141416),
        functionKeyTextColor = Color(0xFFFFFFFF),
        enterKeyBackground = Color(0xFF2979FF),
        enterKeyTextColor = Color(0xFFFFFFFF),
        laptopRowBackground = Color(0xFF08080A),
        laptopKeyBackground = Color(0xFF18181D),
        laptopKeyTextColor = Color(0xFFE1F5FE),
        laptopKeyActiveBackground = Color(0xFF2979FF),
        laptopKeyActiveTextColor = Color(0xFFFFFFFF),
        popupBackground = Color(0xFF1A1A20),
        popupTextColor = Color(0xFFFFFFFF),
        chipBackground = Color(0xFF1A1A1E),
        chipTextColor = Color(0xFFFFFFFF),
        isDark = true
    )

    val GboardLight = KeyboardColors(
        background = Color(0xFFECEFF1),
        toolbarBackground = Color(0xFFDFE3E6),
        toolbarIconTint = Color(0xFF444746),
        toolbarIconActiveTint = Color(0xFF0B57D0),
        letterKeyBackground = Color(0xFFFFFFFF),
        letterKeyTextColor = Color(0xFF1F1F1F),
        letterKeySecondaryTextColor = Color(0xFF747775),
        functionKeyBackground = Color(0xFFD6DBDF),
        functionKeyTextColor = Color(0xFF1F1F1F),
        enterKeyBackground = Color(0xFF0B57D0),
        enterKeyTextColor = Color(0xFFFFFFFF),
        laptopRowBackground = Color(0xFFDDE2E6),
        laptopKeyBackground = Color(0xFFF1F3F5),
        laptopKeyTextColor = Color(0xFF0B57D0),
        laptopKeyActiveBackground = Color(0xFF0B57D0),
        laptopKeyActiveTextColor = Color(0xFFFFFFFF),
        popupBackground = Color(0xFFFFFFFF),
        popupTextColor = Color(0xFF1F1F1F),
        chipBackground = Color(0xFFFFFFFF),
        chipTextColor = Color(0xFF1F1F1F),
        isDark = false
    )

    val CyberNavy = KeyboardColors(
        background = Color(0xFF0A111E),
        toolbarBackground = Color(0xFF060B14),
        toolbarIconTint = Color(0xFF80D8FF),
        toolbarIconActiveTint = Color(0xFF00E5FF),
        letterKeyBackground = Color(0xFF132238),
        letterKeyTextColor = Color(0xFFE1F5FE),
        letterKeySecondaryTextColor = Color(0xFF00B0FF),
        functionKeyBackground = Color(0xFF0F1A2A),
        functionKeyTextColor = Color(0xFF80D8FF),
        enterKeyBackground = Color(0xFF00E5FF),
        enterKeyTextColor = Color(0xFF04192B),
        laptopRowBackground = Color(0xFF060D17),
        laptopKeyBackground = Color(0xFF16253C),
        laptopKeyTextColor = Color(0xFF00E5FF),
        laptopKeyActiveBackground = Color(0xFF00E5FF),
        laptopKeyActiveTextColor = Color(0xFF04192B),
        popupBackground = Color(0xFF12243C),
        popupTextColor = Color(0xFFE1F5FE),
        chipBackground = Color(0xFF112239),
        chipTextColor = Color(0xFFE1F5FE),
        isDark = true
    )

    fun getTheme(type: KeyboardThemeType): KeyboardColors {
        return when (type) {
            KeyboardThemeType.GBOARD_DARK -> GboardDark
            KeyboardThemeType.AMOLED_BLACK -> AmoledBlack
            KeyboardThemeType.GBOARD_LIGHT -> GboardLight
            KeyboardThemeType.CYBER_NAVY -> CyberNavy
        }
    }
}
