package com.example.ui.keyboard.components

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.StickerEntity
import com.example.data.StickerRepository
import com.example.ui.keyboard.model.EmojiData
import com.example.ui.keyboard.model.EmojiPack
import com.example.ui.keyboard.model.EmojiPackDownloader
import com.example.ui.keyboard.model.EmojiPackItem
import com.example.ui.keyboard.model.EmojiPackRegistry
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.util.KeyboardPreferences
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val keyboardPrefs = remember { KeyboardPreferences.getInstance(context) }
    val recentEmojis by keyboardPrefs.recentEmojis.collectAsState()
    val installedPackIds by keyboardPrefs.installedEmojiPacks.collectAsState()
    val downloader = remember { EmojiPackDownloader(context) }

    val installedPacks = remember(installedPackIds) {
        EmojiPackRegistry.ALL_PACKS.filter { installedPackIds.contains(it.id) }
    }

    var currentSection by remember { mutableStateOf(initialSection) }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var stickerPendingDelete by remember { mutableStateOf<StickerEntity?>(null) }
    var showQuickPacksDialog by remember { mutableStateOf(false) }
    var downloadingPackId by remember { mutableStateOf<String?>(null) }

    val totalStandardCategories = EmojiData.categories.size
    val isRecentsSelected = selectedCategoryIndex == 0
    val isStandardCategory = selectedCategoryIndex in 1..totalStandardCategories
    val currentCategory = if (isStandardCategory) {
        EmojiData.categories[selectedCategoryIndex - 1]
    } else null

    val installedPackIndex = if (selectedCategoryIndex > totalStandardCategories) {
        selectedCategoryIndex - totalStandardCategories - 1
    } else -1

    val currentPack = if (installedPackIndex in installedPacks.indices) {
        installedPacks[installedPackIndex]
    } else null

    val currentPackItems: List<EmojiPackItem> = remember(currentPack?.id, installedPackIds) {
        if (currentPack != null) {
            downloader.loadPackItems(currentPack.id)
        } else {
            emptyList()
        }
    }

    val displayedEmojis = if (isRecentsSelected) {
        recentEmojis
    } else if (isStandardCategory) {
        currentCategory?.emojis ?: emptyList()
    } else {
        emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .background(colors.background)
    ) {
        // Top Bar: Back button, Section Switcher (Emojis vs Stickers only), Backspace
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

            // Section Switcher: Emojis vs Stickers (Math & Greek buttons removed per Request #2)
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
                        .padding(horizontal = 16.dp, vertical = 6.dp)
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
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("tab_section_stickers"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✨ Stickers",
                        color = if (currentSection == MediaPickerSection.STICKERS) colors.enterKeyTextColor else colors.letterKeySecondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = if (currentSection == MediaPickerSection.STICKERS) FontWeight.Bold else FontWeight.Medium
                    )
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
                // Category Tab Row (Tab 0 is Recents 🕒, followed by Smileys, Gestures, Animals, Food, ..., Math ∫, Greek Ω)
                ScrollableTabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    containerColor = colors.toolbarBackground,
                    contentColor = colors.letterKeyTextColor,
                    edgePadding = 4.dp,
                    divider = {}
                ) {
                    // Recents Tab
                    Tab(
                        selected = selectedCategoryIndex == 0,
                        onClick = { selectedCategoryIndex = 0 },
                        selectedContentColor = colors.enterKeyBackground,
                        unselectedContentColor = colors.letterKeyTextColor,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🕒", fontSize = 16.sp)
                        }
                    }

                    // Standard & Expanded Emoji Categories (including Math ∫ & Greek Ω)
                    EmojiData.categories.forEachIndexed { index, category ->
                        val isSelected = selectedCategoryIndex == index + 1
                        Tab(
                            selected = isSelected,
                            onClick = { selectedCategoryIndex = index + 1 },
                            selectedContentColor = colors.enterKeyBackground,
                            unselectedContentColor = colors.letterKeyTextColor,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category.icon,
                                    color = if (isSelected) colors.enterKeyBackground else colors.letterKeyTextColor,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Downloadable Emoji / Ancient Symbol Packs
                    installedPacks.forEachIndexed { pIdx, pack ->
                        val tabIdx = totalStandardCategories + 1 + pIdx
                        val isSelected = selectedCategoryIndex == tabIdx
                        Tab(
                            selected = isSelected,
                            onClick = { selectedCategoryIndex = tabIdx },
                            selectedContentColor = colors.enterKeyBackground,
                            unselectedContentColor = colors.letterKeyTextColor,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pack.icon,
                                    color = if (isSelected) colors.enterKeyBackground else colors.letterKeyTextColor,
                                    fontSize = if (pack.icon.length > 2) 12.sp else 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // "+ Packs" Button to quick-download or browse packs
                    Tab(
                        selected = false,
                        onClick = { showQuickPacksDialog = true },
                        selectedContentColor = colors.enterKeyBackground,
                        unselectedContentColor = colors.letterKeyTextColor,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .testTag("tab_get_emoji_packs")
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.enterKeyBackground.copy(alpha = 0.2f))
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Get Emoji Packs",
                                    tint = colors.enterKeyBackground,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Packs",
                                    color = colors.enterKeyBackground,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Emoji & Symbols Grid
                if (currentPack != null) {
                    if (currentPackItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No symbols found for ${currentPack.name}",
                                    color = colors.letterKeySecondaryTextColor,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { showQuickPacksDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground)
                                ) {
                                    Text("Download or Reinstall", fontSize = 11.sp, color = colors.enterKeyTextColor)
                                }
                            }
                        }
                    } else if (currentPack.id == "kaomoji") {
                        // Kaomoji Grid (longer Japanese text emoticons)
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 88.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(currentPackItems) { item ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.letterKeyBackground)
                                        .border(0.5.dp, colors.functionKeyBackground.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .combinedClickable(
                                            onClick = {
                                                keyboardPrefs.addRecentEmoji(item.symbol)
                                                onEmojiSelected(item.symbol)
                                            },
                                            onLongClick = {
                                                if (item.name.isNotBlank()) {
                                                    Toast.makeText(context, item.name, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 8.dp)
                                        .testTag("emoji_kaomoji_${item.name.lowercase().replace(" ", "_")}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.symbol,
                                        color = colors.letterKeyTextColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    } else {
                        // Ancient Symbols Grid (Runic, Hieroglyphs, Alchemy, etc.)
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 44.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            items(currentPackItems) { item ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.letterKeyBackground)
                                        .combinedClickable(
                                            onClick = {
                                                keyboardPrefs.addRecentEmoji(item.symbol)
                                                onEmojiSelected(item.symbol)
                                            },
                                            onLongClick = {
                                                val detail = if (item.meaning.isNotBlank()) {
                                                    "${item.symbol} ${item.name}: ${item.meaning}"
                                                } else {
                                                    "${item.symbol} ${item.name}"
                                                }
                                                Toast.makeText(context, detail, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        .testTag("emoji_symbol_${item.symbol}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = item.symbol,
                                            color = colors.letterKeyTextColor,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (item.name.isNotBlank()) {
                                            Text(
                                                text = item.name,
                                                color = colors.letterKeySecondaryTextColor,
                                                fontSize = 7.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (displayedEmojis.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRecentsSelected) "No recent emojis yet" else "No matching items found",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    val isSymbolOrLetterCategory = currentCategory?.title in listOf("Math", "Greek")

                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.material3.LocalContentColor provides colors.letterKeyTextColor
                    ) {
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
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSymbolOrLetterCategory) colors.letterKeyBackground else Color.Transparent)
                                        .clickable {
                                            keyboardPrefs.addRecentEmoji(emoji)
                                            onEmojiSelected(emoji)
                                        }
                                        .testTag("emoji_$emoji"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Explicitly apply theme text color so Greek letters and Math symbols match the keyboard theme!
                                    Text(
                                        text = emoji,
                                        color = colors.letterKeyTextColor,
                                        fontSize = if (emoji.length > 2) 13.sp else if (isSymbolOrLetterCategory) 18.sp else 22.sp,
                                        fontWeight = if (isSymbolOrLetterCategory) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            MediaPickerSection.STICKERS -> {
                // Stickers Section (Renders Animated .webp Stickers)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
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
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "STICKERS",
                            color = colors.letterKeyTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Animated .webp supported",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 10.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Quick Button to open Sticker Studio in app
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.enterKeyBackground.copy(alpha = 0.2f))
                                .clickable {
                                    try {
                                        val intent = Intent(context, MainActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            putExtra("open_tab", "stickers")
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ Import",
                                color = colors.enterKeyBackground,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Load Demo Animated Stickers
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.functionKeyBackground)
                                .clickable {
                                    scope.launch {
                                        try {
                                            val db = AppDatabase.getDatabase(context)
                                            val repo = StickerRepository(db.stickerDao())
                                            repo.seedSampleStickers(context)
                                            Toast.makeText(context, "Loaded animated stickers!", Toast.LENGTH_SHORT).show()
                                        } catch (_: Exception) {}
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Load Demo",
                                color = colors.functionKeyTextColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (stickers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(12.dp),
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
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No Animated Stickers Yet",
                                color = colors.letterKeyTextColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Import animated .webp stickers from WhatsApp or files, or load the built-in demo stickers.",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val db = AppDatabase.getDatabase(context)
                                        val repo = StickerRepository(db.stickerDao())
                                        repo.seedSampleStickers(context)
                                        Toast.makeText(context, "Loaded animated demo stickers!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Load Animated Demo Stickers", fontSize = 11.sp, color = colors.enterKeyTextColor)
                            }
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

                                if (sticker.isAnimated) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.enterKeyBackground.copy(alpha = 0.85f))
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "ANIM",
                                            color = colors.enterKeyTextColor,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
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
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.enterKeyBackground)
                    .clickable(onClick = onBackToLetters)
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .testTag("btn_emoji_to_abc"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ABC",
                    color = colors.enterKeyTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Text(
                text = if (currentSection == MediaPickerSection.EMOJIS) {
                    when {
                        isRecentsSelected -> "Recent Emojis"
                        currentPack != null -> "${currentPack.name} (${currentPack.nativeName}) • ${currentPackItems.size} items"
                        else -> "${currentCategory?.title}"
                    }
                } else {
                    "${stickers.size} Stickers Available • Hold to Delete"
                },
                color = colors.letterKeySecondaryTextColor,
                fontSize = 11.sp
            )
        }
    }

    // In-keyboard Quick Emoji Packs Downloader Dialog
    if (showQuickPacksDialog) {
        AlertDialog(
            onDismissRequest = { showQuickPacksDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = colors.enterKeyBackground
                )
            },
            title = {
                Text(
                    text = "Download Emoji & Symbol Packs",
                    fontWeight = FontWeight.Bold,
                    color = colors.letterKeyTextColor,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ancient runes, hieroglyphs, kaomoji and alchemy symbols stored in the GitHub repository. Tap download to add them to your emoji drawer.",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    EmojiPackRegistry.ALL_PACKS.forEach { pack ->
                        val isInstalled = installedPackIds.contains(pack.id)
                        val isDownloading = downloadingPackId == pack.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.letterKeyBackground)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = pack.icon,
                                    fontSize = if (pack.icon.length > 2) 13.sp else 20.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = pack.name,
                                        color = colors.letterKeyTextColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${pack.itemCount} items • ${pack.sizeDisplay}",
                                        color = colors.letterKeySecondaryTextColor,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (isInstalled) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Installed",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Added",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            downloadingPackId = pack.id
                                            val res = downloader.downloadEmojiPack(pack) { _, _ -> }
                                            downloadingPackId = null
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "${pack.name} added to emoji drawer!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Download failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = !isDownloading,
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("btn_quick_download_${pack.id}")
                                ) {
                                    Text(
                                        text = if (isDownloading) "..." else "Download",
                                        fontSize = 11.sp,
                                        color = colors.enterKeyTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showQuickPacksDialog = false
                        try {
                            val intent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra("open_tab", "emoji_packs")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                ) {
                    Text("Open Full Manager", color = colors.enterKeyBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickPacksDialog = false }) {
                    Text("Close", color = colors.letterKeySecondaryTextColor)
                }
            },
            containerColor = colors.toolbarBackground
        )
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
