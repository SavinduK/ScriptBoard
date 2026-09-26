package com.example.ui.keyboard.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.model.KeyboardThemeType
import com.example.ui.keyboard.model.KeyboardThemes
import com.example.ui.keyboard.util.KeyboardPreferences

@Composable
fun InKeyboardSettingsView(
    colors: KeyboardColors,
    onBackToLetters: () -> Unit
) {
    val context = LocalContext.current
    val keyboardPrefs = remember { KeyboardPreferences.getInstance(context) }

    val currentTheme by keyboardPrefs.themeType.collectAsState()
    val soundEnabled by keyboardPrefs.soundEnabled.collectAsState()
    val hapticEnabled by keyboardPrefs.hapticEnabled.collectAsState()
    val laptopBarEnabled by keyboardPrefs.laptopBarVisible.collectAsState()
    val holdForSymbolsEnabled by keyboardPrefs.holdForSymbolsEnabled.collectAsState()
    val autoSuggestEnabled by keyboardPrefs.autoSuggestEnabled.collectAsState()
    val keyFontSize by keyboardPrefs.keyFontSize.collectAsState()

    var showRgbThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .background(colors.background)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.toolbarBackground)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.enterKeyBackground)
                        .clickable(onClick = onBackToLetters)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("btn_settings_to_abc"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ABC",
                        color = colors.enterKeyTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = colors.enterKeyBackground,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Keyboard Settings",
                    color = colors.letterKeyTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.enterKeyBackground.copy(alpha = 0.15f))
                    .clickable(onClick = onBackToLetters)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("btn_settings_done"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Done",
                    color = colors.enterKeyBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Settings Content Scrollable Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Section 1: Themes
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = colors.enterKeyBackground,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KEYBOARD THEME",
                        color = colors.enterKeyBackground,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.enterKeyBackground.copy(alpha = 0.15f))
                        .clickable { showRgbThemeDialog = true }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("btn_open_rgb_palette_inline"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎨 Custom RGB Palette",
                        color = colors.enterKeyBackground,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 10.dp)
            ) {
                items(KeyboardThemeType.values()) { theme ->
                    val isSelected = theme == currentTheme
                    val previewColors = if (theme == KeyboardThemeType.CUSTOM) keyboardPrefs.getCustomKeyboardColors() else KeyboardThemes.getTheme(theme)

                    Card(
                        modifier = Modifier
                            .width(96.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                keyboardPrefs.setThemeType(theme)
                                if (theme == KeyboardThemeType.CUSTOM && isSelected) {
                                    showRgbThemeDialog = true
                                }
                            }
                            .testTag("inline_theme_${theme.name}"),
                        colors = CardDefaults.cardColors(containerColor = previewColors.background),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, colors.enterKeyBackground) else null,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(previewColors.letterKeyBackground)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(previewColors.enterKeyBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = previewColors.enterKeyTextColor,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = theme.displayName,
                                color = previewColors.letterKeyTextColor,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Section: Key Character Font Size (Request #2)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = colors.enterKeyBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Key Character Font Size",
                                color = colors.letterKeyTextColor,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${keyFontSize.toInt()} sp",
                            color = colors.enterKeyBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Small (20sp)" to 20f, "Normal (24sp)" to 24f, "Large (27sp)" to 27f, "XL (30sp)" to 30f).forEach { (label, size) ->
                            val isSelected = keyFontSize.toInt() == size.toInt()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) colors.enterKeyBackground else colors.functionKeyBackground)
                                    .clickable { keyboardPrefs.setKeyFontSize(size) }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) colors.enterKeyTextColor else colors.letterKeyTextColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section 2: Preferences Switches
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    // Hold Key for Symbols (Request #1)
                    SettingToggleRow(
                        icon = Icons.Default.TextFields,
                        title = "Hold Key for Symbols",
                        subtitle = "Hold letters for @, #, $, 0-9 & symbols",
                        checked = holdForSymbolsEnabled,
                        onCheckedChange = { keyboardPrefs.setHoldForSymbolsEnabled(it) },
                        colors = colors,
                        testTag = "switch_hold_for_symbols_inline"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Auto-suggest Words
                    SettingToggleRow(
                        icon = Icons.Default.AutoFixHigh,
                        title = "Auto-suggest Words",
                        subtitle = "Display 3 suggested words while typing",
                        checked = autoSuggestEnabled,
                        onCheckedChange = { keyboardPrefs.setAutoSuggestEnabled(it) },
                        colors = colors,
                        testTag = "switch_auto_suggest_inline"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Sound
                    SettingToggleRow(
                        icon = Icons.Default.VolumeUp,
                        title = "Typing Sound",
                        subtitle = "Play audio clicks on keypress",
                        checked = soundEnabled,
                        onCheckedChange = { keyboardPrefs.setSoundEnabled(it) },
                        colors = colors,
                        testTag = "switch_sound_inline"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Haptic
                    SettingToggleRow(
                        icon = Icons.Default.Vibration,
                        title = "Vibration / Haptics",
                        subtitle = "Haptic feedback on keypress",
                        checked = hapticEnabled,
                        onCheckedChange = { keyboardPrefs.setHapticEnabled(it) },
                        colors = colors,
                        testTag = "switch_haptic_inline"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Laptop keys bar
                    SettingToggleRow(
                        icon = Icons.Default.Laptop,
                        title = "Laptop Keys Bar",
                        subtitle = "Show Esc, Tab, Ctrl, Alt quick bar",
                        checked = laptopBarEnabled,
                        onCheckedChange = { keyboardPrefs.setLaptopBarVisible(it) },
                        colors = colors,
                        testTag = "switch_laptop_bar_inline"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section 3: System Input Settings Shortcut
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.functionKeyBackground)
                    .clickable {
                        try {
                            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("btn_open_system_ime_settings"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = colors.enterKeyBackground,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Android System Keyboard Settings",
                        color = colors.letterKeyTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = colors.letterKeySecondaryTextColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }

    if (showRgbThemeDialog) {
        RgbCustomThemeDialog(
            initialColors = if (currentTheme == KeyboardThemeType.CUSTOM) keyboardPrefs.getCustomKeyboardColors() else colors,
            onDismiss = { showRgbThemeDialog = false },
            onApply = { bg, keyBg, text, accent ->
                keyboardPrefs.setCustomThemeColors(bg, keyBg, text, accent)
                keyboardPrefs.setThemeType(KeyboardThemeType.CUSTOM)
                showRgbThemeDialog = false
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colors: KeyboardColors,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) colors.enterKeyBackground else colors.letterKeySecondaryTextColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = colors.letterKeyTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = colors.letterKeySecondaryTextColor,
                    fontSize = 10.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.enterKeyTextColor,
                checkedTrackColor = colors.enterKeyBackground,
                uncheckedThumbColor = colors.letterKeySecondaryTextColor,
                uncheckedTrackColor = colors.background
            )
        )
    }
}
