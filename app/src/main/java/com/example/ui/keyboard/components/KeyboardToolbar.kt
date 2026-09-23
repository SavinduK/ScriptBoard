package com.example.ui.keyboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SnippetEntity
import com.example.ui.keyboard.model.KeyboardColors

@Composable
fun KeyboardToolbar(
    colors: KeyboardColors,
    isLaptopBarVisible: Boolean,
    pinnedSnippets: List<SnippetEntity>,
    onToggleLaptopBar: () -> Unit,
    onOpenExtendedPcKeys: () -> Unit,
    onOpenEmoji: () -> Unit,
    onOpenClipboard: () -> Unit,
    onOpenThemePicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onInsertSnippet: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.toolbarBackground)
    ) {
        // Main Toolbar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Special Laptop Keys Trigger button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLaptopBarVisible) colors.laptopKeyActiveBackground else Color.Transparent)
                        .clickable(onClick = onToggleLaptopBar)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("btn_trigger_laptop_keys"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Laptop,
                            contentDescription = "Trigger Laptop Keys",
                            tint = if (isLaptopBarVisible) colors.laptopKeyActiveTextColor else colors.toolbarIconActiveTint,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PC Keys",
                            color = if (isLaptopBarVisible) colors.laptopKeyActiveTextColor else colors.toolbarIconActiveTint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Grid Menu / Extended PC Drawer
                IconButton(
                    onClick = onOpenExtendedPcKeys,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("btn_extended_pc_drawer")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Extended PC Keypad",
                        tint = colors.toolbarIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Right icons: Emoji, Settings, Theme, Clipboard, Mic
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenEmoji,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("btn_toolbar_emoji")
                ) {
                    Icon(
                        imageVector = Icons.Default.SentimentSatisfiedAlt,
                        contentDescription = "Emoji Picker",
                        tint = colors.toolbarIconTint,
                        modifier = Modifier.size(21.dp)
                    )
                }

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

                IconButton(
                    onClick = onOpenThemePicker,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("btn_toolbar_palette")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Theme Palette",
                        tint = colors.toolbarIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Clipboard Manager Icon
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenClipboard)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("btn_toolbar_clipboard"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Clipboard Manager",
                            tint = colors.toolbarIconTint,
                            modifier = Modifier.size(19.dp)
                        )
                        if (pinnedSnippets.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(colors.enterKeyBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${pinnedSnippets.size}",
                                    color = colors.enterKeyTextColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Pinned Snippets Strip (Fast 1-tap pasting from top toolbar!)
        if (pinnedSnippets.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(pinnedSnippets.take(8), key = { it.id }) { snippet ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.chipBackground)
                            .clickable { onInsertSnippet(snippet.content) }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                            .testTag("chip_snippet_${snippet.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = colors.enterKeyBackground,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = snippet.title,
                            color = colors.chipTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
