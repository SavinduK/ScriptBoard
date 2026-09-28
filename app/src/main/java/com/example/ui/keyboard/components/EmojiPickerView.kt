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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.util.KeyboardPreferences
import kotlinx.coroutines.launch
import java.io.File

enum class MediaPickerSection {
    EMOJIS,
    MATH,
    GREEK,
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

    var currentSection by remember { mutableStateOf(initialSection) }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) } // 0 is Recents, 1..N are EmojiData categories
    var stickerPendingDelete by remember { mutableStateOf<StickerEntity?>(null) }

    // Sub-filters for Math and Greek
    var selectedMathSubIndex by remember { mutableIntStateOf(0) }
    var selectedGreekSubIndex by remember { mutableIntStateOf(0) }

    val mathSubcategories = remember {
        listOf(
            "All" to EmojiData.mathSymbolsCategory.emojis,
            "Integrals & Calculus" to listOf("∫", "∬", "∭", "∮", "∯", "∰", "∂", "∇", "∆", "dx", "dy", "dt", "dz", "lim", "d/dx", "∑", "∏", "∐", "∞"),
            "Roots & Powers" to listOf("√", "∛", "∜", "²", "³", "⁴", "ⁿ", "⁻¹", "½", "⅓", "¼", "¾", "⅛", "⅜", "⅝", "⅞"),
            "Operators" to listOf("+", "−", "±", "∓", "×", "÷", "·", "∘", "*", "/", "%", "=", "≠", "≈", "≡", "≢", "∼", "≅", "∝", "≤", "≥", "≪", "≫", "<", ">"),
            "Sets & Logic" to listOf("∈", "∉", "∋", "∌", "⊂", "⊃", "⊆", "⊇", "⊄", "⊅", "∪", "∩", "∖", "∅", "∀", "∃", "∄", "¬", "∧", "∨", "⊕", "⊗", "⇒", "⇔", "∴", "∵", "∎"),
            "Number Sets" to listOf("ℝ", "ℕ", "ℤ", "ℚ", "ℂ", "ℙ")
        )
    }

    val greekSubcategories = remember {
        listOf(
            "All" to EmojiData.greekLettersCategory.emojis,
            "Lowercase (α..ω)" to listOf("α", "β", "γ", "δ", "ε", "ζ", "η", "θ", "ι", "κ", "λ", "μ", "ν", "ξ", "ο", "π", "ρ", "σ", "ς", "τ", "υ", "φ", "χ", "ψ", "ω"),
            "Uppercase (Α..Ω)" to listOf("Α", "Β", "Γ", "Δ", "Ε", "Ζ", "Η", "Θ", "Ι", "Κ", "Λ", "Μ", "Ν", "Ξ", "Ο", "Π", "Ρ", "Σ", "Τ", "Υ", "Φ", "Χ", "Ψ", "Ω"),
            "Math Variants" to listOf("ϑ", "ϕ", "ϖ", "ϰ", "ϱ", "ϵ")
        )
    }

    val isRecentsSelected = selectedCategoryIndex == 0
    val currentCategory = if (!isRecentsSelected && selectedCategoryIndex <= EmojiData.categories.size) {
        EmojiData.categories[selectedCategoryIndex - 1]
    } else null

    val displayedEmojis = if (isRecentsSelected) {
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
        // Top Bar: Back button, 4-Way Section Switcher (Emojis, Math, Greek, Stickers), Backspace
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.toolbarBackground)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToLetters,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("btn_emoji_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Keyboard",
                    tint = colors.toolbarIconTint
                )
            }

            // Section Switcher: Emojis | Math | Greek | Stickers
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.functionKeyBackground.copy(alpha = 0.6f))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sections = listOf(
                    MediaPickerSection.EMOJIS to "😊 Emojis",
                    MediaPickerSection.MATH to "∫ Math",
                    MediaPickerSection.GREEK to "Ω Greek",
                    MediaPickerSection.STICKERS to "✨ Stickers"
                )

                sections.forEach { (section, label) ->
                    val isSelected = currentSection == section
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) colors.enterKeyBackground else Color.Transparent)
                            .clickable { currentSection = section }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("tab_section_${section.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) colors.enterKeyTextColor else colors.letterKeySecondaryTextColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Backspace Key
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.functionKeyBackground)
                    .clickable(onClick = onBackspace)
                    .testTag("btn_emoji_backspace"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌫",
                    color = colors.functionKeyTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Section Content
        when (currentSection) {
            MediaPickerSection.EMOJIS -> {
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
                        onClick = { selectedCategoryIndex = 0 },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🕒", fontSize = 15.sp)
                        }
                    }

                    // Standard & Expanded Emoji Categories (including Math & Greek)
                    EmojiData.categories.forEachIndexed { index, category ->
                        Tab(
                            selected = selectedCategoryIndex == index + 1,
                            onClick = { selectedCategoryIndex = index + 1 },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = category.icon, fontSize = 15.sp)
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
                                    .size(38.dp)
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
                                    fontSize = 22.sp
                                )
                            }
                        }
                    }
                }
            }

            MediaPickerSection.MATH -> {
                // Mathematical Symbols Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Subcategory Filter Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.toolbarBackground)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(mathSubcategories) { index, (name, _) ->
                            val isSelected = selectedMathSubIndex == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) colors.enterKeyBackground else colors.letterKeyBackground)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) colors.enterKeyBackground else colors.functionKeyBackground.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedMathSubIndex = index }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) colors.enterKeyTextColor else colors.letterKeyTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Math Symbols Grid
                    val currentMathSymbols = mathSubcategories[selectedMathSubIndex].second
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(currentMathSymbols) { symbol ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.letterKeyBackground)
                                    .border(
                                        width = 1.dp,
                                        color = colors.functionKeyBackground.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        keyboardPrefs.addRecentEmoji(symbol)
                                        onEmojiSelected(symbol)
                                    }
                                    .testTag("math_symbol_$symbol"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = symbol,
                                    color = colors.letterKeyTextColor,
                                    fontSize = if (symbol.length > 2) 14.sp else 21.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            MediaPickerSection.GREEK -> {
                // Greek Letters Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Subcategory Filter Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.toolbarBackground)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(greekSubcategories) { index, (name, _) ->
                            val isSelected = selectedGreekSubIndex == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) colors.enterKeyBackground else colors.letterKeyBackground)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) colors.enterKeyBackground else colors.functionKeyBackground.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedGreekSubIndex = index }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) colors.enterKeyTextColor else colors.letterKeyTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Greek Letters Grid
                    val currentGreekSymbols = greekSubcategories[selectedGreekSubIndex].second
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(currentGreekSymbols) { letter ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.letterKeyBackground)
                                    .border(
                                        width = 1.dp,
                                        color = colors.functionKeyBackground.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        keyboardPrefs.addRecentEmoji(letter)
                                        onEmojiSelected(letter)
                                    }
                                    .testTag("greek_letter_$letter"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    color = colors.letterKeyTextColor,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Medium
                                )
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
                                // AsyncImage with Coil ImageDecoderDecoder decodes and plays animated .webp
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
                text = when (currentSection) {
                    MediaPickerSection.EMOJIS -> if (isRecentsSelected) "Recent Emojis" else "${currentCategory?.title}"
                    MediaPickerSection.MATH -> "Math: ${mathSubcategories[selectedMathSubIndex].first}"
                    MediaPickerSection.GREEK -> "Greek: ${greekSubcategories[selectedGreekSubIndex].first}"
                    MediaPickerSection.STICKERS -> "${stickers.size} Stickers Available • Hold to Delete"
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
