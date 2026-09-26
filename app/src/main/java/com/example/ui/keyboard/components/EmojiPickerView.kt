package com.example.ui.keyboard.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.StickerEntity
import com.example.ui.keyboard.model.EmojiData
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.util.KeyboardPreferences
import java.io.File

enum class MediaPickerSection {
    EMOJIS,
    STICKERS
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiPickerView(
    colors: KeyboardColors,
    stickers: List<StickerEntity> = emptyList(),
    initialSection: MediaPickerSection = MediaPickerSection.EMOJIS,
    onEmojiSelected: (String) -> Unit,
    onStickerSelected: (StickerEntity) -> Unit = {},
    onDeleteSticker: (StickerEntity) -> Unit = {},
    onBackToLetters: () -> Unit,
    onBackspace: () -> Unit
) {
    val context = LocalContext.current
    val keyboardPrefs = remember { KeyboardPreferences.getInstance(context) }
    val recentEmojis by keyboardPrefs.recentEmojis.collectAsState()

    var currentSection by remember { mutableStateOf(initialSection) }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) } // 0 is Recents, 1..N are EmojiData categories
    var searchQuery by remember { mutableStateOf("") }
    var stickerPendingDelete by remember { mutableStateOf<StickerEntity?>(null) }

    val isRecentsSelected = selectedCategoryIndex == 0
    val currentCategory = if (!isRecentsSelected) EmojiData.categories[selectedCategoryIndex - 1] else null

    val displayedEmojis = if (searchQuery.isNotBlank()) {
        EmojiData.categories.flatMap { it.emojis }.distinct()
    } else if (isRecentsSelected) {
        recentEmojis
    } else {
        currentCategory?.emojis ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .background(colors.background)
    ) {
        // Top Bar: Back button, Section Switcher (Emojis vs Stickers), Backspace
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.toolbarBackground)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToLetters,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_emoji_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Keyboard",
                    tint = colors.toolbarIconTint
                )
            }

            // Section Switcher: Emojis vs Stickers
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.functionKeyBackground.copy(alpha = 0.6f))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (currentSection == MediaPickerSection.EMOJIS) colors.enterKeyBackground else Color.Transparent)
                        .clickable { currentSection = MediaPickerSection.EMOJIS }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("tab_section_emojis"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "😊 Emojis",
                        color = if (currentSection == MediaPickerSection.EMOJIS) colors.enterKeyTextColor else colors.letterKeySecondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = if (currentSection == MediaPickerSection.EMOJIS) FontWeight.Bold else FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (currentSection == MediaPickerSection.STICKERS) colors.enterKeyBackground else Color.Transparent)
                        .clickable { currentSection = MediaPickerSection.STICKERS }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("tab_section_stickers"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✨ Stickers",
                            color = if (currentSection == MediaPickerSection.STICKERS) colors.enterKeyTextColor else colors.letterKeySecondaryTextColor,
                            fontSize = 12.sp,
                            fontWeight = if (currentSection == MediaPickerSection.STICKERS) FontWeight.Bold else FontWeight.Medium
                        )
                        if (stickers.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${stickers.size})",
                                color = if (currentSection == MediaPickerSection.STICKERS) colors.enterKeyTextColor else colors.enterKeyBackground,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Backspace Key
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.functionKeyBackground)
                    .clickable(onClick = onBackspace)
                    .testTag("btn_emoji_backspace"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌫",
                    color = colors.functionKeyTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Section Content
        when (currentSection) {
            MediaPickerSection.EMOJIS -> {
                // Search Bar for Emojis
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search emojis...", color = colors.letterKeySecondaryTextColor, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("search_emojis_field"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = colors.toolbarIconTint,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = colors.toolbarIconTint,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.functionKeyBackground,
                            unfocusedContainerColor = colors.functionKeyBackground,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = colors.letterKeyTextColor,
                            unfocusedTextColor = colors.letterKeyTextColor
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                }

                // Category Tab Row (Tab 0 is Recents 🕒)
                ScrollableTabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    containerColor = colors.toolbarBackground,
                    contentColor = colors.enterKeyBackground,
                    edgePadding = 4.dp,
                    divider = {}
                ) {
                    // Recents Tab
                    Tab(
                        selected = selectedCategoryIndex == 0,
                        onClick = {
                            selectedCategoryIndex = 0
                            searchQuery = ""
                        },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🕒",
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Standard Emoji Categories
                    EmojiData.categories.forEachIndexed { index, category ->
                        Tab(
                            selected = selectedCategoryIndex == index + 1,
                            onClick = {
                                selectedCategoryIndex = index + 1
                                searchQuery = ""
                            },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category.icon,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // Emoji Grid
                if (displayedEmojis.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRecentsSelected) "No recent emojis yet" else "No matching emojis found",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        items(displayedEmojis) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        keyboardPrefs.addRecentEmoji(emoji)
                                        onEmojiSelected(emoji)
                                    }
                                    .testTag("emoji_$emoji"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 23.sp
                                )
                            }
                        }
                    }
                }
            }

            MediaPickerSection.STICKERS -> {
                // Stickers Section (NO search bar per user request)
                // Shows most used stickers on top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = colors.enterKeyBackground,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "STICKERS",
                            color = colors.letterKeyTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Most used on top • Hold to delete",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 11.sp
                    )
                }

                if (stickers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SentimentSatisfiedAlt,
                                contentDescription = null,
                                tint = colors.letterKeySecondaryTextColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Stickers Imported Yet",
                                color = colors.letterKeyTextColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use the Stickers Studio on the Home page to import stickers from WhatsApp or device files.",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 64.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("grid_keyboard_stickers"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stickers, key = { it.id }) { sticker ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.letterKeyBackground)
                                    .border(
                                        width = 1.dp,
                                        color = colors.functionKeyBackground.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .combinedClickable(
                                        onClick = { onStickerSelected(sticker) },
                                        onLongClick = { stickerPendingDelete = sticker }
                                    )
                                    .padding(4.dp)
                                    .testTag("sticker_keyboard_item_${sticker.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = File(sticker.filePath),
                                    contentDescription = sticker.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar to return to ABC
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.toolbarBackground)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.enterKeyBackground)
                    .clickable(onClick = onBackToLetters)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("btn_emoji_to_abc"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ABC",
                    color = colors.enterKeyTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Text(
                text = if (currentSection == MediaPickerSection.EMOJIS) {
                    if (isRecentsSelected) "Recent Emojis" else "${currentCategory?.title}"
                } else {
                    "${stickers.size} Stickers Available"
                },
                color = colors.letterKeySecondaryTextColor,
                fontSize = 11.sp
            )
        }
    }

    // Long Hold Delete Confirmation Dialog for Stickers
    stickerPendingDelete?.let { sticker ->
        AlertDialog(
            onDismissRequest = { stickerPendingDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF5350)
                )
            },
            title = {
                Text(
                    text = "Delete Sticker?",
                    fontWeight = FontWeight.Bold,
                    color = colors.letterKeyTextColor
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${sticker.name}\" from your stickers?",
                    color = colors.letterKeySecondaryTextColor,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val s = stickerPendingDelete
                        stickerPendingDelete = null
                        if (s != null) {
                            onDeleteSticker(s)
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { stickerPendingDelete = null }) {
                    Text("Cancel", color = colors.letterKeySecondaryTextColor)
                }
            },
            containerColor = colors.toolbarBackground
        )
    }
}
