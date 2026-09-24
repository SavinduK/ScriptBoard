package com.example.ui.keyboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.keyboard.model.KeyboardColors

@Composable
fun KeyboardToolbar(
    colors: KeyboardColors,
    isLaptopBarVisible: Boolean,
    isExtendedPcActive: Boolean,
    onToggleLaptopBar: () -> Unit,
    onToggleExtendedPcKeys: () -> Unit,
    onPasteCopiedContent: () -> Unit,
    onOpenClipboard: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(colors.toolbarBackground)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Laptop key switch button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isLaptopBarVisible) colors.laptopKeyActiveBackground else Color.Transparent)
                .clickable(onClick = onToggleLaptopBar)
                .testTag("btn_trigger_laptop_keys"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Laptop,
                contentDescription = "Toggle Laptop Keys Bar",
                tint = if (isLaptopBarVisible) colors.laptopKeyActiveTextColor else colors.toolbarIconActiveTint,
                modifier = Modifier.size(20.dp)
            )
        }

        // 2. Extended laptop keyboard buttons
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isExtendedPcActive) colors.laptopKeyActiveBackground else Color.Transparent)
                .clickable(onClick = onToggleExtendedPcKeys)
                .testTag("btn_extended_pc_drawer"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Keyboard,
                contentDescription = "Extended Laptop Keyboard Layout",
                tint = if (isExtendedPcActive) colors.laptopKeyActiveTextColor else colors.toolbarIconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        // 3. Paste currently copied content button (Request #6)
        IconButton(
            onClick = onPasteCopiedContent,
            modifier = Modifier
                .size(38.dp)
                .testTag("btn_toolbar_paste")
        ) {
            Icon(
                imageVector = Icons.Default.ContentPasteGo,
                contentDescription = "Paste Currently Copied Content",
                tint = colors.enterKeyBackground,
                modifier = Modifier.size(20.dp)
            )
        }

        // 4. Clipboard manager button
        IconButton(
            onClick = onOpenClipboard,
            modifier = Modifier
                .size(38.dp)
                .testTag("btn_toolbar_clipboard")
        ) {
            Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = "Clipboard Manager",
                tint = colors.toolbarIconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        // 5. Settings button
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(38.dp)
                .testTag("btn_toolbar_settings")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = colors.toolbarIconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
