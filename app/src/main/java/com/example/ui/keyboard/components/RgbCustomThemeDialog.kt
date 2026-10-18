package com.example.ui.keyboard.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.model.KeyboardThemes

private enum class ColorTarget(val label: String) {
    BACKGROUND("Background"),
    KEY_BG("Key Cap"),
    TEXT("Letters / Text"),
    ACCENT("Enter / Accent")
}

data class PresetPalette(
    val name: String,
    val bg: Color,
    val keyBg: Color,
    val text: Color,
    val accent: Color
)

val PRESET_PALETTES = listOf(
    PresetPalette("Cyber Neon", Color(0xFF130924), Color(0xFF261245), Color(0xFF00FFFF), Color(0xFFFF007F)),
    PresetPalette("Emerald Forest", Color(0xFF0A1F14), Color(0xFF143D28), Color(0xFFE8F5E9), Color(0xFF00E676)),
    PresetPalette("Sunset Flame", Color(0xFF240E14), Color(0xFF451926), Color(0xFFFFE0B2), Color(0xFFFF5722)),
    PresetPalette("Deep Cobalt", Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFFE0E1DD), Color(0xFF00B4D8)),
    PresetPalette("Hacker Terminal", Color(0xFF0A0A0A), Color(0xFF1C1C1C), Color(0xFF00FF66), Color(0xFF39FF14)),
    PresetPalette("Lavender Bliss", Color(0xFF1F1D2B), Color(0xFF2F2C3F), Color(0xFFE5E0F2), Color(0xFFBB86FC))
)

