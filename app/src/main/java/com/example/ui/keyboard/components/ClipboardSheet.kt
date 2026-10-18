package com.example.ui.keyboard.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SnippetEntity
import com.example.ui.keyboard.model.KeyboardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardSheet(
    snippets: List<SnippetEntity>,
    colors: KeyboardColors,
    onDismiss: () -> Unit,
    onSnippetSelected: (String) -> Unit,
    onTogglePin: (SnippetEntity) -> Unit,
    onDeleteSnippet: (Long) -> Unit,
    onSaveSnippet: (title: String, content: String, isPinned: Boolean, category: String, shortcut: String) -> Unit,
    onClearHistory: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSnippet by remember { mutableStateOf<SnippetEntity?>(null) }

    val filteredSnippets = snippets.filter { snippet ->
        val matchesQuery = searchQuery.isBlank() ||
                snippet.title.contains(searchQuery, ignoreCase = true) ||
                snippet.content.contains(searchQuery, ignoreCase = true)
        val matchesCategory = when (selectedFilter) {
            "Pinned" -> snippet.isPinned
            "History" -> !snippet.isPinned
            "All" -> true
            else -> snippet.category.equals(selectedFilter, ignoreCase = true)
        }
        matchesQuery && matchesCategory
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Clipboard & Snippets",
                        tint = colors.enterKeyBackground,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clipboard & Pinned Snippets",
                        color = colors.letterKeyTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("btn_add_snippet")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Snippet",
                            tint = colors.enterKeyBackground
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_clipboard")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.toolbarIconTint
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("search_snippets_field"),
                placeholder = {
                    Text(
                        text = "Search pinned snippets...",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.toolbarIconTint
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = colors.toolbarIconTint
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.letterKeyTextColor,
                    unfocusedTextColor = colors.letterKeyTextColor,
                    focusedBorderColor = colors.enterKeyBackground,
                    unfocusedBorderColor = colors.functionKeyBackground,
                    focusedContainerColor = colors.functionKeyBackground,
                    unfocusedContainerColor = colors.functionKeyBackground
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Chips Row
            val categories = listOf("All", "Pinned", "History", "Work", "Messages", "Code", "Personal")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedFilter == cat,
                        onClick = { selectedFilter = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.enterKeyBackground,
                            selectedLabelColor = colors.enterKeyTextColor,
                            containerColor = colors.functionKeyBackground,
                            labelColor = colors.functionKeyTextColor
                        ),
                        border = null,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Snippet List
            if (filteredSnippets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No snippets found",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap + to create a custom snippet to pin!",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredSnippets, key = { it.id }) { snippet ->
                        SnippetItemCard(
                            snippet = snippet,
                            colors = colors,
                            onSelect = {
                                onSnippetSelected(snippet.content)
                                onDismiss()
                            },
                            onTogglePin = { onTogglePin(snippet) },
                            onDelete = { onDeleteSnippet(snippet.id) },
                            onEdit = { editingSnippet = snippet }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Add New Snippet Dialog
    if (showAddDialog) {
        SnippetEditorDialog(
            colors = colors,
            initialTitle = "",
            initialContent = "",
            initialCategory = "Quick Text",
            initialPinned = true,
            initialShortcut = "",
            dialogTitle = "New Pinned Snippet",
            onDismiss = { showAddDialog = false },
            onConfirm = { title, content, isPinned, category, shortcut ->
                onSaveSnippet(title, content, isPinned, category, shortcut)
                showAddDialog = false
            }
        )
    }

    // Edit Snippet Dialog
    editingSnippet?.let { snippet ->
        SnippetEditorDialog(
            colors = colors,
            initialTitle = snippet.title,
            initialContent = snippet.content,
            initialCategory = snippet.category,
            initialPinned = snippet.isPinned,
            initialShortcut = snippet.shortcut,
            dialogTitle = "Edit Snippet",
            onDismiss = { editingSnippet = null },
            onConfirm = { title, content, isPinned, category, shortcut ->
                onSaveSnippet(title, content, isPinned, category, shortcut)
                onDeleteSnippet(snippet.id) // replace
                editingSnippet = null
            }
        )
    }
}

@Composable
fun SnippetItemCard(
    snippet: SnippetEntity,
    colors: KeyboardColors,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .testTag("snippet_item_${snippet.id}"),
        colors = CardDefaults.cardColors(
            containerColor = colors.letterKeyBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
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
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colors.enterKeyBackground.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "Pinned",
                                tint = colors.enterKeyBackground,
                                modifier = Modifier.size(14.dp)
                            )
                        }
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.functionKeyBackground)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = snippet.category,
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(28.dp).testTag("btn_pin_${snippet.id}")
                    ) {
                        Icon(
                            imageVector = if (snippet.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (snippet.isPinned) "Unpin" else "Pin",
                            tint = if (snippet.isPinned) colors.enterKeyBackground else colors.toolbarIconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp).testTag("btn_edit_${snippet.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = colors.toolbarIconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp).testTag("btn_delete_${snippet.id}")
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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = snippet.content,
                color = colors.letterKeySecondaryTextColor,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SnippetEditorDialog(
    colors: KeyboardColors,
    initialTitle: String,
    initialContent: String,
    initialCategory: String,
    initialPinned: Boolean,
    initialShortcut: String = "",
    dialogTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, isPinned: Boolean, category: String, shortcut: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    var category by remember { mutableStateOf(initialCategory) }
    var shortcut by remember { mutableStateOf(initialShortcut) }
    var isPinned by remember { mutableStateOf(initialPinned) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        title = {
            Text(
                text = dialogTitle,
                color = colors.letterKeyTextColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Snippet Title", color = colors.letterKeySecondaryTextColor) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.letterKeyTextColor,
                        unfocusedTextColor = colors.letterKeyTextColor,
                        focusedBorderColor = colors.enterKeyBackground,
                        unfocusedBorderColor = colors.functionKeyBackground
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = shortcut,
                    onValueChange = { shortcut = it },
                    label = { Text("Shortcut Phrase (e.g. @email, @sig)", color = colors.letterKeySecondaryTextColor) },
                    placeholder = { Text("@email", color = colors.letterKeySecondaryTextColor.copy(alpha = 0.5f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.letterKeyTextColor,
                        unfocusedTextColor = colors.letterKeyTextColor,
                        focusedBorderColor = colors.enterKeyBackground,
                        unfocusedBorderColor = colors.functionKeyBackground
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Snippet Content / Text", color = colors.letterKeySecondaryTextColor) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.letterKeyTextColor,
                        unfocusedTextColor = colors.letterKeyTextColor,
                        focusedBorderColor = colors.enterKeyBackground,
                        unfocusedBorderColor = colors.functionKeyBackground
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Work, Code, Replies)", color = colors.letterKeySecondaryTextColor) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.letterKeyTextColor,
                        unfocusedTextColor = colors.letterKeyTextColor,
                        focusedBorderColor = colors.enterKeyBackground,
                        unfocusedBorderColor = colors.functionKeyBackground
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isPinned = !isPinned }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin toggle",
                        tint = if (isPinned) colors.enterKeyBackground else colors.toolbarIconTint
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPinned) "Pinned for quick access" else "Unpinned",
                        color = colors.letterKeyTextColor,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        val cleanShortcut = if (shortcut.isNotBlank() && !shortcut.startsWith("@")) "@$shortcut" else shortcut.trim()
                        onConfirm(title.ifBlank { content.take(15) }, content, isPinned, category.ifBlank { "General" }, cleanShortcut)
                    }
                },
                enabled = content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.enterKeyBackground,
                    contentColor = colors.enterKeyTextColor
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.letterKeySecondaryTextColor)
            }
        }
    )
}
