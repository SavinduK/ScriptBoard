package com.example.ui.keyboard.model

enum class KeyboardLayoutMode {
    TEXT,
    SYMBOLS_1,     // ?123
    SYMBOLS_2,     // =\<
    NUMPAD,        // 1234
    EXTENDED_PC,   // Laptop / PC special keys in-place
    EMOJI,
    CLIPBOARD,
    SETTINGS,
    STICKERS
}

enum class ShiftState {
    OFF,
    ONCE,
    CAPS_LOCKED
}

sealed class KeyAction {
    data class InsertText(val text: String) : KeyAction()
    data class InsertSticker(val filePath: String, val name: String = "Sticker") : KeyAction()
    object Backspace : KeyAction()
    object DeleteForward : KeyAction()
    object Enter : KeyAction()
    object ToggleShift : KeyAction()
    object SwitchToSymbols : KeyAction()
    object SwitchToLetters : KeyAction()
    object SwitchToMoreSymbols : KeyAction()
    object SwitchToNumpad : KeyAction()
    object SwitchToExtendedPc : KeyAction()
    object SwitchToEmoji : KeyAction()
    object SwitchToClipboard : KeyAction()
    object Space : KeyAction()
    
    // Laptop specific keys
    object Escape : KeyAction()
    object Tab : KeyAction()
    object ToggleCtrl : KeyAction()
    object ToggleAlt : KeyAction()
    object CursorLeft : KeyAction()
    object CursorRight : KeyAction()
    object CursorUp : KeyAction()
    object CursorDown : KeyAction()
    object Home : KeyAction()
    object End : KeyAction()
    object PageUp : KeyAction()
    object PageDown : KeyAction()
    object SelectAll : KeyAction()
    object Copy : KeyAction()
    object Cut : KeyAction()
    object Paste : KeyAction()
    object Undo : KeyAction()
    object Redo : KeyAction()
    data class FunctionKey(val fNumber: Int) : KeyAction()
    data class ExpandShortcut(val shortcutText: String, val fullContent: String) : KeyAction()
    data class ApplySuggestion(val typedWord: String, val suggestedWord: String) : KeyAction()
}

data class KeyItem(
    val primaryText: String,
    val secondaryText: String? = null,
    val action: KeyAction,
    val weight: Float = 1f,
    val isFunctional: Boolean = false,
    val isAccent: Boolean = false,
    val isActiveModifier: Boolean = false,
    val testTag: String
)
