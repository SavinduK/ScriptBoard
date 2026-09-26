package com.example.ui.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.SnippetEntity
import com.example.data.StickerEntity
import com.example.data.StickerRepository
import com.example.data.WordSuggestionManager
import com.example.ui.keyboard.components.EmojiPickerView
import com.example.ui.keyboard.components.InKeyboardClipboardView
import com.example.ui.keyboard.components.InKeyboardSettingsView
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
import com.example.ui.keyboard.util.KeyboardPreferences
import kotlinx.coroutines.launch

@Composable
fun KeyProKeyboardView(
    colors: KeyboardColors,
    allSnippets: List<SnippetEntity> = emptyList(),
    pinnedSnippets: List<SnippetEntity> = allSnippets.filter { it.isPinned },
    historySnippets: List<SnippetEntity> = allSnippets.filter { !it.isPinned },
    stickers: List<StickerEntity> = emptyList(),
    isSoundEnabled: Boolean = true,
    isHapticEnabled: Boolean = true,
    initialLaptopBarVisible: Boolean = true,
    onAction: (KeyAction) -> Unit,
    onTogglePin: (SnippetEntity) -> Unit = {},
    onDeleteSnippet: (Long) -> Unit = {},
    onSaveSnippet: (title: String, content: String, isPinned: Boolean, category: String, shortcut: String) -> Unit = { _, _, _, _, _ -> },
    onClearHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardPrefs = remember { KeyboardPreferences.getInstance(context) }
    val holdForSymbolsEnabled by keyboardPrefs.holdForSymbolsEnabled.collectAsState()
    val autoSuggestWordsEnabled by keyboardPrefs.autoSuggestEnabled.collectAsState()
    val keyFontSize by keyboardPrefs.keyFontSize.collectAsState()

    val stickerRepo = remember { StickerRepository(AppDatabase.getDatabase(context).stickerDao()) }
    val localStickers by stickerRepo.allStickers.collectAsState(initial = emptyList())
    val displayStickers = if (stickers.isNotEmpty()) stickers else localStickers

    val wordSuggestionManager = remember {
        WordSuggestionManager(AppDatabase.getDatabase(context).wordFrequencyDao())
    }

    DisposableEffect(isSoundEnabled) {
        view.isSoundEffectsEnabled = isSoundEnabled
        onDispose { }
    }

    var layoutMode by remember { mutableStateOf(KeyboardLayoutMode.TEXT) }
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }
    var isLaptopBarVisible by remember { mutableStateOf(initialLaptopBarVisible) }
    var isCtrlActive by remember { mutableStateOf(false) }
    var isAltActive by remember { mutableStateOf(false) }
    var currentWordBuffer by remember { mutableStateOf("") }
    var suggestedWords by remember { mutableStateOf(listOf("I", "The", "Thanks")) }

    // Update 3 word suggestions dynamically as user types (toggleable in settings)
    LaunchedEffect(currentWordBuffer, autoSuggestWordsEnabled) {
        if (autoSuggestWordsEnabled) {
            val suggestions = wordSuggestionManager.getSuggestions(currentWordBuffer)
            suggestedWords = suggestions
        } else {
            suggestedWords = emptyList()
        }
    }

    // Matching shortcuts for phrase expansion
    val matchingShortcuts = remember(currentWordBuffer, pinnedSnippets) {
        if (currentWordBuffer.isNotBlank()) {
            val query = currentWordBuffer.trim()
            pinnedSnippets.filter { snippet ->
                val s = snippet.shortcut.trim()
                s.isNotEmpty() && (
                    s.equals(query, ignoreCase = true) ||
                    s.startsWith(query, ignoreCase = true) ||
                    query.startsWith(s, ignoreCase = true) ||
                    (query.startsWith("@") && s.contains(query.removePrefix("@"), ignoreCase = true))
                )
            }
        } else {
            emptyList()
        }
    }

    fun handleKeyAction(action: KeyAction) {
        FeedbackUtil.performKeyPressFeedback(context, view, isSoundEnabled, isHapticEnabled)

        // Track typed word buffer for suggestions and phrase expansions
        when (action) {
            is KeyAction.InsertText -> {
                if (action.text.length == 1 && (action.text[0].isLetterOrDigit() || action.text in listOf("@", "/", "#", "_", "-", "."))) {
                    currentWordBuffer += action.text
                } else {
                    if (currentWordBuffer.isNotBlank()) {
                        val wordToRecord = currentWordBuffer
                        coroutineScope.launch { wordSuggestionManager.recordWordUsed(wordToRecord) }
                    }
                    currentWordBuffer = ""
                }
            }
            is KeyAction.Backspace -> {
                if (currentWordBuffer.isNotEmpty()) {
                    currentWordBuffer = currentWordBuffer.dropLast(1)
                }
            }
            is KeyAction.Space -> {
                // Check if user typed an exact shortcut
                val matched = pinnedSnippets.firstOrNull { it.shortcut.isNotBlank() && it.shortcut.equals(currentWordBuffer.trim(), ignoreCase = true) }
                if (matched != null) {
                    onAction(KeyAction.ExpandShortcut(currentWordBuffer, matched.content + " "))
                    currentWordBuffer = ""
                    return
                }
                // Record typed word into typing history (Request #3: same word used more -> suggest more)
                // Note: Typed word is NOT autocorrected (no auto correct only suggest)
                if (currentWordBuffer.isNotBlank()) {
                    val wordToRecord = currentWordBuffer
                    coroutineScope.launch { wordSuggestionManager.recordWordUsed(wordToRecord) }
                }
                currentWordBuffer = ""
            }
            is KeyAction.Enter -> {
                if (currentWordBuffer.isNotBlank()) {
                    val wordToRecord = currentWordBuffer
                    coroutineScope.launch { wordSuggestionManager.recordWordUsed(wordToRecord) }
                }
                currentWordBuffer = ""
            }
            else -> {
                currentWordBuffer = ""
            }
        }

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
                "x" -> {
                    onAction(KeyAction.Cut)
                    isCtrlActive = false
                    return
                }
                "v" -> {
                    onAction(KeyAction.Paste)
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

        // Standard actions
        when (action) {
            is KeyAction.ToggleShift -> {
                shiftState = when (shiftState) {
                    ShiftState.OFF -> ShiftState.ONCE
                    ShiftState.ONCE -> ShiftState.CAPS_LOCKED
                    ShiftState.CAPS_LOCKED -> ShiftState.OFF
                }
            }
            is KeyAction.SwitchToSymbols -> layoutMode = KeyboardLayoutMode.SYMBOLS_1
            is KeyAction.SwitchToLetters -> layoutMode = KeyboardLayoutMode.TEXT
            is KeyAction.SwitchToMoreSymbols -> layoutMode = KeyboardLayoutMode.SYMBOLS_2
            is KeyAction.SwitchToNumpad -> layoutMode = KeyboardLayoutMode.NUMPAD
            is KeyAction.SwitchToExtendedPc -> layoutMode = KeyboardLayoutMode.EXTENDED_PC
            is KeyAction.SwitchToEmoji -> layoutMode = KeyboardLayoutMode.EMOJI
            is KeyAction.SwitchToClipboard -> layoutMode = KeyboardLayoutMode.CLIPBOARD
            is KeyAction.ToggleCtrl -> isCtrlActive = !isCtrlActive
            is KeyAction.ToggleAlt -> isAltActive = !isAltActive
            is KeyAction.InsertText -> {
                onAction(action)
                if (shiftState == ShiftState.ONCE) {
                    shiftState = ShiftState.OFF
                }
            }
            else -> onAction(action)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        // Laptop Keys Bar (Top row: Esc, Tab, Ctrl, Alt, Arrow keys)
        AnimatedVisibility(
            visible = isLaptopBarVisible && (layoutMode != KeyboardLayoutMode.EMOJI && layoutMode != KeyboardLayoutMode.CLIPBOARD && layoutMode != KeyboardLayoutMode.SETTINGS),
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

        // Toolbar: Laptop switch, Extended PC keys, Direct Paste, Clipboard, Settings
        KeyboardToolbar(
            colors = colors,
            isLaptopBarVisible = isLaptopBarVisible,
            isExtendedPcActive = layoutMode == KeyboardLayoutMode.EXTENDED_PC,
            isStickersActive = layoutMode == KeyboardLayoutMode.STICKERS,
            onToggleLaptopBar = { isLaptopBarVisible = !isLaptopBarVisible },
            onToggleExtendedPcKeys = {
                layoutMode = if (layoutMode == KeyboardLayoutMode.EXTENDED_PC) {
                    KeyboardLayoutMode.TEXT
                } else {
                    KeyboardLayoutMode.EXTENDED_PC
                }
            },
            onPasteCopiedContent = {
                handleKeyAction(KeyAction.Paste)
            },
            onOpenClipboard = {
                layoutMode = if (layoutMode == KeyboardLayoutMode.CLIPBOARD) {
                    KeyboardLayoutMode.TEXT
                } else {
                    KeyboardLayoutMode.CLIPBOARD
                }
            },
            onToggleStickers = {
                layoutMode = if (layoutMode == KeyboardLayoutMode.STICKERS) {
                    KeyboardLayoutMode.TEXT
                } else {
                    KeyboardLayoutMode.STICKERS
                }
            },
            onOpenSettings = {
                // Open settings directly from the keyboard itself without opening the app
                layoutMode = if (layoutMode == KeyboardLayoutMode.SETTINGS) {
                    KeyboardLayoutMode.TEXT
                } else {
                    KeyboardLayoutMode.SETTINGS
                }
            }
        )

        // Shortcut suggestion bar (when shortcut matches query)
        AnimatedVisibility(
            visible = matchingShortcuts.isNotEmpty() && layoutMode == KeyboardLayoutMode.TEXT,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(colors.toolbarBackground)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = colors.enterKeyBackground,
                    modifier = Modifier.size(16.dp)
                )
                matchingShortcuts.forEach { snippet ->
                    AssistChip(
                        onClick = {
                            FeedbackUtil.performKeyPressFeedback(context, view, isSoundEnabled, isHapticEnabled)
                            onAction(KeyAction.ExpandShortcut(currentWordBuffer, snippet.content))
                            currentWordBuffer = ""
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = snippet.shortcut,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = colors.enterKeyBackground
                                )
                                Text(
                                    text = " ➔ ",
                                    fontSize = 11.sp,
                                    color = colors.letterKeySecondaryTextColor
                                )
                                Text(
                                    text = snippet.content.take(24) + if (snippet.content.length > 24) "..." else "",
                                    fontSize = 12.sp,
                                    color = colors.letterKeyTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = colors.letterKeyBackground,
                            labelColor = colors.letterKeyTextColor
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("chip_shortcut_${snippet.shortcut}")
                    )
                }
            }
        }

        // Auto Suggest Words Feature - Show 3 Suggested Words on Top (toggleable in settings)
        // Displays 3 suggestions with user typing history frequency prioritization
        // Tapping replaces the word. User typing space/enter leaves typed word unchanged.
        if (autoSuggestWordsEnabled && layoutMode in listOf(KeyboardLayoutMode.TEXT, KeyboardLayoutMode.SYMBOLS_1, KeyboardLayoutMode.SYMBOLS_2, KeyboardLayoutMode.NUMPAD, KeyboardLayoutMode.EXTENDED_PC)) {
            val suggestionsToDisplay = remember(suggestedWords) {
                val list = suggestedWords.toMutableList()
                while (list.size < 3) {
                    list.add("")
                }
                list.take(3)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(colors.toolbarBackground.copy(alpha = 0.95f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("row_auto_suggest_words"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                suggestionsToDisplay.forEachIndexed { index, word ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(colors.functionKeyBackground.copy(alpha = 0.6f))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = word.isNotBlank()) {
                                if (word.isNotBlank()) {
                                    FeedbackUtil.performKeyPressFeedback(context, view, isSoundEnabled, isHapticEnabled)
                                    onAction(KeyAction.ApplySuggestion(currentWordBuffer, word))
                                    coroutineScope.launch {
                                        wordSuggestionManager.recordWordUsed(word)
                                    }
                                    currentWordBuffer = ""
                                }
                            }
                            .testTag("suggested_word_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (word.isNotBlank()) {
                            Text(
                                text = word,
                                color = if (index == 1) colors.enterKeyBackground else colors.letterKeyTextColor,
                                fontSize = if (index == 1) 14.sp else 13.sp,
                                fontWeight = if (index == 1) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Main Keyboard Area
        when (layoutMode) {
            KeyboardLayoutMode.EMOJI -> {
                EmojiPickerView(
                    colors = colors,
                    stickers = displayStickers,
                    initialSection = com.example.ui.keyboard.components.MediaPickerSection.EMOJIS,
                    onEmojiSelected = { emoji ->
                        FeedbackUtil.performKeyPressFeedback(context, view, isSoundEnabled, isHapticEnabled)
                        onAction(KeyAction.InsertText(emoji))
                    },
                    onStickerSelected = { sticker ->
                        FeedbackUtil.performKeyPressFeedback(context, view, isSoundEnabled, isHapticEnabled)
                        onAction(KeyAction.InsertSticker(sticker.filePath, sticker.name))
                        coroutineScope.launch { stickerRepo.recordStickerUsed(sticker.id) }
                    },
                    onDeleteSticker = { sticker ->
                        coroutineScope.launch { stickerRepo.deleteSticker(sticker) }
                    },
                    onBackToLetters = { layoutMode = KeyboardLayoutMode.TEXT },
                    onBackspace = { handleKeyAction(KeyAction.Backspace) }
                )
            }
            KeyboardLayoutMode.CLIPBOARD -> {
                InKeyboardClipboardView(
                    pinnedSnippets = pinnedSnippets,
                    historySnippets = historySnippets,
                    colors = colors,
                    onSnippetSelected = { text ->
                        FeedbackUtil.performKeyPressFeedback(context, view, isSoundEnabled, isHapticEnabled)
                        onAction(KeyAction.InsertText(text))
                    },
                    onTogglePin = onTogglePin,
                    onDeleteSnippet = onDeleteSnippet,
                    onSaveSnippet = onSaveSnippet,
                    onClearHistory = onClearHistory,
                    onBackToLetters = { layoutMode = KeyboardLayoutMode.TEXT }
                )
            }
            KeyboardLayoutMode.SETTINGS -> {
                InKeyboardSettingsView(
                    colors = colors,
                    onBackToLetters = { layoutMode = KeyboardLayoutMode.TEXT }
                )
            }
            KeyboardLayoutMode.STICKERS -> {
                EmojiPickerView(
                    colors = colors,
                    stickers = displayStickers,
                    initialSection = com.example.ui.keyboard.components.MediaPickerSection.STICKERS,
                    onEmojiSelected = { emoji ->
                        FeedbackUtil.performKeyPressFeedback(context, view, isSoundEnabled, isHapticEnabled)
                        onAction(KeyAction.InsertText(emoji))
                    },
                    onStickerSelected = { sticker ->
                        FeedbackUtil.performKeyPressFeedback(context, view, isSoundEnabled, isHapticEnabled)
                        onAction(KeyAction.InsertSticker(sticker.filePath, sticker.name))
                        coroutineScope.launch { stickerRepo.recordStickerUsed(sticker.id) }
                    },
                    onDeleteSticker = { sticker ->
                        coroutineScope.launch { stickerRepo.deleteSticker(sticker) }
                    },
                    onBackToLetters = { layoutMode = KeyboardLayoutMode.TEXT },
                    onBackspace = { handleKeyAction(KeyAction.Backspace) }
                )
            }
            else -> {
                // Layout is either TEXT, SYMBOLS_1, SYMBOLS_2, NUMPAD, or EXTENDED_PC
                val currentRows = when (layoutMode) {
                    KeyboardLayoutMode.TEXT -> KeyboardLayoutGenerator.getQwertyRows(shiftState, holdForSymbols = holdForSymbolsEnabled)
                    KeyboardLayoutMode.SYMBOLS_1 -> KeyboardLayoutGenerator.getSymbols1Rows()
                    KeyboardLayoutMode.SYMBOLS_2 -> KeyboardLayoutGenerator.getSymbols2Rows()
                    KeyboardLayoutMode.NUMPAD -> KeyboardLayoutGenerator.getNumpadRows()
                    KeyboardLayoutMode.EXTENDED_PC -> KeyboardLayoutGenerator.getExtendedPcRows()
                    else -> KeyboardLayoutGenerator.getQwertyRows(shiftState, holdForSymbols = holdForSymbolsEnabled)
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
                                    characterFontSize = keyFontSize,
                                    onKeyClick = { handleKeyAction(it.action) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom spacer
        Spacer(modifier = Modifier.height(48.dp))
    }
}
