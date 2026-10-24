package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.keyboard.model.KeyboardColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ClipboardPageSection {
    SHORTCUTS,
    HISTORY,
    PINNED
}

@Composable
fun ClipboardHistoryPageView(
    pinnedSnippets: List<SnippetEntity>,
    historySnippets: List<SnippetEntity>,
    colors: KeyboardColors,
    autoDeleteEnabled: Boolean = false,
    retentionDays: Int = 7,
    customCategories: Set<String> = setOf("Work", "Personal", "Travel", "Study"),
    onToggleAutoDelete: (Boolean) -> Unit = {},
    onChangeRetentionDays: (Int) -> Unit = {},
    onPurgeExpiredNow: () -> Unit = {},
    onNavigateToConnectedDevices: () -> Unit = {},
    onBack: () -> Unit,
    onTogglePin: (SnippetEntity) -> Unit,
    onPinWithCategory: (SnippetEntity, String) -> Unit = { _, _ -> },
    onUnpin: (SnippetEntity) -> Unit = {},
    onAddCustomCategory: (String) -> Unit = {},
    onDeleteSnippet: (Long) -> Unit,
    onSaveSnippet: (title: String, content: String, isPinned: Boolean, category: String, shortcut: String) -> Unit,
    onSaveImage: (uri: String) -> Unit = {},
    onRemoveDuplicates: () -> Unit = {},
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var currentSection by remember { mutableStateOf(ClipboardPageSection.HISTORY) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Dialog & Modal states
    var showAutoDeleteModal by remember { mutableStateOf(false) }
    var showAddShortcutDialog by remember { mutableStateOf(false) }
    var showAddPinnedDialog by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var itemToPinWithCategory by remember { mutableStateOf<SnippetEntity?>(null) }
    var copiedFeedbackId by remember { mutableStateOf<Long?>(null) }

    // Photo picker for adding images directly to clipboard history
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val persistentPath = com.example.data.ClipboardImageHelper.persistImageLocally(context, uri.toString())
            onSaveImage(persistentPath)
            Toast.makeText(context, "Image added to clipboard history!", Toast.LENGTH_SHORT).show()
        }
    }

    // Split pinnedSnippets into Shortcuts and Pinned Messages
    // Shortcuts: items with non-blank shortcut phrase
    val shortcutsList = remember(pinnedSnippets) {
        pinnedSnippets.filter { it.shortcut.isNotBlank() }
    }

    // Pinned messages: items that are pinned (and not pure shortcuts)
    val pinnedMessagesList = remember(pinnedSnippets) {
        pinnedSnippets.filter { it.shortcut.isBlank() }
    }

    // Search filters
    val filteredShortcuts = remember(shortcutsList, searchQuery) {
        if (searchQuery.isBlank()) shortcutsList
        else shortcutsList.filter {
            it.shortcut.contains(searchQuery, ignoreCase = true) ||
            it.content.contains(searchQuery, ignoreCase = true) ||
            it.title.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredHistory = remember(historySnippets, searchQuery) {
        if (searchQuery.isBlank()) historySnippets
        else historySnippets.filter {
            it.content.contains(searchQuery, ignoreCase = true) ||
            it.title.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredPinned = remember(pinnedMessagesList, searchQuery, selectedCategoryFilter) {
        pinnedMessagesList.filter { item ->
            val matchesCategory = (selectedCategoryFilter == "All") || item.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.content.contains(searchQuery, ignoreCase = true) ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 1. TOP APP BAR
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            color = colors.toolbarBackground,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
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
                            contentDescription = "Back",
                            tint = colors.letterKeyTextColor
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = when (currentSection) {
                                ClipboardPageSection.SHORTCUTS -> "Keyboard Shortcuts"
                                ClipboardPageSection.HISTORY -> "Clipboard History"
                                ClipboardPageSection.PINNED -> "Pinned Messages"
                            },
                            color = colors.letterKeyTextColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${shortcutsList.size} shortcuts • ${historySnippets.size} clips • ${pinnedMessagesList.size} pinned",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 11.5.sp
                        )
                    }
                }

                // Section-specific top bar action buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (currentSection) {
                        ClipboardPageSection.SHORTCUTS -> {
                            // Section 1 requirement: Show add shortcut icon on header
                            IconButton(
                                onClick = { showAddShortcutDialog = true },
                                modifier = Modifier.testTag("btn_add_shortcut_header")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Shortcut",
                                    tint = colors.enterKeyBackground
                                )
                            }
                        }
                        ClipboardPageSection.HISTORY -> {
                            // Section 2 requirements: Add Photo, Auto-Delete modal trigger
                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("btn_history_add_image")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Add Image",
                                    tint = colors.enterKeyBackground
                                )
                            }

                            IconButton(
                                onClick = { showAutoDeleteModal = true },
                                modifier = Modifier.testTag("btn_header_auto_delete_modal")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoDelete,
                                    contentDescription = "Auto-Delete Settings",
                                    tint = if (autoDeleteEnabled) colors.enterKeyBackground else colors.letterKeySecondaryTextColor
                                )
                            }
                        }
                        ClipboardPageSection.PINNED -> {
                            // Section 3: Add pinned message
                            IconButton(
                                onClick = { showAddPinnedDialog = true },
                                modifier = Modifier.testTag("btn_add_pinned_message_header")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Pinned Message",
                                    tint = colors.enterKeyBackground
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. SEARCH BAR
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
                        text = when (currentSection) {
                            ClipboardPageSection.SHORTCUTS -> "Search shortcuts (e.g. @email, brb)..."
                            ClipboardPageSection.HISTORY -> "Search clipboard text & images..."
                            ClipboardPageSection.PINNED -> "Search pinned messages..."
                        },
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
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = colors.letterKeySecondaryTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
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

            // 3. TOP HORIZONTAL CHIP BAR (User requirement: 3 separate sections)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionTabChip(
                    title = "Shortcuts (${shortcutsList.size})",
                    isSelected = currentSection == ClipboardPageSection.SHORTCUTS,
                    colors = colors,
                    onClick = { currentSection = ClipboardPageSection.SHORTCUTS },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_section_shortcuts")
                )

                SectionTabChip(
                    title = "History (${historySnippets.size})",
                    isSelected = currentSection == ClipboardPageSection.HISTORY,
                    colors = colors,
                    onClick = { currentSection = ClipboardPageSection.HISTORY },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_section_history")
                )

                SectionTabChip(
                    title = "Pinned (${pinnedMessagesList.size})",
                    isSelected = currentSection == ClipboardPageSection.PINNED,
                    colors = colors,
                    onClick = { currentSection = ClipboardPageSection.PINNED },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_section_pinned")
                )
            }
        }

        // 4. MAIN CONTENT AREA (Based on active section)
        when (currentSection) {
            // ==========================================
            // SECTION 1: SHORTCUTS
            // ==========================================
            ClipboardPageSection.SHORTCUTS -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
                ) {
                    // Header row with title and Add Shortcut icon only
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = null,
                                    tint = colors.enterKeyBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "KEYBOARD SHORTCUTS (${filteredShortcuts.size})",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.letterKeySecondaryTextColor
                                )
                            }

                            // Show Add Shortcut icon on header
                            IconButton(
                                onClick = { showAddShortcutDialog = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("btn_add_shortcut_section_header")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Shortcut",
                                    tint = colors.enterKeyBackground,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    if (filteredShortcuts.isEmpty()) {
                        item {
                            EmptyShortcutsCard(colors = colors, onAddClick = { showAddShortcutDialog = true })
                        }
                    } else {
                        items(filteredShortcuts, key = { it.id }) { shortcutItem ->
                            ShortcutItemCard(
                                snippet = shortcutItem,
                                colors = colors,
                                isCopied = copiedFeedbackId == shortcutItem.id,
                                onCopy = {
                                    clipboardManager.setPrimaryClip(ClipData.newPlainText(shortcutItem.shortcut, shortcutItem.content))
                                    copiedFeedbackId = shortcutItem.id
                                    Toast.makeText(context, "Copied: ${shortcutItem.content}", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = { onDeleteSnippet(shortcutItem.id) }
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 2: HISTORY (TEXT & IMAGES)
            // ==========================================
            ClipboardPageSection.HISTORY -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
                ) {
                    // Header with title and Auto-Delete button + Clean Duplicate button
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = colors.enterKeyBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CLIPBOARD HISTORY (${filteredHistory.size})",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.letterKeySecondaryTextColor
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Auto-Delete button in header: opens modal dialog
                                FilledTonalButton(
                                    onClick = { showAutoDeleteModal = true },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (autoDeleteEnabled) colors.enterKeyBackground.copy(alpha = 0.2f) else colors.functionKeyBackground.copy(alpha = 0.5f),
                                        contentColor = if (autoDeleteEnabled) colors.enterKeyBackground else colors.letterKeyTextColor
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag("btn_auto_delete_header_modal")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoDelete,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (autoDeleteEnabled) "Auto: ${retentionDays}d" else "Auto-Delete",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Remove Duplicates action
                                IconButton(
                                    onClick = onRemoveDuplicates,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("btn_remove_duplicates_history")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ClearAll,
                                        contentDescription = "Remove Duplicates",
                                        tint = colors.letterKeySecondaryTextColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (filteredHistory.isEmpty()) {
                        item {
                            EmptyHistoryCard(colors = colors)
                        }
                    } else {
                        items(filteredHistory, key = { it.id }) { historyItem ->
                            HistoryItemCard(
                                snippet = historyItem,
                                colors = colors,
                                isCopied = copiedFeedbackId == historyItem.id,
                                onCopy = {
                                    if (historyItem.isImage && !historyItem.imageUri.isNullOrBlank()) {
                                        Toast.makeText(context, "Image copied", Toast.LENGTH_SHORT).show()
                                    } else {
                                        clipboardManager.setPrimaryClip(ClipData.newPlainText(historyItem.title, historyItem.content))
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                    copiedFeedbackId = historyItem.id
                                },
                                onPinToCategory = {
                                    itemToPinWithCategory = historyItem
                                },
                                onDelete = { onDeleteSnippet(historyItem.id) }
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 3: PINNED (CUSTOM CATEGORIES)
            // ==========================================
            ClipboardPageSection.PINNED -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Category Filter Chips Bar
                    val allCategories = remember(customCategories) {
                        listOf("All") + customCategories.toList()
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        allCategories.forEach { categoryName ->
                            val isSelected = selectedCategoryFilter == categoryName
                            val count = if (categoryName == "All") pinnedMessagesList.size
                            else pinnedMessagesList.count { it.category.equals(categoryName, ignoreCase = true) }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) colors.enterKeyBackground
                                        else colors.letterKeyBackground
                                    )
                                    .clickable { selectedCategoryFilter = categoryName }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("chip_pinned_category_$categoryName")
                            ) {
                                Text(
                                    text = "$categoryName ($count)",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.enterKeyTextColor else colors.letterKeyTextColor
                                )
                            }
                        }

                        // Add Custom Category Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, colors.functionKeyBackground, RoundedCornerShape(8.dp))
                                .clickable { showNewCategoryDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("btn_add_custom_category")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = colors.enterKeyBackground,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Category",
                                    fontSize = 12.sp,
                                    color = colors.enterKeyBackground,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = null,
                                        tint = colors.enterKeyBackground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (selectedCategoryFilter == "All") "ALL PINNED MESSAGES (${filteredPinned.size})" else "${selectedCategoryFilter.uppercase()} (${filteredPinned.size})",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.letterKeySecondaryTextColor
                                    )
                                }

                                IconButton(
                                    onClick = { showAddPinnedDialog = true },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("btn_add_pinned_message_section_header")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "New Pinned Message",
                                        tint = colors.enterKeyBackground,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        if (filteredPinned.isEmpty()) {
                            item {
                                EmptyPinnedCard(
                                    category = selectedCategoryFilter,
                                    colors = colors,
                                    onAddClick = { showAddPinnedDialog = true }
                                )
                            }
                        } else {
                            items(filteredPinned, key = { it.id }) { pinnedItem ->
                                PinnedItemCard(
                                    snippet = pinnedItem,
                                    colors = colors,
                                    isCopied = copiedFeedbackId == pinnedItem.id,
                                    onCopy = {
                                        if (pinnedItem.isImage && !pinnedItem.imageUri.isNullOrBlank()) {
                                            Toast.makeText(context, "Image copied", Toast.LENGTH_SHORT).show()
                                        } else {
                                            clipboardManager.setPrimaryClip(ClipData.newPlainText(pinnedItem.title, pinnedItem.content))
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                        copiedFeedbackId = pinnedItem.id
                                    },
                                    onChangeCategory = {
                                        itemToPinWithCategory = pinnedItem
                                    },
                                    onUnpin = {
                                        onUnpin(pinnedItem)
                                    },
                                    onDelete = { onDeleteSnippet(pinnedItem.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // MODALS AND DIALOGS
    // ==========================================

    // 1. AUTO-DELETE MODAL (Opened from header button in History section)
    if (showAutoDeleteModal) {
        AutoDeleteModalDialog(
            autoDeleteEnabled = autoDeleteEnabled,
            retentionDays = retentionDays,
            colors = colors,
            onDismiss = { showAutoDeleteModal = false },
            onToggleAutoDelete = onToggleAutoDelete,
            onChangeRetentionDays = onChangeRetentionDays,
            onPurgeExpiredNow = onPurgeExpiredNow,
            onRemoveDuplicates = onRemoveDuplicates
        )
    }

    // 2. ADD SHORTCUT DIALOG
    if (showAddShortcutDialog) {
        AddShortcutModalDialog(
            colors = colors,
            onDismiss = { showAddShortcutDialog = false },
            onSave = { phrase, expansion ->
                onSaveSnippet(
                    phrase,
                    expansion,
                    true,
                    "Shortcuts",
                    phrase
                )
                showAddShortcutDialog = false
                Toast.makeText(context, "Shortcut '$phrase' saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 3. PIN WITH CATEGORY DIALOG (Pin from history or change category)
    itemToPinWithCategory?.let { snippet ->
        PinToCategoryModalDialog(
            snippet = snippet,
            categories = customCategories,
            colors = colors,
            onDismiss = { itemToPinWithCategory = null },
            onSelectCategory = { selectedCat ->
                onPinWithCategory(snippet, selectedCat)
                itemToPinWithCategory = null
                Toast.makeText(context, "Pinned to '$selectedCat'!", Toast.LENGTH_SHORT).show()
            },
            onAddCategory = onAddCustomCategory
        )
    }

    // 4. ADD PINNED MESSAGE DIALOG
    if (showAddPinnedDialog) {
        AddPinnedMessageModalDialog(
            categories = customCategories,
            colors = colors,
            onDismiss = { showAddPinnedDialog = false },
            onSave = { title, content, category ->
                onSaveSnippet(
                    title,
                    content,
                    true,
                    category,
                    ""
                )
                showAddPinnedDialog = false
                Toast.makeText(context, "Pinned message saved to '$category'!", Toast.LENGTH_SHORT).show()
            },
            onAddCategory = onAddCustomCategory
        )
    }

    // 5. NEW CATEGORY DIALOG
    if (showNewCategoryDialog) {
        NewCategoryModalDialog(
            colors = colors,
            onDismiss = { showNewCategoryDialog = false },
            onAdd = { newCat ->
                onAddCustomCategory(newCat)
                selectedCategoryFilter = newCat
                showNewCategoryDialog = false
            }
        )
    }
}

// ==========================================
// COMPOSABLE CARDS & COMPONENTS
// ==========================================

@Composable
private fun SectionTabChip(
    title: String,
    isSelected: Boolean,
    colors: KeyboardColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) colors.enterKeyBackground
                else colors.letterKeyBackground
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) Color.Transparent else colors.functionKeyBackground.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.enterKeyTextColor else colors.letterKeyTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ShortcutItemCard(
    snippet: SnippetEntity,
    colors: KeyboardColors,
    isCopied: Boolean,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_shortcut_${snippet.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shortcut Phrase Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.enterKeyBackground.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = snippet.shortcut,
                        color = colors.enterKeyBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_copy_shortcut_${snippet.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Expansion",
                            tint = if (isCopied) colors.enterKeyBackground else colors.letterKeyTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_delete_shortcut_${snippet.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Shortcut",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expanded Text Content
            Text(
                text = snippet.content,
                color = colors.letterKeyTextColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun HistoryItemCard(
    snippet: SnippetEntity,
    colors: KeyboardColors,
    isCopied: Boolean,
    onCopy: () -> Unit,
    onPinToCategory: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(snippet.timestamp) {
        val diff = System.currentTimeMillis() - snippet.timestamp
        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(snippet.timestamp))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_history_${snippet.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    fontSize = 11.5.sp,
                    color = colors.letterKeySecondaryTextColor
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pin to Category action button
                    IconButton(
                        onClick = onPinToCategory,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_pin_to_category_${snippet.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = "Pin to Category",
                            tint = colors.enterKeyBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Copy action button
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_copy_history_${snippet.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy to Clipboard",
                            tint = if (isCopied) colors.enterKeyBackground else colors.letterKeyTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete action button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_delete_history_${snippet.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Item",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (snippet.isImage && !snippet.imageUri.isNullOrBlank()) {
                // Image item preview
                AsyncImage(
                    model = snippet.imageUri,
                    contentDescription = "Clipboard Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Text item
                Text(
                    text = snippet.content,
                    color = colors.letterKeyTextColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PinnedItemCard(
    snippet: SnippetEntity,
    colors: KeyboardColors,
    isCopied: Boolean,
    onCopy: () -> Unit,
    onChangeCategory: () -> Unit,
    onUnpin: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_pinned_${snippet.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.functionKeyBackground.copy(alpha = 0.8f),
                    modifier = Modifier.clickable(onClick = onChangeCategory)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = colors.enterKeyBackground,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = snippet.category.ifBlank { "General" },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.letterKeyTextColor
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Copy
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = if (isCopied) colors.enterKeyBackground else colors.letterKeyTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Unpin
                    IconButton(
                        onClick = onUnpin,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Unpin",
                            tint = colors.enterKeyBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
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

            if (snippet.title.isNotBlank() && snippet.title != snippet.content) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = snippet.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = colors.letterKeyTextColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (snippet.isImage && !snippet.imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = snippet.imageUri,
                    contentDescription = "Pinned Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = snippet.content,
                    color = colors.letterKeyTextColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ==========================================
// MODAL DIALOGS
// ==========================================

@Composable
private fun AutoDeleteModalDialog(
    autoDeleteEnabled: Boolean,
    retentionDays: Int,
    colors: KeyboardColors,
    onDismiss: () -> Unit,
    onToggleAutoDelete: (Boolean) -> Unit,
    onChangeRetentionDays: (Int) -> Unit,
    onPurgeExpiredNow: () -> Unit,
    onRemoveDuplicates: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoDelete,
                    contentDescription = null,
                    tint = colors.enterKeyBackground,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Auto-Delete History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.letterKeyTextColor
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Switch row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Delete Unpinned History",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = colors.letterKeyTextColor
                        )
                        Text(
                            text = "Deletes unpinned clips older than selected retention period",
                            fontSize = 11.5.sp,
                            color = colors.letterKeySecondaryTextColor
                        )
                    }

                    Switch(
                        checked = autoDeleteEnabled,
                        onCheckedChange = onToggleAutoDelete,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.enterKeyBackground,
                            checkedTrackColor = colors.enterKeyBackground.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("switch_modal_auto_delete")
                    )
                }

                // Retention Days Selection
                if (autoDeleteEnabled) {
                    Text(
                        text = "Retention Period:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.letterKeyTextColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(7, 14, 28).forEach { days ->
                            val isSelected = retentionDays == days
                            FilledTonalButton(
                                onClick = { onChangeRetentionDays(days) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSelected) colors.enterKeyBackground else colors.functionKeyBackground,
                                    contentColor = if (isSelected) colors.enterKeyTextColor else colors.letterKeyTextColor
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("btn_retention_${days}d")
                            ) {
                                Text("$days Days", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPurgeExpiredNow,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clean Expired", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onRemoveDuplicates,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clean Duplicates", fontSize = 12.sp)
                    }
                }

                // Information Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.functionKeyBackground.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔒 Pinned snippets and keyboard shortcuts are protected and will never be deleted by auto-delete.",
                        fontSize = 11.5.sp,
                        color = colors.letterKeySecondaryTextColor,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground)
            ) {
                Text("Done", color = colors.enterKeyTextColor)
            }
        },
        containerColor = colors.toolbarBackground
    )
}

@Composable
private fun AddShortcutModalDialog(
    colors: KeyboardColors,
    onDismiss: () -> Unit,
    onSave: (phrase: String, expansion: String) -> Unit
) {
    var phrase by remember { mutableStateOf("@") }
    var expansion by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Keyboard Shortcut",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = colors.letterKeyTextColor
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text("Shortcut Trigger (e.g. @email, brb)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_shortcut_phrase")
                )

                OutlinedTextField(
                    value = expansion,
                    onValueChange = { expansion = it },
                    label = { Text("Full Text Expansion") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("input_shortcut_expansion")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phrase.isNotBlank() && expansion.isNotBlank()) {
                        onSave(phrase.trim(), expansion.trim())
                    }
                },
                enabled = phrase.isNotBlank() && expansion.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground)
            ) {
                Text("Save Shortcut", color = colors.enterKeyTextColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.letterKeySecondaryTextColor)
            }
        },
        containerColor = colors.toolbarBackground
    )
}

@Composable
private fun PinToCategoryModalDialog(
    snippet: SnippetEntity,
    categories: Set<String>,
    colors: KeyboardColors,
    onDismiss: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onAddCategory: (String) -> Unit
) {
    var newCatName by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pin to Category",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = colors.letterKeyTextColor
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select a category to pin this item into:",
                    fontSize = 13.sp,
                    color = colors.letterKeySecondaryTextColor
                )

                // Categories list
                val list = categories.toList()
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    list.forEach { cat ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectCategory(cat) }
                                .background(colors.letterKeyBackground)
                                .testTag("btn_select_category_$cat"),
                            color = colors.letterKeyBackground
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = colors.enterKeyBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = cat,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = colors.letterKeyTextColor
                                )
                            }
                        }
                    }
                }

                if (isAddingNew) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = newCatName,
                            onValueChange = { newCatName = it },
                            placeholder = { Text("New category name...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newCatName.isNotBlank()) {
                                    val cat = newCatName.trim()
                                    onAddCategory(cat)
                                    onSelectCategory(cat)
                                }
                            },
                            enabled = newCatName.isNotBlank()
                        ) {
                            Text("Add")
                        }
                    }
                } else {
                    TextButton(onClick = { isAddingNew = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Create New Category", color = colors.enterKeyBackground)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.letterKeySecondaryTextColor)
            }
        },
        containerColor = colors.toolbarBackground
    )
}

@Composable
private fun AddPinnedMessageModalDialog(
    categories: Set<String>,
    colors: KeyboardColors,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String) -> Unit,
    onAddCategory: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "Work") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Pinned Message",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = colors.letterKeyTextColor
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Summary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Message Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )

                Text(
                    text = "Category:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.letterKeySecondaryTextColor
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) colors.enterKeyBackground else colors.letterKeyBackground)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.enterKeyTextColor else colors.letterKeyTextColor
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        onSave(title.ifBlank { content.take(25) }, content.trim(), selectedCategory)
                    }
                },
                enabled = content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground)
            ) {
                Text("Pin Message", color = colors.enterKeyTextColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.letterKeySecondaryTextColor)
            }
        },
        containerColor = colors.toolbarBackground
    )
}

@Composable
private fun NewCategoryModalDialog(
    colors: KeyboardColors,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Category",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = colors.letterKeyTextColor
            )
        },
        text = {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category Name (e.g. Finance, Ideas)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_new_category_name")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        onAdd(categoryName.trim())
                    }
                },
                enabled = categoryName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground)
            ) {
                Text("Create Category", color = colors.enterKeyTextColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.letterKeySecondaryTextColor)
            }
        },
        containerColor = colors.toolbarBackground
    )
}