@Composable
fun RgbCustomThemeDialog(
    initialColors: KeyboardColors,
    onDismiss: () -> Unit,
    onApply: (bg: Int, keyBg: Int, text: Int, accent: Int) -> Unit
) {
    var selectedTarget by remember { mutableStateOf(ColorTarget.BACKGROUND) }

    var bgRed by remember { mutableIntStateOf((initialColors.background.red * 255).toInt()) }
    var bgGreen by remember { mutableIntStateOf((initialColors.background.green * 255).toInt()) }
    var bgBlue by remember { mutableIntStateOf((initialColors.background.blue * 255).toInt()) }

    var keyRed by remember { mutableIntStateOf((initialColors.letterKeyBackground.red * 255).toInt()) }
    var keyGreen by remember { mutableIntStateOf((initialColors.letterKeyBackground.green * 255).toInt()) }
    var keyBlue by remember { mutableIntStateOf((initialColors.letterKeyBackground.blue * 255).toInt()) }

    var textRed by remember { mutableIntStateOf((initialColors.letterKeyTextColor.red * 255).toInt()) }
    var textGreen by remember { mutableIntStateOf((initialColors.letterKeyTextColor.green * 255).toInt()) }
    var textBlue by remember { mutableIntStateOf((initialColors.letterKeyTextColor.blue * 255).toInt()) }

    var accentRed by remember { mutableIntStateOf((initialColors.enterKeyBackground.red * 255).toInt()) }
    var accentGreen by remember { mutableIntStateOf((initialColors.enterKeyBackground.green * 255).toInt()) }
    var accentBlue by remember { mutableIntStateOf((initialColors.enterKeyBackground.blue * 255).toInt()) }

    val currentBg = Color(bgRed, bgGreen, bgBlue)
    val currentKeyBg = Color(keyRed, keyGreen, keyBlue)
    val currentText = Color(textRed, textGreen, textBlue)
    val currentAccent = Color(accentRed, accentGreen, accentBlue)

    val previewTheme = remember(bgRed, bgGreen, bgBlue, keyRed, keyGreen, keyBlue, textRed, textGreen, textBlue, accentRed, accentGreen, accentBlue) {
        KeyboardThemes.buildCustomTheme(
            background = currentBg,
            letterKeyBackground = currentKeyBg,
            letterKeyTextColor = currentText,
            enterKeyBackground = currentAccent
        )
    }

    val (activeRed, activeGreen, activeBlue) = when (selectedTarget) {
        ColorTarget.BACKGROUND -> Triple(bgRed, bgGreen, bgBlue)
        ColorTarget.KEY_BG -> Triple(keyRed, keyGreen, keyBlue)
        ColorTarget.TEXT -> Triple(textRed, textGreen, textBlue)
        ColorTarget.ACCENT -> Triple(accentRed, accentGreen, accentBlue)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1F24),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dialog_rgb_theme_maker"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = currentAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RGB Custom Theme Studio",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Keyboard Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = previewTheme.background),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, currentAccent.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "LIVE KEYBOARD PREVIEW",
                            color = currentAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        // Sample Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Q", "W", "E", "R", "T", "Y", "U").forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(previewTheme.letterKeyBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        color = previewTheme.letterKeyTextColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Sample Row 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(previewTheme.functionKeyBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "?123",
                                    color = previewTheme.functionKeyTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(3.5f)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(previewTheme.letterKeyBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Space",
                                    color = previewTheme.letterKeySecondaryTextColor,
                                    fontSize = 11.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1.6f)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(previewTheme.enterKeyBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "↵ Enter",
                                    color = previewTheme.enterKeyTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Presets Quick Swatches
                Text(
                    text = "Quick Palettes",
                    color = Color(0xFFB0B0B0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 2.dp)
                ) {
                    items(PRESET_PALETTES) { palette ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    bgRed = (palette.bg.red * 255).toInt()
                                    bgGreen = (palette.bg.green * 255).toInt()
                                    bgBlue = (palette.bg.blue * 255).toInt()

                                    keyRed = (palette.keyBg.red * 255).toInt()
                                    keyGreen = (palette.keyBg.green * 255).toInt()
                                    keyBlue = (palette.keyBg.blue * 255).toInt()

                                    textRed = (palette.text.red * 255).toInt()
                                    textGreen = (palette.text.green * 255).toInt()
                                    textBlue = (palette.text.blue * 255).toInt()

                                    accentRed = (palette.accent.red * 255).toInt()
                                    accentGreen = (palette.accent.green * 255).toInt()
                                    accentBlue = (palette.accent.blue * 255).toInt()
                                }
                                .testTag("preset_palette_${palette.name}"),
                            color = palette.bg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, palette.accent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(palette.keyBg)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(palette.accent)
                                )
                                Text(
                                    text = palette.name,
                                    color = palette.text,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Component Target Selector Chips
                Text(
                    text = "Customize Element",
                    color = Color(0xFFB0B0B0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ColorTarget.values().forEach { target ->
                        val isTargetSelected = target == selectedTarget
                        val targetColor = when (target) {
                            ColorTarget.BACKGROUND -> currentBg
                            ColorTarget.KEY_BG -> currentKeyBg
                            ColorTarget.TEXT -> currentText
                            ColorTarget.ACCENT -> currentAccent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isTargetSelected) Color(0xFF33353D) else Color(0xFF25262B))
                                .border(
                                    width = if (isTargetSelected) 1.5.dp else 0.5.dp,
                                    color = if (isTargetSelected) currentAccent else Color(0xFF444444),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTarget = target }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(targetColor)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = target.label,
                                    color = if (isTargetSelected) Color.White else Color(0xFFAAAAAA),
                                    fontSize = 9.sp,
                                    fontWeight = if (isTargetSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Active Color Hex Badge
                val activeColor = when (selectedTarget) {
                    ColorTarget.BACKGROUND -> currentBg
                    ColorTarget.KEY_BG -> currentKeyBg
                    ColorTarget.TEXT -> currentText
                    ColorTarget.ACCENT -> currentAccent
                }
                val hexString = String.format("#%02X%02X%02X", activeRed, activeGreen, activeBlue)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF282A30))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Editing: ${selectedTarget.label}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(activeColor)
                                .border(1.dp, Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = hexString,
                            color = currentAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // RGB Sliders
                // Red Slider
                RgbChannelSlider(
                    channelName = "Red",
                    channelColor = Color(0xFFFF5252),
                    value = activeRed,
                    onValueChange = { newVal ->
                        when (selectedTarget) {
                            ColorTarget.BACKGROUND -> bgRed = newVal
                            ColorTarget.KEY_BG -> keyRed = newVal
                            ColorTarget.TEXT -> textRed = newVal
                            ColorTarget.ACCENT -> accentRed = newVal
                        }
                    },
                    testTag = "slider_rgb_red"
                )

                // Green Slider
                RgbChannelSlider(
                    channelName = "Green",
                    channelColor = Color(0xFF69F0AE),
                    value = activeGreen,
                    onValueChange = { newVal ->
                        when (selectedTarget) {
                            ColorTarget.BACKGROUND -> bgGreen = newVal
                            ColorTarget.KEY_BG -> keyGreen = newVal
                            ColorTarget.TEXT -> textGreen = newVal
                            ColorTarget.ACCENT -> accentGreen = newVal
                        }
                    },
                    testTag = "slider_rgb_green"
                )

                // Blue Slider
                RgbChannelSlider(
                    channelName = "Blue",
                    channelColor = Color(0xFF448AFF),
                    value = activeBlue,
                    onValueChange = { newVal ->
                        when (selectedTarget) {
                            ColorTarget.BACKGROUND -> bgBlue = newVal
                            ColorTarget.KEY_BG -> keyBlue = newVal
                            ColorTarget.TEXT -> textBlue = newVal
                            ColorTarget.ACCENT -> accentBlue = newVal
                        }
                    },
                    testTag = "slider_rgb_blue"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        currentBg.toArgb(),
                        currentKeyBg.toArgb(),
                        currentText.toArgb(),
                        currentAccent.toArgb()
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentAccent,
                    contentColor = previewTheme.enterKeyTextColor
                ),
                modifier = Modifier.testTag("btn_apply_rgb_theme")
            ) {
                Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply Custom Theme", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.LightGray)
            }
        }
    )
}

@Composable
private fun RgbChannelSlider(
    channelName: String,
    channelColor: Color,
    value: Int,
    onValueChange: (Int) -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(channelColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = channelName,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "$value",
                color = channelColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..255f,
            steps = 255,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = SliderDefaults.colors(
                thumbColor = channelColor,
                activeTrackColor = channelColor,
                inactiveTrackColor = Color(0xFF33353D)
            )
        )
    }
}
