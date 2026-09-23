package com.example.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.AppDatabase
import com.example.data.SnippetRepository
import com.example.ui.keyboard.KeyProKeyboardView
import com.example.ui.keyboard.model.KeyAction
import com.example.ui.keyboard.model.KeyboardThemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KeyProInputMethodService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private lateinit var repository: SnippetRepository

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val db = AppDatabase.getDatabase(this)
        repository = SnippetRepository(db.snippetDao())
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@KeyProInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@KeyProInputMethodService)

            setContent {
                val pinnedSnippets by repository.pinnedSnippets.collectAsState(initial = emptyList())

                KeyProKeyboardView(
                    colors = KeyboardThemes.GboardDark,
                    pinnedSnippets = pinnedSnippets,
                    isSoundEnabled = true,
                    isHapticEnabled = true,
                    initialLaptopBarVisible = true,
                    onAction = { action -> handleImeAction(action) },
                    onOpenClipboardSheet = { /* Handled in in-app activity */ },
                    onOpenExtendedPcSheet = { /* Handled in in-app activity */ },
                    onOpenThemePicker = { /* Handled in settings */ },
                    onOpenSettings = { /* Handled in app */ }
                )
            }
        }
        return composeView
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
        super.onDestroy()
    }
}
