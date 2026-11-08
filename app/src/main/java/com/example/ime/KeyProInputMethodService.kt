package com.example.ime

import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.hardware.input.InputManager
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.AppDatabase
import com.example.data.SnippetRepository
import com.example.data.WordSuggestionManager
import com.example.data.cleanup.WordPruneWorker
import com.example.ui.keyboard.KeyProKeyboardView
import com.example.ui.keyboard.model.KeyAction
import com.example.ui.keyboard.model.KeyboardThemes
import com.example.ui.keyboard.util.KeyboardPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class KeyProInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val mViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private lateinit var repository: SnippetRepository
    private lateinit var keyboardPrefs: KeyboardPreferences
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var inputDeviceListener: InputManager.InputDeviceListener? = null

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = mViewModelStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val openSessionTrigger = MutableStateFlow(0L)

    // Composing buffer state and previous word tracking for bigrams
    private val composingBufferFlow = MutableStateFlow("")
    private val prevWordFlow = MutableStateFlow("")
    private var currentComposingBuffer: String = ""
    private var currentPrevWord: String? = null

    // Anti-typo learning: staged word committed on space/enter
    private var pendingLearnedWord: String? = null
    private var pendingPrevWord: String? = null
    private var pendingCommitTimestamp: Long = 0L

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val db = AppDatabase.getDatabase(this)
        repository = SnippetRepository(db.snippetDao())
        keyboardPrefs = KeyboardPreferences.getInstance(this)

        // Schedule weekly WorkManager prune for 6+ month stale low-frequency words (Part 2, D)
        WordPruneWorker.scheduleWeeklyPrune(this)

        // Clipboard history auto-save and cross-device sync listener
        try {
            val syncManager = com.example.sync.ClipboardSyncManager.getInstance(this)
            if (keyboardPrefs.autoDeleteClipboardEnabled.value) {
                serviceScope.launch(Dispatchers.IO) {
                    repository.purgeExpiredHistory(keyboardPrefs.clipboardRetentionDays.value)
                }
            }

            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
                try {
                    if (cm != null && cm.hasPrimaryClip()) {
                        val clip = cm.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val item = clip.getItemAt(0)
                            val uri = item.uri
                            if (uri != null && (clip.description.hasMimeType("image/*") || uri.toString().contains("image") || uri.scheme == "content")) {
                                serviceScope.launch(Dispatchers.IO) {
                                    val persistentPath = com.example.data.ClipboardImageHelper.persistImageLocally(this@KeyProInputMethodService, uri.toString())
                                    val id = repository.saveImageToClipboard(persistentPath)
                                    val entity = com.example.data.SnippetEntity(
                                        id = id,
                                        title = "Image Clip",
                                        content = persistentPath,
                                        isPinned = false,
                                        category = "Images",
                                        imageUri = persistentPath,
                                        isImage = true,
                                        mimeType = "image/*"
                                    )
                                    syncManager.broadcastSnippet(entity)
                                }
                            } else {
                                val text = item.text?.toString()
                                if (!text.isNullOrBlank()) {
                                    serviceScope.launch(Dispatchers.IO) {
                                        val saved = repository.saveToClipboardHistory(text)
                                        if (saved != null) {
                                            syncManager.broadcastSnippet(saved)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
            cm?.addPrimaryClipChangedListener(clipboardListener)
        } catch (_: Exception) {}

        // Listen for hardware/physical keyboard connections
        try {
            val im = getSystemService(Context.INPUT_SERVICE) as? InputManager
            val listener = object : InputManager.InputDeviceListener {
                override fun onInputDeviceAdded(deviceId: Int) {
                    checkAndHideIfPhysicalKeyboardConnected()
                }

                override fun onInputDeviceRemoved(deviceId: Int) {}

                override fun onInputDeviceChanged(deviceId: Int) {
                    checkAndHideIfPhysicalKeyboardConnected()
                }
            }
            inputDeviceListener = listener
            im?.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))
        } catch (_: Exception) {}
    }

    // Revert transliteration: pair of (latinWord, sinhalaWordWithSpace)
    private var lastTransliteration: Pair<String, String>? = null

    /**
     * Privacy check: detects passwords, emails, URIs, TYPE_NULL, and IME_FLAG_NO_PERSONALIZED_LEARNING.
     */
    private fun isSensitiveInput(info: EditorInfo?): Boolean {
        if (info == null) return false
        val inputType = info.inputType
        if (inputType == EditorInfo.TYPE_NULL) return true
        val variation = inputType and EditorInfo.TYPE_MASK_VARIATION
        val isPassword = variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                (inputType and EditorInfo.TYPE_MASK_CLASS == EditorInfo.TYPE_CLASS_NUMBER &&
                        variation == EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD)
        val isEmail = variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
        val isUri = variation == EditorInfo.TYPE_TEXT_VARIATION_URI
        val noPersonalizedLearning = (info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0

        return isPassword || isEmail || isUri || noPersonalizedLearning
    }

    /**
     * Anti-typo learning: commit staged word if user did not edit or backspace before next word.
     */
    private fun commitPendingWordIfAny() {
        val word = pendingLearnedWord ?: return
        val prev = pendingPrevWord
        pendingLearnedWord = null
        pendingPrevWord = null

        val sensitive = isSensitiveInput(currentInputEditorInfo)
        if (!sensitive && !WordSuggestionManager.isInvalidToLearn(word)) {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getDatabase(this@KeyProInputMethodService)
                    val manager = WordSuggestionManager(
                        wordFrequencyDao = db.wordFrequencyDao(),
                        bigramFrequencyDao = db.bigramFrequencyDao(),
                        correctionPairDao = db.correctionPairDao(),
                        translitChoiceDao = db.translitChoiceDao(),
                        dictionaryRepo = com.example.data.DictionaryRepository.getInstance(this@KeyProInputMethodService)
                    )
                    manager.recordWordUsed(word, prev, isPrivateOrSensitive = false)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Discard pending word if user edits it at all before the next word is committed.
     */
    private fun discardPendingWordOnEdit() {
        pendingLearnedWord = null
        pendingPrevWord = null
    }

    fun getDynamicPrevWord(): String? {
        val ic = currentInputConnection ?: return null
        val text = ic.getTextBeforeCursor(60, 0)?.toString() ?: return null
        val words = text.trimEnd().split(Regex("\\s+"))
        return words.lastOrNull { it.isNotBlank() && !WordSuggestionManager.isInvalidToLearn(it) }
    }

    fun isPhysicalKeyboardConnected(): Boolean {
        try {
            val im = getSystemService(Context.INPUT_SERVICE) as? InputManager
            if (im != null) {
                for (id in im.inputDeviceIds) {
                    val device = im.getInputDevice(id) ?: continue
                    if (!device.isVirtual && (device.sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) {
                        if (device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC) {
                            return true
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        val config = resources.configuration
        return config.keyboard != Configuration.KEYBOARD_NOKEYS &&
                config.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES
    }

    private fun checkAndHideIfPhysicalKeyboardConnected() {
        if (keyboardPrefs.hideOnPhysicalKeyboard.value && isPhysicalKeyboardConnected()) {
            requestHideSelf(0)
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onEvaluateInputViewShown(): Boolean {
        if (keyboardPrefs.hideOnPhysicalKeyboard.value && isPhysicalKeyboardConnected()) {
            return false
        }
        val config = resources.configuration
        return config.keyboard == Configuration.KEYBOARD_NOKEYS
                || config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES
    }

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@KeyProInputMethodService)
            setViewTreeViewModelStoreOwner(this@KeyProInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@KeyProInputMethodService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val allSnippets by repository.allSnippets.collectAsState(initial = emptyList())
                val pinnedSnippets by repository.pinnedSnippets.collectAsState(initial = emptyList())
                val historySnippets by repository.historySnippets.collectAsState(initial = emptyList())
                val soundEnabled by keyboardPrefs.soundEnabled.collectAsState()
                val hapticEnabled by keyboardPrefs.hapticEnabled.collectAsState()
                val themeType by keyboardPrefs.themeType.collectAsState()
                val laptopBarVisible by keyboardPrefs.laptopBarVisible.collectAsState()
                val sessionTrigger by openSessionTrigger.collectAsState()
                val syncedComposingBuffer by composingBufferFlow.collectAsState()
                val syncedPrevWord by prevWordFlow.collectAsState()

                val colors = if (themeType == com.example.ui.keyboard.model.KeyboardThemeType.CUSTOM) {
                    keyboardPrefs.getCustomKeyboardColors()
                } else {
                    KeyboardThemes.getTheme(themeType)
                }

                KeyProKeyboardView(
                    colors = colors,
                    allSnippets = allSnippets,
                    pinnedSnippets = pinnedSnippets,
                    historySnippets = historySnippets,
                    isSoundEnabled = soundEnabled,
                    isHapticEnabled = hapticEnabled,
                    initialLaptopBarVisible = laptopBarVisible,
                    openSessionTrigger = sessionTrigger,
                    syncedWordBuffer = syncedComposingBuffer,
                    syncedPrevWord = syncedPrevWord,
                    onAction = { action -> handleImeAction(action, pinnedSnippets) },
                    onTogglePin = { snippet ->
                        serviceScope.launch {
                            val newPinned = repository.togglePin(snippet.id, snippet.isPinned)
                            val syncManager = com.example.sync.ClipboardSyncManager.getInstance(this@KeyProInputMethodService)
                            syncManager.broadcastSnippet(snippet.copy(isPinned = newPinned))
                        }
                    },
                    onDeleteSnippet = { id ->
                        serviceScope.launch { repository.deleteSnippet(id) }
                    },
                    onSaveSnippet = { title, content, isPinned, category, shortcut ->
                        serviceScope.launch {
                            val id = repository.insertSnippet(title, content, isPinned, category, shortcut)
                            if (isPinned) {
                                val entity = com.example.data.SnippetEntity(
                                    id = id,
                                    title = title,
                                    content = content,
                                    isPinned = true,
                                    category = category,
                                    shortcut = shortcut,
                                    timestamp = System.currentTimeMillis()
                                )
                                val syncManager = com.example.sync.ClipboardSyncManager.getInstance(this@KeyProInputMethodService)
                                syncManager.broadcastSnippet(entity)
                            }
                        }
                    },
                    onClearHistory = {
                        serviceScope.launch { repository.clearUnpinned() }
                    },
                    onOpenSettings = {}
                )
            }
        }
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (keyboardPrefs.hideOnPhysicalKeyboard.value && isPhysicalKeyboardConnected()) {
            requestHideSelf(0)
            return
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // Reset composing state
        currentComposingBuffer = ""
        composingBufferFlow.value = ""
        pendingLearnedWord = null
        pendingPrevWord = null

        // Try reading word before cursor to set previous word for bigrams
        currentInputConnection?.let { ic ->
            val textBefore = ic.getTextBeforeCursor(30, 0)?.toString() ?: ""
            val lastWord = textBefore.trim().split(Regex("\\s+")).lastOrNull() ?: ""
            if (lastWord.isNotBlank() && !WordSuggestionManager.isInvalidToLearn(lastWord)) {
                currentPrevWord = lastWord
                prevWordFlow.value = lastWord
            } else {
                currentPrevWord = null
                prevWordFlow.value = ""
            }
        }

        try {
            keyboardPrefs.setCurrentLanguage("en")
        } catch (_: Exception) {}
        openSessionTrigger.value++
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (keyboardPrefs.hideOnPhysicalKeyboard.value && isPhysicalKeyboardConnected()) {
            requestHideSelf(0)
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        commitPendingWordIfAny()
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    private fun handleImeAction(action: KeyAction, pinnedSnippets: List<com.example.data.SnippetEntity> = emptyList()) {
        val ic = currentInputConnection ?: return

        when (action) {
            is KeyAction.InsertText -> {
                lastTransliteration = null
                // Direct commit: completely prevents white underline and selection reset glitches
                ic.commitText(action.text, 1)
                prevWordFlow.value = getDynamicPrevWord() ?: ""
            }
            is KeyAction.Space -> {
                val currentLang = keyboardPrefs.currentLanguage.value
                val before = ic.getTextBeforeCursor(40, 0)?.toString() ?: ""
                val lastWord = before.split(Regex("\\s+")).lastOrNull() ?: ""

                // English -> Sinhala Transliteration Keyboard (si-trans)
                if (currentLang == "si-trans" && lastWord.isNotBlank() && lastWord.all { it.isLetter() }) {
                    val transliterated = com.example.ui.keyboard.util.SinhalaTransliterator.transliterateWord(lastWord)
                    val committedStr = transliterated + " "
                    ic.deleteSurroundingText(lastWord.length, 0)
                    ic.commitText(committedStr, 1)
                    lastTransliteration = Pair(lastWord, committedStr)
                    commitPendingWordIfAny()
                    pendingLearnedWord = transliterated
                    pendingPrevWord = currentPrevWord
                    currentPrevWord = transliterated
                    prevWordFlow.value = transliterated
                    return
                }

                lastTransliteration = null

                // Check shortcut expansion
                val matched = if (lastWord.isNotBlank()) {
                    pinnedSnippets.firstOrNull { it.shortcut.isNotBlank() && it.shortcut.equals(lastWord, ignoreCase = true) }
                } else null

                if (matched != null) {
                    ic.deleteSurroundingText(lastWord.length, 0)
                    ic.commitText(matched.content + " ", 1)
                } else {
                    ic.commitText(" ", 1)
                }

                // Commit previous pending word, then stage current word
                commitPendingWordIfAny()
                if (lastWord.isNotBlank()) {
                    pendingLearnedWord = lastWord
                    pendingPrevWord = currentPrevWord
                    currentPrevWord = lastWord
                    prevWordFlow.value = lastWord
                }
            }
            is KeyAction.ExpandShortcut -> {
                lastTransliteration = null
                if (action.shortcutText.isNotEmpty()) {
                    val before = ic.getTextBeforeCursor(action.shortcutText.length, 0)?.toString()
                    if (before == action.shortcutText) {
                        ic.deleteSurroundingText(action.shortcutText.length, 0)
                    }
                }
                ic.commitText(action.fullContent, 1)
                prevWordFlow.value = getDynamicPrevWord() ?: ""
            }
            is KeyAction.ApplySuggestion -> {
                lastTransliteration = null
                val prefixLen = action.typedWord.length
                if (prefixLen > 0) {
                    val before = ic.getTextBeforeCursor(prefixLen, 0)?.toString() ?: ""
                    if (before.equals(action.typedWord, ignoreCase = true)) {
                        ic.deleteSurroundingText(prefixLen, 0)
                    } else {
                        val textBefore = ic.getTextBeforeCursor(40, 0)?.toString() ?: ""
                        val lastWord = textBefore.split(Regex("\\s+")).lastOrNull() ?: ""
                        if (lastWord.isNotBlank() && (action.suggestedWord.startsWith(lastWord, ignoreCase = true) || lastWord.equals(action.typedWord, ignoreCase = true))) {
                            ic.deleteSurroundingText(lastWord.length, 0)
                        }
                    }
                }
                ic.commitText(action.suggestedWord + " ", 1)

                // Record correction pair if typed word was replaced by suggestion
                if (action.typedWord.isNotBlank() && !action.typedWord.equals(action.suggestedWord, ignoreCase = true)) {
                    val currentLang = keyboardPrefs.currentLanguage.value
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            val db = AppDatabase.getDatabase(this@KeyProInputMethodService)
                            val manager = WordSuggestionManager(
                                wordFrequencyDao = db.wordFrequencyDao(),
                                bigramFrequencyDao = db.bigramFrequencyDao(),
                                correctionPairDao = db.correctionPairDao(),
                                translitChoiceDao = db.translitChoiceDao(),
                                dictionaryRepo = com.example.data.DictionaryRepository.getInstance(this@KeyProInputMethodService)
                            )
                            manager.recordCorrection(action.typedWord, action.suggestedWord, currentLang)
                            if (currentLang == "si-trans") {
                                manager.recordTranslitChoice(action.typedWord, action.suggestedWord)
                            }
                        } catch (_: Exception) {}
                    }
                }

                commitPendingWordIfAny()
                pendingLearnedWord = action.suggestedWord
                pendingPrevWord = currentPrevWord
                currentPrevWord = action.suggestedWord
                prevWordFlow.value = action.suggestedWord
            }
            is KeyAction.Backspace -> {
                // If user hits Backspace immediately after transliteration conversion, revert to original Latin
                if (lastTransliteration != null) {
                    val (origLatin, converted) = lastTransliteration!!
                    lastTransliteration = null
                    ic.deleteSurroundingText(converted.length, 0)
                    ic.commitText(origLatin, 1)
                    discardPendingWordOnEdit()
                    prevWordFlow.value = getDynamicPrevWord() ?: ""
                    return
                }

                lastTransliteration = null
                discardPendingWordOnEdit()
                ic.deleteSurroundingText(1, 0)
                prevWordFlow.value = getDynamicPrevWord() ?: ""
            }
            is KeyAction.DeleteForward -> {
                lastTransliteration = null
                discardPendingWordOnEdit()
                ic.deleteSurroundingText(0, 1)
                prevWordFlow.value = getDynamicPrevWord() ?: ""
            }
            is KeyAction.Enter -> {
                lastTransliteration = null
                val before = ic.getTextBeforeCursor(40, 0)?.toString() ?: ""
                val lastWord = before.split(Regex("\\s+")).lastOrNull() ?: ""

                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))

                commitPendingWordIfAny()
                if (lastWord.isNotBlank()) {
                    pendingLearnedWord = lastWord
                    pendingPrevWord = currentPrevWord
                    currentPrevWord = lastWord
                    prevWordFlow.value = lastWord
                }
            }
            is KeyAction.Tab -> {
                ic.commitText("\t", 1)
            }
            is KeyAction.Escape -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE))
            }
            is KeyAction.CursorLeft -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
            }
            is KeyAction.CursorRight -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
            }
            is KeyAction.CursorUp -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP))
            }
            is KeyAction.CursorDown -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN))
            }
            is KeyAction.Home -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME))
            }
            is KeyAction.End -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END))
            }
            is KeyAction.PageUp -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_UP))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_UP))
            }
            is KeyAction.PageDown -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_DOWN))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_DOWN))
            }
            is KeyAction.SelectAll -> {
                ic.performContextMenuAction(android.R.id.selectAll)
            }
            is KeyAction.Copy -> {
                ic.performContextMenuAction(android.R.id.copy)
            }
            is KeyAction.Cut -> {
                ic.performContextMenuAction(android.R.id.cut)
            }
            is KeyAction.Paste -> {
                try {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = cm?.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val text = clip.getItemAt(0).text?.toString()
                        if (!text.isNullOrEmpty()) {
                            ic.commitText(text, 1)
                        } else {
                            ic.performContextMenuAction(android.R.id.paste)
                        }
                    } else {
                        ic.performContextMenuAction(android.R.id.paste)
                    }
                } catch (_: Exception) {
                    ic.performContextMenuAction(android.R.id.paste)
                }
            }
            is KeyAction.Undo -> {
                ic.performContextMenuAction(android.R.id.undo)
            }
            is KeyAction.Redo -> {
                ic.performContextMenuAction(android.R.id.redo)
            }
            is KeyAction.FunctionKey -> {
                val keycode = when (action.fNumber) {
                    1 -> KeyEvent.KEYCODE_F1
                    2 -> KeyEvent.KEYCODE_F2
                    3 -> KeyEvent.KEYCODE_F3
                    4 -> KeyEvent.KEYCODE_F4
                    5 -> KeyEvent.KEYCODE_F5
                    6 -> KeyEvent.KEYCODE_F6
                    7 -> KeyEvent.KEYCODE_F7
                    8 -> KeyEvent.KEYCODE_F8
                    9 -> KeyEvent.KEYCODE_F9
                    10 -> KeyEvent.KEYCODE_F10
                    11 -> KeyEvent.KEYCODE_F11
                    12 -> KeyEvent.KEYCODE_F12
                    else -> KeyEvent.KEYCODE_UNKNOWN
                }
                if (keycode != KeyEvent.KEYCODE_UNKNOWN) {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keycode))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keycode))
                }
            }
            is KeyAction.InsertSinhalaPillam -> {
                val before = ic.getTextBeforeCursor(1, 0)
                if (!before.isNullOrEmpty() && isSinhalaConsonant(before.first())) {
                    ic.commitText(action.modifier, 1)
                } else {
                    ic.commitText(action.baseWithPillam, 1)
                }
            }
            is KeyAction.SwitchLanguage -> {
                val prefs = KeyboardPreferences.getInstance(this)
                val nextLang = prefs.cycleLanguage()
                val langName = when (nextLang) {
                    "si" -> "සිංහල (Wijesekara)"
                    "si-trans" -> "සිංහල (English ➔ Sinhala)"
                    else -> "English"
                }
                android.widget.Toast.makeText(this, "Language: $langName", android.widget.Toast.LENGTH_SHORT).show()
            }
            is KeyAction.InsertSticker -> {
                try {
                    val file = java.io.File(action.filePath)
                    if (!file.exists() || file.length() == 0L) {
                        android.widget.Toast.makeText(
                            this,
                            "Sticker error: File not found or empty (${file.name})",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    val editorInfo = currentInputEditorInfo
                    val isAnim = action.isAnimated || com.example.ui.keyboard.util.WebpAnimationHelper.isAnimated(file)

                    val candidates = com.example.ui.keyboard.util.StickerFormatHelper.resolveCandidatesForTarget(
                        context = this,
                        stickerFile = file,
                        gifPath = action.gifPath,
                        staticPngPath = action.staticPngPath,
                        isAnimatedSticker = isAnim,
                        editorInfo = editorInfo,
                        declaredMimeType = action.mimeType
                    )

                    var committed = false
                    var lastUri: android.net.Uri? = null
                    val targetPkg = editorInfo?.packageName
                    var lastErrorMsg: String? = null

                    if (candidates.isEmpty()) {
                        lastErrorMsg = "Target application (${targetPkg ?: "current field"}) does not accept sticker image formats"
                    }

                    for (candidate in candidates) {
                        val targetFile = candidate.file
                        if (!targetFile.exists() || targetFile.length() == 0L) {
                            lastErrorMsg = "Candidate image file missing (${targetFile.name})"
                            continue
                        }

                        val contentUri = try {
                            androidx.core.content.FileProvider.getUriForFile(
                                this,
                                "${packageName}.fileprovider",
                                targetFile
                            )
                        } catch (e: Exception) {
                            lastErrorMsg = "FileProvider error: ${e.localizedMessage ?: e.message}"
                            null
                        } ?: continue

                        lastUri = contentUri

                        // EXACT single MIME type in ClipDescription
                        val description = android.content.ClipDescription(
                            action.name,
                            arrayOf(candidate.mimeType)
                        )
                        val inputContentInfo = androidx.core.view.inputmethod.InputContentInfoCompat(
                            contentUri,
                            description,
                            null
                        )

                        var flags = 0
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                            flags = flags or androidx.core.view.inputmethod.InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                        }

                        // On API 25 and below, also grant URI read permission manually
                        if (targetPkg != null) {
                            try {
                                grantUriPermission(
                                    targetPkg,
                                    contentUri,
                                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (e: Exception) {
                                lastErrorMsg = "Permission error: ${e.localizedMessage ?: e.message}"
                            }
                        }

                        committed = if (editorInfo != null) {
                            androidx.core.view.inputmethod.InputConnectionCompat.commitContent(
                                ic,
                                editorInfo,
                                inputContentInfo,
                                flags,
                                null
                            )
                        } else false

                        if (committed) {
                            lastErrorMsg = null
                            break
                        } else {
                            lastErrorMsg = "Target app (${targetPkg ?: "unknown"}) rejected ${candidate.mimeType}"
                            // If commitContent returned false on API 25 and below, revoke
                            if (targetPkg != null && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N_MR1) {
                                try {
                                    revokeUriPermission(contentUri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                } catch (_: Exception) {}
                            }
                        }
                    }

                    if (!committed) {
                        val fallbackFile = action.gifPath?.let { java.io.File(it) }?.takeIf { it.exists() && it.length() > 0 }
                            ?: file.takeIf { it.exists() && it.length() > 0 }

                        val fallbackUri = lastUri ?: (fallbackFile?.let {
                            try {
                                androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", it)
                            } catch (e: Exception) {
                                lastErrorMsg = "FileProvider error: ${e.localizedMessage ?: e.message}"
                                null
                            }
                        })

                        if (fallbackUri != null) {
                            try {
                                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = android.content.ClipData.newUri(contentResolver, action.name, fallbackUri)
                                cm?.setPrimaryClip(clip)
                                if (targetPkg != null) {
                                    try {
                                        grantUriPermission(
                                            targetPkg,
                                            fallbackUri,
                                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    } catch (_: Exception) {}
                                }
                                val errorDetail = if (!lastErrorMsg.isNullOrBlank()) " ($lastErrorMsg)" else ""
                                android.widget.Toast.makeText(
                                    this,
                                    "Sticker copied to clipboard$errorDetail. Paste it into your message.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    this,
                                    "Sticker error: ${e.localizedMessage ?: e.message ?: lastErrorMsg ?: "Failed to send or copy sticker"}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            val failureReason = lastErrorMsg ?: "Target app rejected sticker and no compatible file could be prepared"
                            android.widget.Toast.makeText(
                                this,
                                "Sticker send error: $failureReason",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        this,
                        "Sticker send error: ${e.localizedMessage ?: e.message ?: "Unexpected error"}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
            else -> {}
        }
    }

    private fun isSinhalaConsonant(c: Char): Boolean {
        return c in '\u0D9A'..'\u0DC6'
    }

    override fun onDestroy() {
        commitPendingWordIfAny()
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardListener?.let { cm?.removePrimaryClipChangedListener(it) }
        } catch (_: Exception) {}

        try {
            val im = getSystemService(Context.INPUT_SERVICE) as? InputManager
            inputDeviceListener?.let { im?.unregisterInputDeviceListener(it) }
        } catch (_: Exception) {}

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        mViewModelStore.clear()
        super.onDestroy()
    }
}
