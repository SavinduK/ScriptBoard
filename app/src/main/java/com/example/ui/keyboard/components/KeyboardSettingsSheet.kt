package com.example.ui.keyboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.model.KeyboardThemeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardSettingsSheet(
    colors: KeyboardColors,
    currentThemeType: KeyboardThemeType,
    isLaptopBarEnabled: Boolean,
    isSoundEnabled: Boolean,
    isHapticEnabled: Boolean,
    onDismiss: () -> Unit,
    onThemeSelected: (KeyboardThemeType) -> Unit,
    onToggleLaptopBar: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onOpenSystemImeSettings: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Keyboard Settings",
                    color = colors.letterKeyTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_settings")) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.toolbarIconTint
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // System Keyboard Setup Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.functionKeyBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = colors.enterKeyBackground,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Use as System Keyboard",
                            color = colors.letterKeyTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enable KeyPro in Android Settings to use in any app",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = onOpenSystemImeSettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.enterKeyBackground,
                            contentColor = colors.enterKeyTextColor
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_enable_ime")
                    ) {
                        Text("Enable", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Options
            Text(
                text = "THEME & COLOR",
                color = colors.enterKeyBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            KeyboardThemeType.values().forEach { themeType ->
                val isSelected = currentThemeType == themeType
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.functionKeyBackground else Color.Transparent)
                        .clickable { onThemeSelected(themeType) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("theme_opt_${themeType.name}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when (themeType) {
                                        KeyboardThemeType.GBOARD_DARK -> Color(0xFF1E1F24)
                                        KeyboardThemeType.AMOLED_BLACK -> Color(0xFF000000)
                                        KeyboardThemeType.GBOARD_LIGHT -> Color(0xFFECEFF1)
                                        KeyboardThemeType.CYBER_NAVY -> Color(0xFF00E5FF)
                                        KeyboardThemeType.CUSTOM -> colors.enterKeyBackground
                                    }
                                )
                                .border(1.dp, colors.toolbarIconTint.copy(alpha = 0.5f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = themeType.displayName,
                            color = colors.letterKeyTextColor,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = colors.enterKeyBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Laptop Keys row toggle
            Text(
                text = "LAPTOP & PHYSICAL KEYS",
                color = colors.enterKeyBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show Laptop Keys Row (ESC, CTRL, ALT, Arrows)",
                        color = colors.letterKeyTextColor,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Adds dedicated Esc, Tab, Ctrl, Alt, Home, End, PgUp, PgDn & cursor keys",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = isLaptopBarEnabled,
                    onCheckedChange = onToggleLaptopBar,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.enterKeyTextColor,
                        checkedTrackColor = colors.enterKeyBackground
                    ),
                    modifier = Modifier.testTag("switch_laptop_bar")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sound and Vibration toggles
            Text(
                text = "FEEDBACK",
                color = colors.enterKeyBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = colors.toolbarIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sound on keypress",
                        color = colors.letterKeyTextColor,
                        fontSize = 14.sp
                    )
                }
                Switch(
                    checked = isSoundEnabled,
                    onCheckedChange = onToggleSound,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.enterKeyTextColor,
                        checkedTrackColor = colors.enterKeyBackground
                    ),
                    modifier = Modifier.testTag("switch_sound")
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = colors.toolbarIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Haptic vibration feedback",
                        color = colors.letterKeyTextColor,
                        fontSize = 14.sp
                    )
                }
                Switch(
                    checked = isHapticEnabled,
                    onCheckedChange = onToggleHaptic,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.enterKeyTextColor,
                        checkedTrackColor = colors.enterKeyBackground
                    ),
                    modifier = Modifier.testTag("switch_haptic")
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
