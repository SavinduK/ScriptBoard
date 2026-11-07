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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.unit.Dp
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
import com.example.ui.keyboard.components.VoiceTypingView
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
    openSessionTrigger: Long = 0L,
    syncedWordBuffer: String? = null,
    syncedPrevWord: String? = null,
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
    val altKeyFontSize by keyboardPrefs.altKeyFontSize.collectAsState()
    val currentLanguage by keyboardPrefs.currentLanguage.collectAsState()
    val installedLanguages by keyboardPrefs.installedLanguages.collectAsState()
    val numberRowEnabled by keyboardPrefs.numberRowEnabled.collectAsState()
    val autoCapitalizationEnabled by keyboardPrefs.autoCapitalizationEnabled.collectAsState()

    val tabletSplitEnabled by keyboardPrefs.tabletSplitEnabled.collectAsState()
    val tabletHalfSizeEnabled by keyboardPrefs.tabletHalfSizeEnabled.collectAsState()
    val tabletHalfSide by keyboardPrefs.tabletHalfSide.collectAsState()

    val stickerRepo = remember { StickerRepository(AppDatabase.getDatabase(context).stickerDao()) }
    val localStickers by stickerRepo.allStickers.collectAsState(initial = emptyList())
    val displayStickers = if (stickers.isNotEmpty()) stickers else localStickers

    val wordSuggestionManager = remember {
        val db = AppDatabase.getDatabase(context)
        WordSuggestionManager(
            wordFrequencyDao = db.wordFrequencyDao(),
            bigramFrequencyDao = db.bigramFrequencyDao(),
            correctionPairDao = db.correctionPairDao(),
            translitChoiceDao = db.translitChoiceDao(),
            dictionaryRepo = com.example.data.DictionaryRepository.getInstance(context)
        )
    }

    DisposableEffect(isSoundEnabled) {
        view.isSoundEffectsEnabled = isSoundEnabled
        onDispose { }
    }

    val prefLaptopBarVisible by keyboardPrefs.laptopBarVisible.collectAsState()
    var layoutMode by remember { mutableStateOf(KeyboardLayoutMode.TEXT) }
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }
    var isLaptopBarVisible by remember(prefLaptopBarVisible) { mutableStateOf(prefLaptopBarVisible) }
    var isCtrlActive by remember { mutableStateOf(false) }
    var isAltActive by remember { mutableStateOf(false) }
    var currentWordBuffer by remember { mutableStateOf("") }
    var internalPrevWord by remember { mutableStateOf<String?>(null) }
    val effectivePrevWord = syncedPrevWord ?: internalPrevWord

    var suggestedWords by remember(currentLanguage) {
        mutableStateOf(
            if (currentLanguage == "si" || currentLanguage == "si-trans")
                listOf("ආයුබෝවන්", "ස්තූතියි", "කොහොමද")
            else listOf("I", "The", "Thanks")
        )
    }
    var selectedSinhalaLetter by remember { mutableStateOf("ව") }

    // Sync buffer from IME service when updated
    LaunchedEffect(syncedWordBuffer) {
        if (syncedWordBuffer != null) {
            currentWordBuffer = syncedWordBuffer
        }
    }

    // Reset keyboard position to English alphabet layout whenever the keyboard is opened in an app
    LaunchedEffect(openSessionTrigger) {
        if (openSessionTrigger > 0L) {
            layoutMode = KeyboardLayoutMode.TEXT
            shiftState = if (autoCapitalizationEnabled) ShiftState.ONCE else ShiftState.OFF
            currentWordBuffer = ""
            isCtrlActive = false
            isAltActive = false
            selectedSinhalaLetter = "ව"
            internalPrevWord = null
        }
    }

    // Update 3 word suggestions dynamically with bigram context and typo handling
    LaunchedEffect(currentWordBuffer, autoSuggestWordsEnabled, currentLanguage, effectivePrevWord) {
        if (autoSuggestWordsEnabled) {
            val suggestions = wordSuggestionManager.getSuggestions(
                prefix = currentWordBuffer,
                language = currentLanguage,
                prevWord = effectivePrevWord
            )
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
                // English -> Sinhala Transliteration Keyboard (si-trans)
                if (currentLanguage == "si-trans" && currentWordBuffer.isNotBlank() && currentWordBuffer.all { it.isLetter() }) {
                    val transliterated = com.example.ui.keyboard.util.SinhalaTransliterator.transliterateWord(currentWordBuffer)
                    onAction(KeyAction.ApplySuggestion(currentWordBuffer, transliterated))
                    coroutineScope.launch { wordSuggestionManager.recordWordUsed(transliterated) }
                    currentWordBuffer = ""
                    return
                }

                // Check if user typed an exact shortcut
                val matched = pinnedSnippets.firstOrNull { it.shortcut.isNotBlank() && it.shortcut.equals(currentWordBuffer.trim(), ignoreCase = true) }
                if (matched != null) {
                    onAction(KeyAction.ExpandShortcut(currentWordBuffer, matched.content + " "))
                    currentWordBuffer = ""
                    return
                }
                // Record typed word into typing history
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
            is KeyAction.InsertSinhalaPillam -> {
                currentWordBuffer = ""
                onAction(action)
            }
            is KeyAction.SwitchLanguage -> {
                selectedSinhalaLetter = "ව"
                currentWordBuffer = ""
                val nextLang = keyboardPrefs.cycleLanguage()
                val langName = when (nextLang) {
                    "si" -> "සිංහල (Wijesekara)"
                    "si-trans" -> "සිංහල (English ➔ Sinhala)"
                    else -> "English"
                }
                android.widget.Toast.makeText(context, "Language: $langName", android.widget.Toast.LENGTH_SHORT).show()
            }
            is KeyAction.InsertText -> {
                if (action.text.isNotEmpty() && KeyboardLayoutGenerator.isSinhalaConsonant(action.text)) {
                    selectedSinhalaLetter = action.text
                }
                onAction(action)
                if (shiftState == ShiftState.ONCE) {
                    shiftState = ShiftState.OFF
                }
            }
            is KeyAction.Space -> {
                selectedSinhalaLetter = "ව"
                onAction(action)
            }
            is KeyAction.Enter -> {
                selectedSinhalaLetter = "ව"
                onAction(action)
                if (autoCapitalizationEnabled) {
                    shiftState = ShiftState.ONCE
                }
            }
            else -> onAction(action)
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = screenWidthDp >= 600 || configuration.smallestScreenWidthDp >= 600
    val isVeryWide = screenWidthDp >= 900
    val isLaptopLayout = layoutMode == KeyboardLayoutMode.EXTENDED_PC
    val isSinhala = currentLanguage == "si" && layoutMode == KeyboardLayoutMode.TEXT

    val tabletScale by keyboardPrefs.tabletKeyboardScale.collectAsState()
    val effectiveScale = if (isTablet) tabletScale else 1.0f

    // Increased key sizes and responsive scaling for large displays
    val baseHeightDp = when {
        isLaptopLayout -> if (isTablet) 52 else 42
        isSinhala -> if (isTablet) 54 else 45
        else -> if (isTablet) 66 else 54
    }
    val calculatedHeightDp = (baseHeightDp * effectiveScale).toInt()

    // Increased spacing between keys
    val keyHorizontalPadding = if (isTablet) (4.5f * effectiveScale).coerceAtLeast(3f) else 3.5f
    val keyVerticalPadding = if (isTablet) (4.5f * effectiveScale).coerceAtLeast(3.5f) else 3.8f
    val keyCornerRadius = if (isTablet) (12 * effectiveScale).toInt().coerceAtLeast(8) else 10
    val rowVerticalPadding = if (isTablet) (2.5f * effectiveScale).dp else 1.8.dp

    // Scaled font sizes
    val calculatedFontSize = when {
        isLaptopLayout -> if (isTablet) (16f * effectiveScale) else 13f
        isSinhala -> if (isTablet) (24f * effectiveScale) else 19f
        else -> if (isTablet) ((keyFontSize * 1.25f) * effectiveScale).coerceAtLeast(24f) else keyFontSize
    }
    val calculatedAltFontSize = if (isTablet) (altKeyFontSize * 1.15f * effectiveScale).coerceAtLeast(10f) else altKeyFontSize

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isVeryWide) 940.dp else androidx.compose.ui.unit.Dp.Unspecified)
                .background(colors.background)
        ) {
            // Laptop Keys Bar (Top row: Esc, Tab, Ctrl, Alt, Arrow keys)
            AnimatedVisibility(
                visible = isLaptopBarVisible && (layoutMode != KeyboardLayoutMode.EMOJI && layoutMode != KeyboardLayoutMode.CLIPBOARD && layoutMode != KeyboardLayoutMode.SETTINGS && layoutMode != KeyboardLayoutMode.STICKERS && layoutMode != KeyboardLayoutMode.VOICE_TYPING && layoutMode != KeyboardLayoutMode.EXTENDED_PC),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                LaptopKeysBar(
                    colors = colors,
                    isCtrlActive = isCtrlActive,
                    isAltActive = isAltActive,
                    heightDp = if (isTablet) 50 else 42,
                    horizontalPaddingDp = keyHorizontalPadding,
                    verticalPaddingDp = keyVerticalPadding,
                    cornerRadiusDp = keyCornerRadius,
                    fontSize = if (isTablet) 15f else 13f,
                    onKeyClick = { handleKeyAction(it.action) }
                )
            }

        // Toolbar: Laptop switch, Extended PC keys, Direct Paste, Clipboard, Emoji / Stickers, Voice Typing, Settings
        // In laptop keyboard mode, do not show the top icon bar
        if (layoutMode != KeyboardLayoutMode.EXTENDED_PC) {
            KeyboardToolbar(
                colors = colors,
                isLaptopBarVisible = isLaptopBarVisible,
                isExtendedPcActive = layoutMode == KeyboardLayoutMode.EXTENDED_PC,
                isEmojiActive = layoutMode == KeyboardLayoutMode.EMOJI || layoutMode == KeyboardLayoutMode.STICKERS,
                isVoiceTypeActive = layoutMode == KeyboardLayoutMode.VOICE_TYPING,
                currentLanguage = currentLanguage,
                onToggleLaptopBar = {
                    val next = !isLaptopBarVisible
                    isLaptopBarVisible = next
                    keyboardPrefs.setLaptopBarVisible(next)
                },
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
                onOpenEmoji = {
                    layoutMode = if (layoutMode == KeyboardLayoutMode.EMOJI || layoutMode == KeyboardLayoutMode.STICKERS) {
                        KeyboardLayoutMode.TEXT
                    } else {
                        KeyboardLayoutMode.EMOJI
                    }
                },
                onTriggerVoiceType = {
                    layoutMode = if (layoutMode == KeyboardLayoutMode.VOICE_TYPING) {
                        KeyboardLayoutMode.TEXT
                    } else {
                        KeyboardLayoutMode.VOICE_TYPING
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
        }

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
        if (autoSuggestWordsEnabled && layoutMode in listOf(KeyboardLayoutMode.TEXT, KeyboardLayoutMode.SYMBOLS_1, KeyboardLayoutMode.SYMBOLS_2, KeyboardLayoutMode.NUMPAD)) {
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
                    .height(if (isTablet) 46.dp else 40.dp)
                    .background(colors.toolbarBackground.copy(alpha = 0.95f))
                    .padding(horizontal = if (isTablet) 8.dp else 4.dp, vertical = 2.dp)
                    .testTag("row_auto_suggest_words"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                suggestionsToDisplay.forEachIndexed { index, word ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(if (isTablet) 20.dp else 16.dp)
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
                                    val typed = currentWordBuffer
                                    onAction(KeyAction.ApplySuggestion(typed, word))
                                    coroutineScope.launch {
                                        wordSuggestionManager.recordWordUsed(word)
                                        if (typed.isNotBlank() && !typed.equals(word, ignoreCase = true)) {
                                            wordSuggestionManager.recordCorrection(typed, word, currentLanguage)
                                            if (currentLanguage == "si-trans") {
                                                wordSuggestionManager.recordTranslitChoice(typed, word)
                                            }
                                        }
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
                                fontSize = if (index == 1) (if (isTablet) 16.sp else 14.sp) else (if (isTablet) 15.sp else 13.sp),
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
                        onAction(
                            KeyAction.InsertSticker(
                                filePath = sticker.filePath,
                                name = sticker.name,
                                gifPath = sticker.gifPath,
                                staticPngPath = sticker.staticPngPath,
                                isAnimated = sticker.isAnimated
                            )
                        )
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
                        onAction(
                            KeyAction.InsertSticker(
                                filePath = sticker.filePath,
                                name = sticker.name,
                                gifPath = sticker.gifPath,
                                staticPngPath = sticker.staticPngPath,
                                isAnimated = sticker.isAnimated
                            )
                        )
                        coroutineScope.launch { stickerRepo.recordStickerUsed(sticker.id) }
                    },
                    onDeleteSticker = { sticker ->
                        coroutineScope.launch { stickerRepo.deleteSticker(sticker) }
                    },
                    onBackToLetters = { layoutMode = KeyboardLayoutMode.TEXT },
                    onBackspace = { handleKeyAction(KeyAction.Backspace) }
                )
            }
            KeyboardLayoutMode.VOICE_TYPING -> {
                VoiceTypingView(
                    colors = colors,
                    onInsertText = { text ->
                        FeedbackUtil.performKeyPressFeedback(context, view, isSoundEnabled, isHapticEnabled)
                        onAction(KeyAction.InsertText(text))
                    },
                    onBackspace = { handleKeyAction(KeyAction.Backspace) },
                    onEnter = { handleKeyAction(KeyAction.Enter) },
                    onSpace = { handleKeyAction(KeyAction.Space) },
                    onBackToLetters = { layoutMode = KeyboardLayoutMode.TEXT }
                )
            }
            else -> {
                // Layout is either TEXT, SYMBOLS_1, SYMBOLS_2, NUMPAD, or EXTENDED_PC
                val isSinhala = currentLanguage == "si" && layoutMode == KeyboardLayoutMode.TEXT
                val isSinhalaTranslit = currentLanguage == "si-trans" && layoutMode == KeyboardLayoutMode.TEXT
                val hasMultipleLanguages = installedLanguages.size > 1
                val currentRows = when (layoutMode) {
                    KeyboardLayoutMode.TEXT -> {
                        if (isSinhala) {
                            KeyboardLayoutGenerator.getSinhalaRows(
                                selectedLetter = selectedSinhalaLetter,
                                showLanguageSwitchKey = hasMultipleLanguages
                            )
                        } else if (isSinhalaTranslit) {
                            KeyboardLayoutGenerator.getQwertyRows(
                                shiftState = shiftState,
                                holdForSymbols = holdForSymbolsEnabled,
                                languageDisplayName = "සිංහල (Singlish)",
                                includeNumberRow = numberRowEnabled,
                                showLanguageSwitchKey = hasMultipleLanguages
                            )
                        } else {
                            KeyboardLayoutGenerator.getQwertyRows(
                                shiftState = shiftState,
                                holdForSymbols = holdForSymbolsEnabled,
                                languageDisplayName = "English",
                                includeNumberRow = numberRowEnabled,
                                showLanguageSwitchKey = hasMultipleLanguages
                            )
                        }
                    }
                    KeyboardLayoutMode.SYMBOLS_1 -> KeyboardLayoutGenerator.getSymbols1Rows(
                        languageDisplayName = if (currentLanguage == "si" || currentLanguage == "si-trans") "සිංහල" else "English",
                        holdForSymbols = holdForSymbolsEnabled
                    )
                    KeyboardLayoutMode.SYMBOLS_2 -> KeyboardLayoutGenerator.getSymbols2Rows(
                        languageDisplayName = if (currentLanguage == "si" || currentLanguage == "si-trans") "සිංහල" else "English",
                        holdForSymbols = holdForSymbolsEnabled
                    )
                    KeyboardLayoutMode.NUMPAD -> KeyboardLayoutGenerator.getNumpadRows(
                        holdForSymbols = holdForSymbolsEnabled
                    )
                    KeyboardLayoutMode.EXTENDED_PC -> KeyboardLayoutGenerator.getCompleteLaptopRows(
                        shiftState = shiftState,
                        isCtrlActive = isCtrlActive,
                        isAltActive = isAltActive,
                        languageDisplayName = if (currentLanguage == "si") "සිංහල" else "English"
                    )
                    else -> KeyboardLayoutGenerator.getQwertyRows(shiftState, holdForSymbols = holdForSymbolsEnabled, showLanguageSwitchKey = hasMultipleLanguages)
                }

                if (isTablet && tabletSplitEnabled && layoutMode in listOf(KeyboardLayoutMode.TEXT, KeyboardLayoutMode.SYMBOLS_1, KeyboardLayoutMode.SYMBOLS_2)) {
                    TabletSplitKeyboardRows(
                        rows = currentRows,
                        colors = colors,
                        calculatedHeightDp = calculatedHeightDp,
                        calculatedFontSize = calculatedFontSize,
                        calculatedAltFontSize = calculatedAltFontSize,
                        keyHorizontalPadding = keyHorizontalPadding,
                        keyVerticalPadding = keyVerticalPadding,
                        keyCornerRadius = keyCornerRadius,
                        rowVerticalPadding = rowVerticalPadding,
                        gapWidthDp = if (isVeryWide) 110.dp else 64.dp,
                        holdForSymbolsEnabled = holdForSymbolsEnabled,
                        onKeyClick = { handleKeyAction(it.action) },
                        onKeyLongClick = { clickedKey ->
                            if (clickedKey.action is KeyAction.Space && installedLanguages.size > 1) {
                                handleKeyAction(KeyAction.SwitchLanguage)
                            } else if (isSinhala && clickedKey.action is KeyAction.InsertText && KeyboardLayoutGenerator.isSinhalaConsonant(clickedKey.action.text)) {
                                selectedSinhalaLetter = clickedKey.action.text
                            }
                        }
                    )
                } else if (isTablet && tabletHalfSizeEnabled && layoutMode in listOf(KeyboardLayoutMode.TEXT, KeyboardLayoutMode.SYMBOLS_1, KeyboardLayoutMode.SYMBOLS_2)) {
                    TabletHalfSizeKeyboardRows(
                        rows = currentRows,
                        colors = colors,
                        side = tabletHalfSide,
                        calculatedHeightDp = calculatedHeightDp,
                        calculatedFontSize = calculatedFontSize,
                        calculatedAltFontSize = calculatedAltFontSize,
                        keyHorizontalPadding = keyHorizontalPadding,
                        keyVerticalPadding = keyVerticalPadding,
                        keyCornerRadius = keyCornerRadius,
                        rowVerticalPadding = rowVerticalPadding,
                        holdForSymbolsEnabled = holdForSymbolsEnabled,
                        onToggleSide = { keyboardPrefs.toggleTabletHalfSide() },
                        onKeyClick = { handleKeyAction(it.action) },
                        onKeyLongClick = { clickedKey ->
                            if (clickedKey.action is KeyAction.Space && installedLanguages.size > 1) {
                                handleKeyAction(KeyAction.SwitchLanguage)
                            } else if (isSinhala && clickedKey.action is KeyAction.InsertText && KeyboardLayoutGenerator.isSinhalaConsonant(clickedKey.action.text)) {
                                selectedSinhalaLetter = clickedKey.action.text
                            }
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isTablet) 6.dp else 2.dp, vertical = 2.dp)
                    ) {
                        currentRows.forEach { rowKeys ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = rowVerticalPadding)
                            ) {
                                rowKeys.forEach { key ->
                                    KeyCap(
                                        key = key,
                                        colors = colors,
                                        heightDp = calculatedHeightDp,
                                        characterFontSize = calculatedFontSize,
                                        altCharacterFontSize = calculatedAltFontSize,
                                        horizontalPaddingDp = keyHorizontalPadding,
                                        verticalPaddingDp = keyVerticalPadding,
                                        cornerRadiusDp = keyCornerRadius,
                                        holdForSymbols = holdForSymbolsEnabled,
                                        onKeyClick = { handleKeyAction(it.action) },
                                        onKeyLongClick = { clickedKey ->
                                            if (clickedKey.action is KeyAction.Space && installedLanguages.size > 1) {
                                                handleKeyAction(KeyAction.SwitchLanguage)
                                            } else if (isSinhala && clickedKey.action is KeyAction.InsertText && KeyboardLayoutGenerator.isSinhalaConsonant(clickedKey.action.text)) {
                                                selectedSinhalaLetter = clickedKey.action.text
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tablet / Large Screen Inset Fix: Prevent bottom row of keys from being covered by the IME switcher / close bar
        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .height(if (isTablet) 58.dp else 22.dp)
        )
        }
    }
}

@Composable
private fun TabletSplitKeyboardRows(
    rows: List<List<KeyItem>>,
    colors: KeyboardColors,
    calculatedHeightDp: Int,
    calculatedFontSize: Float,
    calculatedAltFontSize: Float,
    keyHorizontalPadding: Float,
    keyVerticalPadding: Float,
    keyCornerRadius: Int,
    rowVerticalPadding: Dp,
    gapWidthDp: Dp,
    holdForSymbolsEnabled: Boolean,
    onKeyClick: (KeyItem) -> Unit,
    onKeyLongClick: (KeyItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag("tablet_split_keyboard_layout")
    ) {
        rows.forEach { rowKeys ->
            val spaceIndex = rowKeys.indexOfFirst { it.action is KeyAction.Space }
            if (spaceIndex != -1) {
                // Bottom row with space: split with middle space keys on either side
                val spaceKey = rowKeys[spaceIndex]
                val keysBefore = rowKeys.subList(0, spaceIndex)
                val keysAfter = rowKeys.subList(spaceIndex + 1, rowKeys.size)
                val leftSpace = spaceKey.copy(primaryText = " ", weight = 2.2f, testTag = "key_split_space_left")
                val rightSpace = spaceKey.copy(primaryText = " ", weight = 2.2f, testTag = "key_split_space_right")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = rowVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left half
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        keysBefore.forEach { key ->
                            KeyCap(
                                key = key,
                                colors = colors,
                                heightDp = calculatedHeightDp,
                                characterFontSize = calculatedFontSize,
                                altCharacterFontSize = calculatedAltFontSize,
                                horizontalPaddingDp = keyHorizontalPadding,
                                verticalPaddingDp = keyVerticalPadding,
                                cornerRadiusDp = keyCornerRadius,
                                holdForSymbols = holdForSymbolsEnabled,
                                onKeyClick = onKeyClick,
                                onKeyLongClick = onKeyLongClick
                            )
                        }
                        KeyCap(
                            key = leftSpace,
                            colors = colors,
                            heightDp = calculatedHeightDp,
                            characterFontSize = calculatedFontSize,
                            altCharacterFontSize = calculatedAltFontSize,
                            horizontalPaddingDp = keyHorizontalPadding,
                            verticalPaddingDp = keyVerticalPadding,
                            cornerRadiusDp = keyCornerRadius,
                            holdForSymbols = holdForSymbolsEnabled,
                            onKeyClick = onKeyClick,
                            onKeyLongClick = onKeyLongClick
                        )
                    }

                    // Middle thumb space gap
                    Spacer(modifier = Modifier.width(gapWidthDp))

                    // Right half
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KeyCap(
                            key = rightSpace,
                            colors = colors,
                            heightDp = calculatedHeightDp,
                            characterFontSize = calculatedFontSize,
                            altCharacterFontSize = calculatedAltFontSize,
                            horizontalPaddingDp = keyHorizontalPadding,
                            verticalPaddingDp = keyVerticalPadding,
                            cornerRadiusDp = keyCornerRadius,
                            holdForSymbols = holdForSymbolsEnabled,
                            onKeyClick = onKeyClick,
                            onKeyLongClick = onKeyLongClick
                        )
                        keysAfter.forEach { key ->
                            KeyCap(
                                key = key,
                                colors = colors,
                                heightDp = calculatedHeightDp,
                                characterFontSize = calculatedFontSize,
                                altCharacterFontSize = calculatedAltFontSize,
                                horizontalPaddingDp = keyHorizontalPadding,
                                verticalPaddingDp = keyVerticalPadding,
                                cornerRadiusDp = keyCornerRadius,
                                holdForSymbols = holdForSymbolsEnabled,
                                onKeyClick = onKeyClick,
                                onKeyLongClick = onKeyLongClick
                            )
                        }
                    }
                }
            } else {
                // Regular letter/symbol rows: split into left half and right half with middle gap
                val mid = (rowKeys.size + 1) / 2
                val leftKeys = rowKeys.take(mid)
                val rightKeys = rowKeys.drop(mid)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = rowVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        leftKeys.forEach { key ->
                            KeyCap(
                                key = key,
                                colors = colors,
                                heightDp = calculatedHeightDp,
                                characterFontSize = calculatedFontSize,
                                altCharacterFontSize = calculatedAltFontSize,
                                horizontalPaddingDp = keyHorizontalPadding,
                                verticalPaddingDp = keyVerticalPadding,
                                cornerRadiusDp = keyCornerRadius,
                                holdForSymbols = holdForSymbolsEnabled,
                                onKeyClick = onKeyClick,
                                onKeyLongClick = onKeyLongClick
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(gapWidthDp))

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rightKeys.forEach { key ->
                            KeyCap(
                                key = key,
                                colors = colors,
                                heightDp = calculatedHeightDp,
                                characterFontSize = calculatedFontSize,
                                altCharacterFontSize = calculatedAltFontSize,
                                horizontalPaddingDp = keyHorizontalPadding,
                                verticalPaddingDp = keyVerticalPadding,
                                cornerRadiusDp = keyCornerRadius,
                                holdForSymbols = holdForSymbolsEnabled,
                                onKeyClick = onKeyClick,
                                onKeyLongClick = onKeyLongClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletHalfSizeKeyboardRows(
    rows: List<List<KeyItem>>,
    colors: KeyboardColors,
    side: String,
    calculatedHeightDp: Int,
    calculatedFontSize: Float,
    calculatedAltFontSize: Float,
    keyHorizontalPadding: Float,
    keyVerticalPadding: Float,
    keyCornerRadius: Int,
    rowVerticalPadding: Dp,
    holdForSymbolsEnabled: Boolean,
    onToggleSide: () -> Unit,
    onKeyClick: (KeyItem) -> Unit,
    onKeyLongClick: (KeyItem) -> Unit
) {
    val keysBlock: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            rows.forEach { rowKeys ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = rowVerticalPadding)
                ) {
                    rowKeys.forEach { key ->
                        KeyCap(
                            key = key,
                            colors = colors,
                            heightDp = (calculatedHeightDp * 0.95f).toInt(),
                            characterFontSize = calculatedFontSize * 0.9f,
                            altCharacterFontSize = calculatedAltFontSize * 0.9f,
                            horizontalPaddingDp = keyHorizontalPadding * 0.85f,
                            verticalPaddingDp = keyVerticalPadding * 0.85f,
                            cornerRadiusDp = keyCornerRadius,
                            holdForSymbols = holdForSymbolsEnabled,
                            onKeyClick = onKeyClick,
                            onKeyLongClick = onKeyLongClick
                        )
                    }
                }
            }
        }
    }

    val spaceAndToolsSide: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Side switch button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.functionKeyBackground)
                    .clickable { onToggleSide() }
                    .padding(horizontal = 8.dp)
                    .testTag("btn_switch_half_side"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (side == "left") "Dock Right ➔" else "⬅ Dock Left",
                    color = colors.letterKeyTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Dedicated large Thumb Space Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((calculatedHeightDp * 1.55f).dp)
                    .clip(RoundedCornerShape(keyCornerRadius.dp))
                    .background(colors.letterKeyBackground)
                    .clickable {
                        onKeyClick(
                            KeyItem(
                                primaryText = " ",
                                action = KeyAction.Space,
                                testTag = "btn_half_size_space"
                            )
                        )
                    }
                    .testTag("btn_half_size_space"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SPACE",
                        color = colors.letterKeyTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Thumb Key",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 10.sp
                    )
                }
            }

            // Quick Actions: Backspace & Enter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(calculatedHeightDp.dp)
                        .clip(RoundedCornerShape(keyCornerRadius.dp))
                        .background(colors.functionKeyBackground)
                        .clickable {
                            onKeyClick(
                                KeyItem(
                                    primaryText = "⌫",
                                    action = KeyAction.Backspace,
                                    testTag = "btn_half_size_backspace"
                                )
                            )
                        }
                        .testTag("btn_half_size_backspace"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⌫ Del", color = colors.letterKeyTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(calculatedHeightDp.dp)
                        .clip(RoundedCornerShape(keyCornerRadius.dp))
                        .background(colors.enterKeyBackground)
                        .clickable {
                            onKeyClick(
                                KeyItem(
                                    primaryText = "⏎",
                                    action = KeyAction.Enter,
                                    testTag = "btn_half_size_enter"
                                )
                            )
                        }
                        .testTag("btn_half_size_enter"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⏎ Enter", color = colors.enterKeyTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Cursor Navigation row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.functionKeyBackground)
                        .clickable {
                            onKeyClick(
                                KeyItem(
                                    primaryText = "◀",
                                    action = KeyAction.CursorLeft,
                                    testTag = "btn_half_size_left"
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "◀ Left", color = colors.letterKeyTextColor, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.functionKeyBackground)
                        .clickable {
                            onKeyClick(
                                KeyItem(
                                    primaryText = "▶",
                                    action = KeyAction.CursorRight,
                                    testTag = "btn_half_size_right"
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Right ▶", color = colors.letterKeyTextColor, fontSize = 11.sp)
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .testTag("tablet_half_size_keyboard_layout"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (side == "left") {
            Box(modifier = Modifier.weight(0.66f)) {
                keysBlock()
            }
            Box(modifier = Modifier.weight(0.34f)) {
                spaceAndToolsSide()
            }
        } else {
            Box(modifier = Modifier.weight(0.34f)) {
                spaceAndToolsSide()
            }
            Box(modifier = Modifier.weight(0.66f)) {
                keysBlock()
            }
        }
    }
}
