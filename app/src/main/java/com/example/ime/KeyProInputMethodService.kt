package com.example.ime

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.ui.keyboard.KeyProKeyboardView
import com.example.ui.keyboard.model.KeyAction
import com.example.ui.keyboard.model.KeyboardThemes
import com.example.ui.keyboard.util.KeyboardPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KeyProInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val mViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private lateinit var repository: SnippetRepository
    private lateinit var keyboardPrefs: KeyboardPreferences
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = mViewModelStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val db = AppDatabase.getDatabase(this)
        repository = SnippetRepository(db.snippetDao())
        keyboardPrefs = KeyboardPreferences.getInstance(this)

        // Clipboard history auto-save listener (Request #4)
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
                try {
                    if (cm != null && cm.hasPrimaryClip()) {
                        val clip = cm.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val text = clip.getItemAt(0).text?.toString()
                            if (!text.isNullOrBlank()) {
                                serviceScope.launch(Dispatchers.IO) {
                                    repository.saveToClipboardHistory(text)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
            cm?.addPrimaryClipChangedListener(clipboardListener)
        } catch (_: Exception) {}
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onEvaluateInputViewShown(): Boolean = true

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
                    onAction = { action -> handleImeAction(action, pinnedSnippets) },
                    onTogglePin = { snippet ->
                        serviceScope.launch { repository.togglePin(snippet.id, snippet.isPinned) }
                    },
                    onDeleteSnippet = { id ->
                        serviceScope.launch { repository.deleteSnippet(id) }
                    },
                    onSaveSnippet = { title, content, isPinned, category, shortcut ->
                        serviceScope.launch { repository.insertSnippet(title, content, isPinned, category, shortcut) }
                    },
                    onClearHistory = {
                        serviceScope.launch { repository.clearUnpinned() }
                    },
                    onOpenSettings = {
                        // Handled in-keyboard without opening the app (Request #3)
                    }
                )
            }
        }
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    private fun handleImeAction(action: KeyAction, pinnedSnippets: List<com.example.data.SnippetEntity> = emptyList()) {
        val ic = currentInputConnection ?: return

        when (action) {
            is KeyAction.InsertText -> {
                ic.commitText(action.text, 1)
            }
            is KeyAction.Space -> {
                // Check if the preceding word matches a pinned snippet's shortcut (Request #2)
                val before = ic.getTextBeforeCursor(40, 0)?.toString() ?: ""
                val lastWord = before.split(Regex("\\s+")).lastOrNull() ?: ""
                val matched = if (lastWord.isNotBlank()) {
                    pinnedSnippets.firstOrNull { it.shortcut.isNotBlank() && it.shortcut.equals(lastWord, ignoreCase = true) }
                } else null

                if (matched != null) {
                    ic.deleteSurroundingText(lastWord.length, 0)
                    ic.commitText(matched.content + " ", 1)
                } else {
                    ic.commitText(" ", 1)
                    if (lastWord.isNotBlank()) {
                        serviceScope.launch {
                            try {
                                val wordDao = com.example.data.AppDatabase.getDatabase(this@KeyProInputMethodService).wordFrequencyDao()
                                com.example.data.WordSuggestionManager(wordDao).recordWordUsed(lastWord)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
            is KeyAction.ExpandShortcut -> {
                if (action.shortcutText.isNotEmpty()) {
                    val before = ic.getTextBeforeCursor(action.shortcutText.length, 0)?.toString()
                    if (before == action.shortcutText) {
                        ic.deleteSurroundingText(action.shortcutText.length, 0)
                    }
                }
                ic.commitText(action.fullContent, 1)
            }
            is KeyAction.ApplySuggestion -> {
                if (action.typedWord.isNotEmpty()) {
                    val before = ic.getTextBeforeCursor(action.typedWord.length, 0)?.toString()
                    if (before == action.typedWord) {
                        ic.deleteSurroundingText(action.typedWord.length, 0)
                    }
                }
                ic.commitText(action.suggestedWord + " ", 1)
                serviceScope.launch {
                    try {
                        val wordDao = com.example.data.AppDatabase.getDatabase(this@KeyProInputMethodService).wordFrequencyDao()
                        com.example.data.WordSuggestionManager(wordDao).recordWordUsed(action.suggestedWord)
                    } catch (_: Exception) {}
                }
            }
            is KeyAction.Backspace -> {
                ic.deleteSurroundingText(1, 0)
            }
            is KeyAction.DeleteForward -> {
                ic.deleteSurroundingText(0, 1)
            }
            is KeyAction.Enter -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
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
            is KeyAction.InsertSticker -> {
                try {
                    val file = java.io.File(action.filePath)
                    if (file.exists()) {
                        val contentUri = androidx.core.content.FileProvider.getUriForFile(
                            this,
                            "${packageName}.fileprovider",
                            file
                        )
                        val description = android.content.ClipDescription(
                            action.name,
                            arrayOf("image/webp", "image/png", "image/*")
                        )
                        val inputContentInfo = androidx.core.view.inputmethod.InputContentInfoCompat(
                            contentUri,
                            description,
                            null
                        )

                        val editorInfo = currentInputEditorInfo
                        var flags = 0
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                            flags = flags or androidx.core.view.inputmethod.InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                        }

                        val committed = if (editorInfo != null) {
                            androidx.core.view.inputmethod.InputConnectionCompat.commitContent(
                                ic,
                                editorInfo,
                                inputContentInfo,
                                flags,
                                null
                            )
                        } else false

                        // Also copy to clipboard for apps that support clipboard paste
                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = android.content.ClipData.newUri(contentResolver, action.name, contentUri)
                        cm?.setPrimaryClip(clip)

                        if (!committed) {
                            android.widget.Toast.makeText(
                                this,
                                "Sticker copied to clipboard! Paste it into your message.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (_: Exception) {
                    android.widget.Toast.makeText(this, "Could not send sticker", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            else -> {}
        }
    }

    override fun onDestroy() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardListener?.let { cm?.removePrimaryClipChangedListener(it) }
        } catch (_: Exception) {}

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        mViewModelStore.clear()
        super.onDestroy()
    }
}
