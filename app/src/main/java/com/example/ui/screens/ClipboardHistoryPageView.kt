package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.keyboard.components.SnippetEditorDialog
import com.example.ui.keyboard.model.KeyboardColors

private enum class HistoryFilter {
    ALL, PINNED, HISTORY, IMAGES
}

@Composable
fun ClipboardHistoryPageView(
    pinnedSnippets: List<SnippetEntity>,
    historySnippets: List<SnippetEntity>,
    colors: KeyboardColors,
    onBack: () -> Unit,
    onTogglePin: (SnippetEntity) -> Unit,
    onDeleteSnippet: (Long) -> Unit,
    onSaveSnippet: (title: String, content: String, isPinned: Boolean, category: String, shortcut: String) -> Unit,
    onSaveImage: (uri: String) -> Unit = {},
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var copiedFeedbackId by remember { mutableStateOf<Long?>(null) }

    // Photo picker for adding images to clipboard (compliance with Play policy zero-permission picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSaveImage(uri.toString())
            Toast.makeText(context, "Image added to clipboard history!", Toast.LENGTH_SHORT).show()
        }
    }

    val allList = remember(pinnedSnippets, historySnippets) { pinnedSnippets + historySnippets }

    val filteredPinned = remember(pinnedSnippets, searchQuery) {
        if (searchQuery.isBlank()) pinnedSnippets
        else pinnedSnippets.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.content.contains(searchQuery, ignoreCase = true) ||
                    it.shortcut.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredHistory = remember(historySnippets, searchQuery) {
        if (searchQuery.isBlank()) historySnippets
        else historySnippets.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.content.contains(searchQuery, ignoreCase = true) ||
                    it.shortcut.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredImages = remember(allList, searchQuery) {
        val images = allList.filter { it.isImage || !it.imageUri.isNullOrBlank() }
        if (searchQuery.isBlank()) images
        else images.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(colors.toolbarBackground)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_back_from_clipboard_page")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = colors.letterKeyTextColor
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Text(
                        text = "Saved Clipboard & Pinned",
                        color = colors.letterKeyTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${pinnedSnippets.size} pinned • ${historySnippets.size} history clips • ${filteredImages.size} images",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (historySnippets.isNotEmpty()) {
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("btn_page_clear_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear History",
                            tint = colors.toolbarIconTint
                        )
                    }
                }

                // Add Image button (Photo Picker)
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("btn_page_add_image")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Add Image to Clipboard",
                        tint = colors.enterKeyBackground
                    )
                }

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("btn_page_add_snippet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Snippet",
                        tint = colors.enterKeyBackground
                    )
                }
            }
        }

        // Search Bar & Filter Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_clipboard"),
                placeholder = {
                    Text(
                        text = "Search text, shortcuts, or images...",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.letterKeySecondaryTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.enterKeyBackground,
                    unfocusedBorderColor = colors.functionKeyBackground,
                    focusedTextColor = colors.letterKeyTextColor,
                    unfocusedTextColor = colors.letterKeyTextColor
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTabButton(
                    label = "All (${pinnedSnippets.size + historySnippets.size})",
                    isSelected = selectedFilter == HistoryFilter.ALL,
                    colors = colors,
                    onClick = { selectedFilter = HistoryFilter.ALL }
                )
                FilterTabButton(
                    label = "📌 Pinned (${pinnedSnippets.size})",
                    isSelected = selectedFilter == HistoryFilter.PINNED,
                    colors = colors,
                    onClick = { selectedFilter = HistoryFilter.PINNED }
                )
                FilterTabButton(
                    label = "🕒 History (${historySnippets.size})",
                    isSelected = selectedFilter == HistoryFilter.HISTORY,
                    colors = colors,
                    onClick = { selectedFilter = HistoryFilter.HISTORY }
                )
                FilterTabButton(
                    label = "🖼️ Images (${filteredImages.size})",
                    isSelected = selectedFilter == HistoryFilter.IMAGES,
                    colors = colors,
                    onClick = { selectedFilter = HistoryFilter.IMAGES }
                )
            }
        }

        // List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // IMAGES SECTION (when Images tab selected)
            if (selectedFilter == HistoryFilter.IMAGES) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = colors.enterKeyBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SAVED IMAGES (${filteredImages.size})",
                            color = colors.enterKeyBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (filteredImages.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "No image clips yet. Tap the image icon in the top right to add photos from your gallery!",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(filteredImages, key = { "page_img_${it.id}" }) { snippet ->
                        SnippetDetailCard(
                            snippet = snippet,
                            colors = colors,
                            isCopied = copiedFeedbackId == snippet.id,
                            onCopy = {
                                copyToSystemClipboard(context, clipboardManager, snippet)
                                copiedFeedbackId = snippet.id
                            },
                            onTogglePin = { onTogglePin(snippet) },
                            onDelete = { onDeleteSnippet(snippet.id) }
                        )
                    }
                }
            }

            // Pinned section
            if (selectedFilter == HistoryFilter.ALL || selectedFilter == HistoryFilter.PINNED) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = colors.enterKeyBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PINNED MESSAGES (${filteredPinned.size})",
                            color = colors.enterKeyBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (filteredPinned.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No pinned items match \"$searchQuery\""
                                else "No pinned messages yet. Tap the + icon or pin from history below to save frequent text and images!",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(filteredPinned, key = { "page_pinned_${it.id}" }) { snippet ->
                        SnippetDetailCard(
                            snippet = snippet,
                            colors = colors,
                            isCopied = copiedFeedbackId == snippet.id,
                            onCopy = {
                                copyToSystemClipboard(context, clipboardManager, snippet)
                                copiedFeedbackId = snippet.id
                            },
                            onTogglePin = { onTogglePin(snippet) },
                            onDelete = { onDeleteSnippet(snippet.id) }
                        )
                    }
                }
            }

            // Clipboard History section
            if (selectedFilter == HistoryFilter.ALL || selectedFilter == HistoryFilter.HISTORY) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = colors.enterKeyBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CLIPBOARD HISTORY (${filteredHistory.size})",
                            color = colors.enterKeyBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (filteredHistory.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No history clips match \"$searchQuery\""
                                else "Clipboard history is empty. Anything you copy on your phone (text or image) is automatically preserved here!",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(filteredHistory, key = { "page_history_${it.id}" }) { snippet ->
                        SnippetDetailCard(
                            snippet = snippet,
                            colors = colors,
                            isCopied = copiedFeedbackId == snippet.id,
                            onCopy = {
                                copyToSystemClipboard(context, clipboardManager, snippet)
                                copiedFeedbackId = snippet.id
                            },
                            onTogglePin = { onTogglePin(snippet) },
                            onDelete = { onDeleteSnippet(snippet.id) }
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

private fun copyToSystemClipboard(context: Context, clipboardManager: ClipboardManager, snippet: SnippetEntity) {
    try {
        if (snippet.isImage || !snippet.imageUri.isNullOrBlank()) {
            val uriStr = snippet.imageUri ?: snippet.content
            val clip = ClipData.newUri(context.contentResolver, "Image Clip", Uri.parse(uriStr))
            clipboardManager.setPrimaryClip(clip)
            Toast.makeText(context, "Image copied to clipboard!", Toast.LENGTH_SHORT).show()
        } else {
            val clip = ClipData.newPlainText("KeyPro Text", snippet.content)
            clipboardManager.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
    } catch (_: Exception) {}
}

@Composable
private fun FilterTabButton(
    label: String,
    isSelected: Boolean,
    colors: KeyboardColors,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) colors.enterKeyBackground else colors.letterKeyBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) colors.enterKeyTextColor else colors.letterKeyTextColor,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SnippetDetailCard(
    snippet: SnippetEntity,
    colors: KeyboardColors,
    isCopied: Boolean,
    onCopy: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val isImageClip = snippet.isImage || !snippet.imageUri.isNullOrBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (snippet.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = colors.enterKeyBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = snippet.title,
                        color = colors.letterKeyTextColor,
                        fontSize = 14.sp,
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
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = snippet.shortcut,
                                color = colors.enterKeyBackground,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (isImageClip) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.enterKeyBackground.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "IMAGE",
                                color = colors.enterKeyBackground,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy to clipboard",
                            tint = if (isCopied) colors.enterKeyBackground else colors.toolbarIconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (snippet.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (snippet.isPinned) "Unpin" else "Pin",
                            tint = if (snippet.isPinned) colors.enterKeyBackground else colors.toolbarIconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isImageClip) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.functionKeyBackground)
                        .border(1.dp, colors.functionKeyBackground, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = snippet.imageUri ?: snippet.content,
                        contentDescription = snippet.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Text(
                    text = snippet.content,
                    color = colors.letterKeySecondaryTextColor,
                    fontSize = 13.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isCopied) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isImageClip) "✓ Image copied to clipboard!" else "✓ Copied to clipboard!",
                    color = colors.enterKeyBackground,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
