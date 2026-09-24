package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.keyboard.components.RgbCustomThemeDialog
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.model.KeyboardThemeType
import com.example.ui.keyboard.model.KeyboardThemes

@Composable
fun SettingsPageView(
    colors: KeyboardColors,
    currentThemeType: KeyboardThemeType,
    customColors: KeyboardColors,
    isSoundEnabled: Boolean,
    isHapticEnabled: Boolean,
    isLaptopBarEnabled: Boolean,
    isHoldForSymbolsEnabled: Boolean,
    onBack: () -> Unit,
    onThemeSelected: (KeyboardThemeType) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleLaptopBar: (Boolean) -> Unit,
    onToggleHoldForSymbols: (Boolean) -> Unit,
    onCustomColorsChanged: (bg: Int, keyBg: Int, text: Int, accent: Int) -> Unit
) {
    val context = LocalContext.current
    var showRgbThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(colors.toolbarBackground)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_back_from_settings")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = colors.letterKeyTextColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "Keyboard Settings",
                    color = colors.letterKeyTextColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Preferences, audio, and visual themes",
                    color = colors.letterKeySecondaryTextColor,
                    fontSize = 11.sp
                )
            }
        }

        // Settings Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Themes
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = colors.enterKeyBackground,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Keyboard Theme",
                                color = colors.letterKeyTextColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { showRgbThemeDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.enterKeyBackground,
                                contentColor = colors.enterKeyTextColor
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_page_custom_rgb_palette")
                        ) {
                            Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RGB Studio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(KeyboardThemeType.values()) { theme ->
                            val isSelected = theme == currentThemeType
                            val themeColors = if (theme == KeyboardThemeType.CUSTOM) customColors else KeyboardThemes.getTheme(theme)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onThemeSelected(theme)
                                        if (theme == KeyboardThemeType.CUSTOM && isSelected) {
                                            showRgbThemeDialog = true
                                        }
                                    }
                                    .testTag("theme_card_${theme.name}"),
                                colors = CardDefaults.cardColors(containerColor = themeColors.background),
                                border = if (isSelected) BorderStroke(2.5.dp, colors.enterKeyBackground) else BorderStroke(1.dp, themeColors.letterKeyBackground),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(themeColors.letterKeyBackground)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(themeColors.enterKeyBackground),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = themeColors.enterKeyTextColor,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = theme.displayName,
                                        color = themeColors.letterKeyTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Typing Feedback
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Feedback & Actions",
                        color = colors.letterKeyTextColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Hold Key for Symbols
                    SettingsPageToggle(
                        icon = Icons.Default.TextFields,
                        title = "Hold Key for Symbols",
                        subtitle = "Long press letter keys for @, #, $, 0-9 & symbols",
                        checked = isHoldForSymbolsEnabled,
                        onCheckedChange = onToggleHoldForSymbols,
                        colors = colors,
                        testTag = "page_switch_hold_symbols"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsPageToggle(
                        icon = Icons.Default.VolumeUp,
                        title = "Keypress Sound",
                        subtitle = "Audio feedback when typing keys",
                        checked = isSoundEnabled,
                        onCheckedChange = onToggleSound,
                        colors = colors,
                        testTag = "page_switch_sound"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsPageToggle(
                        icon = Icons.Default.Vibration,
                        title = "Haptic Vibration",
                        subtitle = "Vibration response on key tap",
                        checked = isHapticEnabled,
                        onCheckedChange = onToggleHaptic,
                        colors = colors,
                        testTag = "page_switch_haptic"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsPageToggle(
                        icon = Icons.Default.Laptop,
                        title = "Laptop Keys Bar",
                        subtitle = "Quick Esc, Tab, Ctrl, Alt, and Arrows bar",
                        checked = isLaptopBarEnabled,
                        onCheckedChange = onToggleLaptopBar,
                        colors = colors,
                        testTag = "page_switch_laptop_bar"
                    )
                }
            }

            // Section 3: Android OS Keyboard Settings
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    .testTag("btn_page_open_android_settings"),
                colors = CardDefaults.cardColors(containerColor = colors.functionKeyBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Android System Keyboard Settings",
                            color = colors.letterKeyTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage system default inputs & languages",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open System Settings",
                        tint = colors.enterKeyBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showRgbThemeDialog) {
        RgbCustomThemeDialog(
            initialColors = if (currentThemeType == KeyboardThemeType.CUSTOM) customColors else colors,
            onDismiss = { showRgbThemeDialog = false },
            onApply = { bg, keyBg, text, accent ->
                onCustomColorsChanged(bg, keyBg, text, accent)
                onThemeSelected(KeyboardThemeType.CUSTOM)
                showRgbThemeDialog = false
            }
        )
    }
}

@Composable
private fun SettingsPageToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colors: KeyboardColors,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = colors.letterKeyTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = colors.letterKeySecondaryTextColor,
                    fontSize = 11.sp
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
