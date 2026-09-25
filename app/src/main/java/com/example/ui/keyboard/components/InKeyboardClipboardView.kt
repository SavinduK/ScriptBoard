package com.example.ui.keyboard.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.SnippetEntity
import com.example.ui.keyboard.model.KeyboardColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ClipboardTab {
    ALL, PINNED, HISTORY, IMAGES
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

    // Auto-capture current system clipboard (text and images)
    LaunchedEffect(Unit) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (cm != null && cm.hasPrimaryClip()) {
                val clip = cm.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val item = clip.getItemAt(0)
                    val uri = item.uri
                    if (uri != null && (clip.description.hasMimeType("image/*") || uri.toString().contains("image") || uri.scheme == "content")) {
                        val uriStr = uri.toString()
                        val alreadyInHistory = historySnippets.any { it.imageUri == uriStr || it.content == uriStr } ||
                                pinnedSnippets.any { it.imageUri == uriStr || it.content == uriStr }
                        if (!alreadyInHistory) {
                            val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())
                            onSaveSnippet("Image ($dateStr)", uriStr, false, "Images", "")
                        }
                    } else {
                        val text = item.text?.toString()
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
            }
        } catch (_: Exception) {}
    }

    val imageSnippets = remember(pinnedSnippets, historySnippets) {
        (pinnedSnippets + historySnippets).filter { it.isImage || !it.imageUri.isNullOrBlank() }
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

                Spacer(modifier = Modifier.width(6.dp))

                // Tabs: All, Pinned, History, Images
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
                    TabChip(
                        label = "Images (${imageSnippets.size})",
                        isSelected = selectedTab == ClipboardTab.IMAGES,
                        colors = colors,
                        onClick = { selectedTab = ClipboardTab.IMAGES },
                        testTag = "tab_clipboard_images"
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

        // Snippet & Clipboard List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            // IMAGES TAB
            if (selectedTab == ClipboardTab.IMAGES) {
                item {
                    SectionHeader(
                        icon = Icons.Default.Image,
                        title = "IMAGE CLIPS (${imageSnippets.size})",
                        colors = colors
                    )
                }
                if (imageSnippets.isEmpty()) {
                    item {
                        EmptySectionPlaceholder(
                            text = "No image clips yet. Copy an image anywhere or add one from Saved Clipboard page!",
                            colors = colors
                        )
                    }
                } else {
                    items(imageSnippets, key = { "img_${it.id}" }) { snippet ->
                        SnippetItemCard(
                            snippet = snippet,
                            colors = colors,
                            onSelect = {
                                copyImageClip(context, snippet)
                            },
                            onTogglePin = { onTogglePin(snippet) },
                            onDelete = { onDeleteSnippet(snippet.id) },
                            testTagPrefix = "image_item"
                        )
                    }
                }
            }

            // PINNED MESSAGES SECTION
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
                            onSelect = {
                                if (snippet.isImage || !snippet.imageUri.isNullOrBlank()) {
                                    copyImageClip(context, snippet)
                                } else {
                                    onSnippetSelected(snippet.content)
                                }
                            },
                            onTogglePin = { onTogglePin(snippet) },
                            onDelete = { onDeleteSnippet(snippet.id) },
                            testTagPrefix = "pinned_item"
                        )
                    }
                }
            }

            // CLIPBOARD HISTORY SECTION
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
                            text = "Clipboard history is empty. Copied text and images will appear here automatically.",
                            colors = colors
                        )
                    }
                } else {
                    items(historySnippets, key = { "history_${it.id}" }) { snippet ->
                        SnippetItemCard(
                            snippet = snippet,
                            colors = colors,
                            onSelect = {
                                if (snippet.isImage || !snippet.imageUri.isNullOrBlank()) {
                                    copyImageClip(context, snippet)
                                } else {
                                    onSnippetSelected(snippet.content)
                                }
                            },
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

private fun copyImageClip(context: Context, snippet: SnippetEntity) {
    try {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val uriStr = snippet.imageUri ?: snippet.content
        val uri = Uri.parse(uriStr)
        val clip = ClipData.newUri(context.contentResolver, "Image Clip", uri)
        cm?.setPrimaryClip(clip)
        Toast.makeText(context, "Image copied to clipboard! Ready to paste.", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Could not copy image to clipboard", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    colors: KeyboardColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.enterKeyBackground,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = title,
            color = colors.letterKeySecondaryTextColor,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun EmptySectionPlaceholder(
    text: String,
    colors: KeyboardColors
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.functionKeyBackground.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colors.letterKeySecondaryTextColor,
            fontSize = 11.5.sp,
            lineHeight = 15.sp
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
private fun SnippetItemCard(
    snippet: SnippetEntity,
    colors: KeyboardColors,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    testTagPrefix: String
) {
    val isImageClip = snippet.isImage || !snippet.imageUri.isNullOrBlank()

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
            if (isImageClip) {
                // Image preview layout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.functionKeyBackground)
                            .border(1.dp, colors.enterKeyBackground.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = snippet.imageUri ?: snippet.content,
                            contentDescription = snippet.title,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
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
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(colors.enterKeyBackground.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "IMAGE",
                                    color = colors.enterKeyBackground,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tap to copy image",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                // Text clip layout
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
