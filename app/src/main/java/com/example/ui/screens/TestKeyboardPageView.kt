package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SnippetEntity
import com.example.ui.keyboard.KeyProKeyboardView
import com.example.ui.keyboard.model.KeyAction
import com.example.ui.keyboard.model.KeyboardColors

@Composable
fun TestKeyboardPageView(
    editorValue: TextFieldValue,
    onEditorValueChange: (TextFieldValue) -> Unit,
    colors: KeyboardColors,
    allSnippets: List<SnippetEntity>,
    pinnedSnippets: List<SnippetEntity>,
    historySnippets: List<SnippetEntity>,
    isSoundEnabled: Boolean,
    isHapticEnabled: Boolean,
    isLaptopBarVisible: Boolean,
    lastActionStatus: String,
    onAction: (KeyAction) -> Unit,
    onTogglePin: (SnippetEntity) -> Unit,
    onDeleteSnippet: (Long) -> Unit,
    onSaveSnippet: (title: String, content: String, isPinned: Boolean, category: String, shortcut: String) -> Unit,
    onClearHistory: () -> Unit,
    onClearEditor: () -> Unit,
    onBack: () -> Unit
) {
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
                    modifier = Modifier.testTag("btn_back_from_test_keyboard")
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
                        text = "Test Keyboard & Layout",
                        color = colors.letterKeyTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Interactive typing pad & live preview",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onClearEditor,
                modifier = Modifier.testTag("btn_clear_text")
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear Editor",
                    tint = colors.toolbarIconTint
                )
            }
        }

        // Status bar indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.functionKeyBackground.copy(alpha = 0.6f))
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = lastActionStatus.ifEmpty { "Ready to type" },
                color = colors.letterKeyTextColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${editorValue.text.length} chars | ${editorValue.text.split("\\s+".toRegex()).count { it.isNotEmpty() }} words",
                color = colors.letterKeySecondaryTextColor,
                fontSize = 11.sp
            )
        }

        // Test Text Editor Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            OutlinedTextField(
                value = editorValue,
                onValueChange = onEditorValueChange,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("editor_text_field"),
                placeholder = {
                    Text(
                        text = "Tap any key below to test typing, hold to delete, PC keys (Ctrl, Alt, Tab, Esc), emojis, and clipboard...",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 14.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.enterKeyBackground,
                    unfocusedBorderColor = colors.functionKeyBackground,
                    focusedTextColor = colors.letterKeyTextColor,
                    unfocusedTextColor = colors.letterKeyTextColor,
                    cursorColor = colors.enterKeyBackground
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Active KeyPro Keyboard Component
        KeyProKeyboardView(
            colors = colors,
            allSnippets = allSnippets,
            pinnedSnippets = pinnedSnippets,
            historySnippets = historySnippets,
            isSoundEnabled = isSoundEnabled,
            isHapticEnabled = isHapticEnabled,
            initialLaptopBarVisible = isLaptopBarVisible,
            onAction = onAction,
            onTogglePin = onTogglePin,
            onDeleteSnippet = onDeleteSnippet,
            onSaveSnippet = onSaveSnippet,
            onClearHistory = onClearHistory,
            onOpenSettings = {
                // Handled in-keyboard!
            }
        )
    }
}
