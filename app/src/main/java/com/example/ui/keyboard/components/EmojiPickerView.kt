package com.example.ui.keyboard.components

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.keyboard.model.EmojiData
import com.example.ui.keyboard.model.KeyboardColors

@Composable
fun EmojiPickerView(
    colors: KeyboardColors,
    onEmojiSelected: (String) -> Unit,
    onBackToLetters: () -> Unit,
    onBackspace: () -> Unit
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val currentCategory = EmojiData.categories[selectedCategoryIndex]
    val displayedEmojis = if (searchQuery.isBlank()) {
        currentCategory.emojis
    } else {
        EmojiData.categories.flatMap { it.emojis }.distinct()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .background(colors.background)
    ) {
        // Search & Back Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
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

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search emojis...", color = colors.letterKeySecondaryTextColor, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("search_emojis_field"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.toolbarIconTint,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = colors.toolbarIconTint,
                                modifier = Modifier.size(16.dp)
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
                shape = RoundedCornerShape(20.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Backspace Key
            Box(
                modifier = Modifier
                    .size(38.dp)
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

        // Category Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedCategoryIndex,
            containerColor = colors.toolbarBackground,
            contentColor = colors.enterKeyBackground,
            edgePadding = 4.dp,
            divider = {}
        ) {
            EmojiData.categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedCategoryIndex == index,
                    onClick = {
                        selectedCategoryIndex = index
                        searchQuery = ""
                    },
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.icon,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        // Emoji Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp),
            contentPadding = PaddingValues(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            items(displayedEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEmojiSelected(emoji) }
                        .testTag("emoji_$emoji"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp
                    )
                }
            }
        }

        // Bottom Action Bar to return to ABC
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.toolbarBackground)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.enterKeyBackground)
                    .clickable(onClick = onBackToLetters)
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .testTag("btn_emoji_to_abc"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ABC",
                    color = colors.enterKeyTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Text(
                text = "${currentCategory.title} • Tap any emoji to type",
                color = colors.letterKeySecondaryTextColor,
                fontSize = 12.sp
            )
        }
    }
}
