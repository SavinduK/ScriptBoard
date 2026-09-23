package com.example.ui.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.example.data.SnippetEntity
import com.example.ui.keyboard.components.EmojiPickerView
import com.example.ui.keyboard.components.InKeyboardClipboardView
import com.example.ui.keyboard.components.KeyCap
import com.example.ui.keyboard.components.KeyboardToolbar
import com.example.ui.keyboard.components.LaptopKeysBar
import com.example.ui.keyboard.model.KeyAction
import com.example.ui.keyboard.model.KeyItem
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.model.KeyboardLayoutGenerator
import com.example.ui.keyboard.model.KeyboardLayoutMode
import com.example.ui.keyboard.model.ShiftState
import com.example.ui.keyboard.util.FeedbackUtil

@Composable
fun KeyProKeyboardView(
    colors: KeyboardColors,
    allSnippets: List<SnippetEntity> = emptyList(),
    isSoundEnabled: Boolean = true,
    isHapticEnabled: Boolean = true,
    initialLaptopBarVisible: Boolean = true,
    onAction: (KeyAction) -> Unit,
    onTogglePin: (SnippetEntity) -> Unit = {},
    onDeleteSnippet: (Long) -> Unit = {},
    onSaveSnippet: (title: String, content: String, isPinned: Boolean, category: String) -> Unit = { _, _, _, _ -> },
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(isSoundEnabled) {
        view.isSoundEffectsEnabled = isSoundEnabled
        onDispose { }
    }

    var layoutMode by remember { mutableStateOf(KeyboardLayoutMode.TEXT) }
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }
    var isLaptopBarVisible by remember { mutableStateOf(initialLaptopBarVisible) }
    var isCtrlActive by remember { mutableStateOf(false) }
    var isAltActive by remember { mutableStateOf(false) }

    fun handleKeyAction(action: KeyAction) {
        FeedbackUtil.performKeyPressFeedback(context, view, isSoundEnabled, isHapticEnabled)

        // Check if CTRL shortcut is being performed
        if (isCtrlActive && action is KeyAction.InsertText) {
            val keyChar = action.text.lowercase()
            when (keyChar) {
                "a" -> {
                    onAction(KeyAction.SelectAll)
                    isCtrlActive = false
                    return
                }
                "c" -> {
                    onAction(KeyAction.Copy)
                    isCtrlActive = false
                    return
                }
                "v" -> {
                    onAction(KeyAction.Paste)
                    isCtrlActive = false
                    return
                }
                "x" -> {
                    onAction(KeyAction.Cut)
                    isCtrlActive = false
                    return
                }
                "z" -> {
                    onAction(KeyAction.Undo)
                    isCtrlActive = false
                    return
                }
                "y" -> {
                    onAction(KeyAction.Redo)
                    isCtrlActive = false
                    return
                }
            }
        }

        when (action) {
            is KeyAction.ToggleShift -> {
                shiftState = when (shiftState) {
                    ShiftState.OFF -> ShiftState.ONCE
                    ShiftState.ONCE -> ShiftState.CAPS_LOCKED
                    ShiftState.CAPS_LOCKED -> ShiftState.OFF
                }
            }
            is KeyAction.SwitchToSymbols -> {
                layoutMode = KeyboardLayoutMode.SYMBOLS_1
            }
            is KeyAction.SwitchToLetters -> {
                layoutMode = KeyboardLayoutMode.TEXT
            }
            is KeyAction.SwitchToMoreSymbols -> {
                layoutMode = KeyboardLayoutMode.SYMBOLS_2
            }
            is KeyAction.SwitchToNumpad -> {
                layoutMode = KeyboardLayoutMode.NUMPAD
            }
            is KeyAction.SwitchToExtendedPc -> {
                layoutMode = if (layoutMode == KeyboardLayoutMode.EXTENDED_PC) {
                    KeyboardLayoutMode.TEXT
                } else {
                    KeyboardLayoutMode.EXTENDED_PC
                }
            }
            is KeyAction.SwitchToEmoji -> {
                layoutMode = KeyboardLayoutMode.EMOJI
            }
            is KeyAction.SwitchToClipboard -> {
                layoutMode = if (layoutMode == KeyboardLayoutMode.CLIPBOARD) {
                    KeyboardLayoutMode.TEXT
                } else {
                    KeyboardLayoutMode.CLIPBOARD
                }
            }
            is KeyAction.ToggleCtrl -> {
                isCtrlActive = !isCtrlActive
            }
            is KeyAction.ToggleAlt -> {
                isAltActive = !isAltActive
            }
            is KeyAction.InsertText -> {
                onAction(action)
                if (shiftState == ShiftState.ONCE) {
                    shiftState = ShiftState.OFF
                }
            }
            else -> {
                onAction(action)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        // Laptop Keys Bar (Special laptop keys: ESC, /, —, HOME, ↑, END, PGUP, ↹, CTRL, ALT, ←, ↓, →, PGDN)
        AnimatedVisibility(
            visible = isLaptopBarVisible,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            LaptopKeysBar(
                colors = colors,
                isCtrlActive = isCtrlActive,
                isAltActive = isAltActive,
                onKeyClick = { handleKeyAction(it.action) }
            )
        }

        // Toolbar: ONLY the 4 requested buttons (Laptop key switch, Extended PC keys, Clipboard, Settings)
        KeyboardToolbar(
            colors = colors,
            isLaptopBarVisible = isLaptopBarVisible,
            isExtendedPcActive = layoutMode == KeyboardLayoutMode.EXTENDED_PC,
            onToggleLaptopBar = { isLaptopBarVisible = !isLaptopBarVisible },
            onToggleExtendedPcKeys = {
                layoutMode = if (layoutMode == KeyboardLayoutMode.EXTENDED_PC) {
                    KeyboardLayoutMode.TEXT
                } else {
                    KeyboardLayoutMode.EXTENDED_PC
                }
            },
            onOpenClipboard = {
                layoutMode = if (layoutMode == KeyboardLayoutMode.CLIPBOARD) {
                    KeyboardLayoutMode.TEXT
                } else {
                    KeyboardLayoutMode.CLIPBOARD
                }
            },
            onOpenSettings = onOpenSettings
        )

        // Main Keyboard Area
        when (layoutMode) {
            KeyboardLayoutMode.EMOJI -> {
                EmojiPickerView(
                    colors = colors,
                    onEmojiSelected = { emoji ->
                        FeedbackUtil.performKeyPressFeedback(context, view, isSoundEnabled, isHapticEnabled)
                        onAction(KeyAction.InsertText(emoji))
                    },
                    onBackToLetters = { layoutMode = KeyboardLayoutMode.TEXT },
                    onBackspace = { handleKeyAction(KeyAction.Backspace) }
                )
            }
            KeyboardLayoutMode.CLIPBOARD -> {
                InKeyboardClipboardView(
                    snippets = allSnippets,
                    colors = colors,
                    onSnippetSelected = { text ->
                        FeedbackUtil.performKeyPressFeedback(context, view, isSoundEnabled, isHapticEnabled)
                        onAction(KeyAction.InsertText(text))
                    },
                    onTogglePin = onTogglePin,
                    onDeleteSnippet = onDeleteSnippet,
                    onSaveSnippet = onSaveSnippet,
                    onBackToLetters = { layoutMode = KeyboardLayoutMode.TEXT }
                )
            }
            else -> {
                // Layout is either TEXT, SYMBOLS_1, SYMBOLS_2, NUMPAD, or EXTENDED_PC (special set of laptop keys!)
                val currentRows = when (layoutMode) {
                    KeyboardLayoutMode.TEXT -> KeyboardLayoutGenerator.getQwertyRows(shiftState)
                    KeyboardLayoutMode.SYMBOLS_1 -> KeyboardLayoutGenerator.getSymbols1Rows()
                    KeyboardLayoutMode.SYMBOLS_2 -> KeyboardLayoutGenerator.getSymbols2Rows()
                    KeyboardLayoutMode.NUMPAD -> KeyboardLayoutGenerator.getNumpadRows()
                    KeyboardLayoutMode.EXTENDED_PC -> KeyboardLayoutGenerator.getExtendedPcRows()
                    else -> KeyboardLayoutGenerator.getQwertyRows(shiftState)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 3.dp)
                ) {
                    currentRows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.5.dp)
                        ) {
                            rowKeys.forEach { key ->
                                KeyCap(
                                    key = key,
                                    colors = colors,
                                    heightDp = 46,
                                    onKeyClick = { handleKeyAction(it.action) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Empty space below the last row of keys (similar height of a row of keys)
        Spacer(modifier = Modifier.height(48.dp))
    }
}
