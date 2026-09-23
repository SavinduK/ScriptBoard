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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
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

@Composable
fun InKeyboardClipboardView(
    snippets: List<SnippetEntity>,
    colors: KeyboardColors,
    onSnippetSelected: (String) -> Unit,
    onTogglePin: (SnippetEntity) -> Unit,
    onDeleteSnippet: (Long) -> Unit,
    onSaveSnippet: (title: String, content: String, isPinned: Boolean, category: String) -> Unit,
    onBackToLetters: () -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    var lastCopiedText by remember { mutableStateOf<String?>(null) }
    var copiedItemSaved by remember { mutableStateOf(false) }

    // Read latest system clipboard content
    LaunchedEffect(Unit) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (cm != null && cm.hasPrimaryClip()) {
                val clip = cm.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).text?.toString()
                    if (!text.isNullOrBlank()) {
                        lastCopiedText = text
                    }
                }
            }
        } catch (_: Exception) {}
    }

    val isAlreadyInSnippets = lastCopiedText != null && snippets.any { it.content == lastCopiedText }

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
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.enterKeyBackground)
                        .clickable(onClick = onBackToLetters)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
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
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Clipboard Manager",
                    color = colors.letterKeyTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("btn_add_snippet_inline")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Snippet",
                    tint = colors.enterKeyBackground
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            // Section 1: Last Copied Content in Clipboard
            item {
                Text(
                    text = "LAST COPIED CONTENT",
                    color = colors.enterKeyBackground,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                )

                if (lastCopiedText != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.enterKeyBackground.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSnippetSelected(lastCopiedText!!) }
                            .testTag("last_copied_clipboard_card"),
                        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Copied",
                                        tint = colors.enterKeyBackground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Current Clipboard",
                                        color = colors.letterKeyTextColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Save / Pin action
                                if (copiedItemSaved || isAlreadyInSnippets) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.enterKeyBackground.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Saved",
                                            tint = colors.enterKeyBackground,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Saved",
                                            color = colors.enterKeyBackground,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.enterKeyBackground)
                                            .clickable {
                                                val snippetTitle = if (lastCopiedText!!.length > 25) {
                                                    lastCopiedText!!.take(25) + "..."
                                                } else {
                                                    lastCopiedText!!
                                                }
                                                onSaveSnippet(snippetTitle, lastCopiedText!!, true, "Clipboard")
                                                copiedItemSaved = true
                                            }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                            .testTag("btn_save_last_copied"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.BookmarkAdd,
                                                contentDescription = "Save Snippet",
                                                tint = colors.enterKeyTextColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Save",
                                                color = colors.enterKeyTextColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = lastCopiedText!!,
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Tap to paste into active field",
                                color = colors.enterKeyBackground,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "No recent text copied yet. Copy any text to see it here.",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "SAVED & PINNED (${snippets.size})",
                    color = colors.letterKeySecondaryTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                )
            }

            // Section 2: Saved and Pinned Snippets
            if (snippets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved snippets. Tap \"Save\" above or + to pin snippets!",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(snippets, key = { it.id }) { snippet ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSnippetSelected(snippet.content) }
                            .testTag("inline_snippet_${snippet.id}"),
                        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
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
                                            modifier = Modifier.size(14.dp)
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
                                Text(
                                    text = snippet.content,
                                    color = colors.letterKeySecondaryTextColor,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onTogglePin(snippet) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (snippet.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = "Toggle Pin",
                                        tint = if (snippet.isPinned) colors.enterKeyBackground else colors.toolbarIconTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteSnippet(snippet.id) },
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
            dialogTitle = "Add Pinned Snippet",
            onDismiss = { showAddDialog = false },
            onConfirm = { title, content, isPinned, category ->
                onSaveSnippet(title, content, isPinned, category)
                showAddDialog = false
            }
        )
    }
}
