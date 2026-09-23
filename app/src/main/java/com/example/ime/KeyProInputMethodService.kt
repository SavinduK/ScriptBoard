package com.example.ime

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
                val soundEnabled by keyboardPrefs.soundEnabled.collectAsState()
                val hapticEnabled by keyboardPrefs.hapticEnabled.collectAsState()
                val themeType by keyboardPrefs.themeType.collectAsState()
                val laptopBarVisible by keyboardPrefs.laptopBarVisible.collectAsState()

                KeyProKeyboardView(
                    colors = KeyboardThemes.getTheme(themeType),
                    allSnippets = allSnippets,
                    isSoundEnabled = soundEnabled,
                    isHapticEnabled = hapticEnabled,
                    initialLaptopBarVisible = laptopBarVisible,
                    onAction = { action -> handleImeAction(action) },
                    onTogglePin = { snippet ->
                        serviceScope.launch { repository.togglePin(snippet.id, snippet.isPinned) }
                    },
                    onDeleteSnippet = { id ->
                        serviceScope.launch { repository.deleteSnippet(id) }
                    },
                    onSaveSnippet = { title, content, isPinned, category ->
                        serviceScope.launch { repository.insertSnippet(title, content, isPinned, category) }
                    },
                    onOpenSettings = {
                        try {
                            val intent = packageManager.getLaunchIntentForPackage(packageName)
                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            if (intent != null) startActivity(intent)
                        } catch (_: Exception) {}
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

    private fun handleImeAction(action: KeyAction) {
        val ic = currentInputConnection ?: return

        when (action) {
            is KeyAction.InsertText -> {
                ic.commitText(action.text, 1)
            }
            is KeyAction.Space -> {
                ic.commitText(" ", 1)
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
                ic.performContextMenuAction(android.R.id.paste)
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
            else -> {}
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        mViewModelStore.clear()
        super.onDestroy()
    }
}
