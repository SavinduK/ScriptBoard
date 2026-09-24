package com.example.ui.keyboard.components

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SnippetEntity
import com.example.ui.keyboard.model.KeyboardColors

private enum class ClipboardTab {
    ALL, PINNED, HISTORY
}

@Composable
fun InKeyboardClipboardView(
    pinnedSnippets: List<SnippetEntity>,
    historySnippets: List<SnippetEntity>,
    colors: KeyboardColors,
    onSnippetSelected: (String) -> Unit,
    onTogglePin: (SnippetEntity) -> Unit,
    onDeleteSnippet: (Long) -> Unit,
    onSaveSnippet: (title: String, content: String, isPinned: Boolean, category: String, shortcut: String) -> Unit,
    onClearHistory: () -> Unit = {},
    onBackToLetters: () -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(ClipboardTab.ALL) }

    // On mount, auto-capture current system clipboard into history if new
    LaunchedEffect(Unit) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (cm != null && cm.hasPrimaryClip()) {
                val clip = cm.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).text?.toString()
                    if (!text.isNullOrBlank()) {
                        val alreadyInHistory = historySnippets.any { it.content == text } ||
                                pinnedSnippets.any { it.content == text }
                        if (!alreadyInHistory) {
                            val title = text.lineSequence().firstOrNull()?.trim()?.take(25)?.ifEmpty { "Clipboard Item" } ?: "Clipboard Item"
                            onSaveSnippet(title, text, false, "History", "")
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

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
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.enterKeyBackground)
                        .clickable(onClick = onBackToLetters)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("btn_clipboard_to_abc"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ABC",
                        color = colors.enterKeyTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tabs: All, Pinned, History
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.functionKeyBackground)
                        .padding(2.dp)
                ) {
                    TabChip(
                        label = "All",
                        isSelected = selectedTab == ClipboardTab.ALL,
                        colors = colors,
                        onClick = { selectedTab = ClipboardTab.ALL },
                        testTag = "tab_clipboard_all"
                    )
                    TabChip(
                        label = "Pinned (${pinnedSnippets.size})",
                        isSelected = selectedTab == ClipboardTab.PINNED,
                        colors = colors,
                        onClick = { selectedTab = ClipboardTab.PINNED },
                        testTag = "tab_clipboard_pinned"
                    )
                    TabChip(
                        label = "History (${historySnippets.size})",
                        isSelected = selectedTab == ClipboardTab.HISTORY,
                        colors = colors,
                        onClick = { selectedTab = ClipboardTab.HISTORY },
                        testTag = "tab_clipboard_history"
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (historySnippets.isNotEmpty() && (selectedTab == ClipboardTab.HISTORY || selectedTab == ClipboardTab.ALL)) {
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("btn_clear_clipboard_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear History",
                            tint = colors.toolbarIconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("btn_add_snippet_inline")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Pinned Snippet",
                        tint = colors.enterKeyBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Two Separate Sections (Pinned Messages & Clipboard History)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            // SECTION 1: PINNED MESSAGES
            if (selectedTab == ClipboardTab.ALL || selectedTab == ClipboardTab.PINNED) {
                item {
                    SectionHeader(
                        icon = Icons.Default.PushPin,
                        title = "PINNED MESSAGES (${pinnedSnippets.size})",
                        colors = colors
                    )
                }

                if (pinnedSnippets.isEmpty()) {
                    item {
                        EmptySectionPlaceholder(
                            text = "No pinned messages yet. Tap pin icon on any history item to keep it pinned!",
                            colors = colors
                        )
                    }
                } else {
                    items(pinnedSnippets, key = { "pinned_${it.id}" }) { snippet ->
                        SnippetItemCard(
                            snippet = snippet,
                            colors = colors,
                            onSelect = { onSnippetSelected(snippet.content) },
                            onTogglePin = { onTogglePin(snippet) },
                            onDelete = { onDeleteSnippet(snippet.id) },
                            testTagPrefix = "pinned_item"
                        )
                    }
                }
            }

            // SECTION 2: CLIPBOARD HISTORY
            if (selectedTab == ClipboardTab.ALL || selectedTab == ClipboardTab.HISTORY) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionHeader(
                        icon = Icons.Default.History,
                        title = "CLIPBOARD HISTORY (${historySnippets.size})",
                        colors = colors
                    )
                }

                if (historySnippets.isEmpty()) {
                    item {
                        EmptySectionPlaceholder(
                            text = "Clipboard history is empty. Text copied anywhere on your device will automatically save here!",
                            colors = colors
                        )
                    }
                } else {
                    items(historySnippets, key = { "history_${it.id}" }) { snippet ->
                        SnippetItemCard(
                            snippet = snippet,
                            colors = colors,
                            onSelect = { onSnippetSelected(snippet.content) },
                            onTogglePin = { onTogglePin(snippet) },
                            onDelete = { onDeleteSnippet(snippet.id) },
                            testTagPrefix = "history_item"
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        SnippetEditorDialog(
            colors = colors,
            initialTitle = "",
            initialContent = "",
            initialCategory = "Pinned",
            initialPinned = true,
            initialShortcut = "",
            dialogTitle = "Add Pinned Message",
            onDismiss = { showAddDialog = false },
            onConfirm = { title, content, isPinned, category, shortcut ->
                onSaveSnippet(title, content, isPinned, category, shortcut)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TabChip(
    label: String,
    isSelected: Boolean,
    colors: KeyboardColors,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) colors.enterKeyBackground else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) colors.enterKeyTextColor else colors.letterKeySecondaryTextColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    colors: KeyboardColors
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.enterKeyBackground,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = colors.enterKeyBackground,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptySectionPlaceholder(
    text: String,
    colors: KeyboardColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = colors.letterKeySecondaryTextColor,
            fontSize = 11.sp,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
private fun SnippetItemCard(
    snippet: SnippetEntity,
    colors: KeyboardColors,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    testTagPrefix: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .testTag("${testTagPrefix}_${snippet.id}"),
        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (snippet.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = colors.enterKeyBackground,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = snippet.title,
                        color = colors.letterKeyTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (snippet.shortcut.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.enterKeyBackground.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = snippet.shortcut,
                                color = colors.enterKeyBackground,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = snippet.content,
                    color = colors.letterKeySecondaryTextColor,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (snippet.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (snippet.isPinned) "Unpin message" else "Pin to top",
                        tint = if (snippet.isPinned) colors.enterKeyBackground else colors.toolbarIconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
