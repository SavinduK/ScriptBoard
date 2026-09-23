package com.example.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.keyboard.KeyProKeyboardView
import com.example.ui.keyboard.components.ClipboardSheet
import com.example.ui.keyboard.components.ExtendedPcKeysSheet
import com.example.ui.keyboard.components.KeyboardSettingsSheet
import com.example.ui.keyboard.model.KeyboardThemeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current

    val editorValue by viewModel.editorValue.collectAsState()
    val allSnippets by viewModel.allSnippets.collectAsState()
    val pinnedSnippets by viewModel.pinnedSnippets.collectAsState()
    val currentThemeType by viewModel.themeType.collectAsState()
    val isLaptopBarVisible by viewModel.isLaptopBarVisible.collectAsState()
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsState()
    val lastActionStatus by viewModel.lastActionStatus.collectAsState()

    val colors = viewModel.currentColors

    var showClipboardSheet by remember { mutableStateOf(false) }
    var showExtendedPcSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    fun openImeSettings() {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(colors.toolbarBackground)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.enterKeyBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "App Icon",
                            tint = colors.enterKeyTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "KeyPro Gboard",
                            color = colors.letterKeyTextColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentThemeType.displayName} • Laptop Keys",
                            color = colors.letterKeySecondaryTextColor,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Action: Enable System Keyboard
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.functionKeyBackground)
                            .clickable { openImeSettings() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("btn_top_enable_system"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Enable IME",
                            color = colors.enterKeyBackground,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { viewModel.clearEditor() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_top_clear")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Pad",
                            tint = colors.toolbarIconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Interactive Editor Area (Top Half)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Status pill and document stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    lastActionStatus?.let { status ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.functionKeyBackground)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "⚡ $status",
                                color = colors.enterKeyBackground,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }

                    Text(
                        text = "${editorValue.text.length} chars • Cursor: ${editorValue.selection.start}",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Interactive Document Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.letterKeyBackground.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    TextField(
                        value = editorValue,
                        onValueChange = { viewModel.updateEditorValue(it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .testTag("interactive_text_editor"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = colors.letterKeyTextColor,
                            unfocusedTextColor = colors.letterKeyTextColor,
                            cursorColor = colors.enterKeyBackground
                        ),
                        placeholder = {
                            Text(
                                text = "Tap keys below or snippets to start typing...",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 14.sp
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontFamily = FontFamily.Default
                        )
                    )
                }
            }

            // Keyboard Container (Bottom Half)
            KeyProKeyboardView(
                colors = colors,
                pinnedSnippets = pinnedSnippets,
                isSoundEnabled = isSoundEnabled,
                isHapticEnabled = isHapticEnabled,
                initialLaptopBarVisible = isLaptopBarVisible,
                onAction = { action -> viewModel.handleKeyboardAction(action) },
                onOpenClipboardSheet = { showClipboardSheet = true },
                onOpenExtendedPcSheet = { showExtendedPcSheet = true },
                onOpenThemePicker = { showSettingsSheet = true },
                onOpenSettings = { showSettingsSheet = true }
            )
        }
    }

    // Modal Sheet 1: Clipboard & Pinned Snippets
    if (showClipboardSheet) {
        ClipboardSheet(
            snippets = allSnippets,
            colors = colors,
            onDismiss = { showClipboardSheet = false },
            onSnippetSelected = { text ->
                viewModel.handleKeyboardAction(com.example.ui.keyboard.model.KeyAction.InsertText(text))
            },
            onTogglePin = { viewModel.togglePin(it) },
            onDeleteSnippet = { viewModel.deleteSnippet(it) },
            onSaveSnippet = { title, content, isPinned, category ->
                viewModel.saveSnippet(title, content, isPinned, category)
            },
            onClearHistory = { viewModel.clearUnpinnedHistory() }
        )
    }

    // Modal Sheet 2: Extended PC Keys (F1-F12, Dev symbols, Cursor controls)
    if (showExtendedPcSheet) {
        ExtendedPcKeysSheet(
            colors = colors,
            onDismiss = { showExtendedPcSheet = false },
            onAction = { action -> viewModel.handleKeyboardAction(action) }
        )
    }

    // Modal Sheet 3: Settings & Themes
    if (showSettingsSheet) {
        KeyboardSettingsSheet(
            colors = colors,
            currentThemeType = currentThemeType,
            isLaptopBarEnabled = isLaptopBarVisible,
            isSoundEnabled = isSoundEnabled,
            isHapticEnabled = isHapticEnabled,
            onDismiss = { showSettingsSheet = false },
            onThemeSelected = { viewModel.setTheme(it) },
            onToggleLaptopBar = { viewModel.toggleLaptopBar(it) },
            onToggleSound = { viewModel.toggleSound(it) },
            onToggleHaptic = { viewModel.toggleHaptic(it) },
            onOpenSystemImeSettings = { openImeSettings() }
        )
    }
}