// ==========================================
// EMPTY STATES
// ==========================================

@Composable
private fun EmptyShortcutsCard(colors: KeyboardColors, onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Keyboard,
                contentDescription = null,
                tint = colors.enterKeyBackground,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "No Keyboard Shortcuts Yet",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colors.letterKeyTextColor
            )
            Text(
                text = "Shortcuts allow you to type trigger phrases (like '@email') on your keyboard to expand full text automatically.",
                fontSize = 12.5.sp,
                color = colors.letterKeySecondaryTextColor,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Your First Shortcut", color = colors.enterKeyTextColor)
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(colors: KeyboardColors) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = colors.enterKeyBackground,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "Clipboard History is Empty",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colors.letterKeyTextColor
            )
            Text(
                text = "Text and images you copy on your phone or keyboard will be automatically collected here without duplicates.",
                fontSize = 12.5.sp,
                color = colors.letterKeySecondaryTextColor,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun EmptyPinnedCard(category: String, colors: KeyboardColors, onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = colors.enterKeyBackground,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = if (category == "All") "No Pinned Messages" else "No Messages in '$category'",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colors.letterKeyTextColor
            )
            Text(
                text = "Pin important messages directly from your History into categories like Work, Travel, and Study, or create a new message.",
                fontSize = 12.5.sp,
                color = colors.letterKeySecondaryTextColor,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pin a Message", color = colors.enterKeyTextColor)
            }
        }
    }
}
