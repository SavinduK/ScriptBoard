package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SnippetEntity
import com.example.data.SnippetRepository
import com.example.ui.keyboard.model.KeyAction
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.model.KeyboardThemeType
import com.example.ui.keyboard.model.KeyboardThemes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SnippetRepository
    private val clipboardManager = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    init {
        val db = AppDatabase.getDatabase(application)
        repository = SnippetRepository(db.snippetDao())
    }

    val allSnippets: StateFlow<List<SnippetEntity>> = repository.allSnippets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedSnippets: StateFlow<List<SnippetEntity>> = repository.pinnedSnippets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editorValue = MutableStateFlow(
        TextFieldValue(
            text = "Welcome to KeyPro Keyboard! 🚀\n" +
                    "Try the Gboard standard layout below.\n" +
                    "Tap [PC Keys] to access ESC, TAB, CTRL, ALT, and Arrow keys.",
            selection = TextRange(0)
        )
    )
    val editorValue: StateFlow<TextFieldValue> = _editorValue.asStateFlow()

    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()

    private val _themeType = MutableStateFlow(KeyboardThemeType.GBOARD_DARK)
    val themeType: StateFlow<KeyboardThemeType> = _themeType.asStateFlow()

    val currentColors: KeyboardColors
        get() = KeyboardThemes.getTheme(_themeType.value)

    private val _isLaptopBarVisible = MutableStateFlow(true)
    val isLaptopBarVisible: StateFlow<Boolean> = _isLaptopBarVisible.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    private val _isHapticEnabled = MutableStateFlow(true)
    val isHapticEnabled: StateFlow<Boolean> = _isHapticEnabled.asStateFlow()

    private val _lastActionStatus = MutableStateFlow<String?>("Ready • Tap any key to test")
    val lastActionStatus: StateFlow<String?> = _lastActionStatus.asStateFlow()

    fun updateEditorValue(value: TextFieldValue) {
        _editorValue.value = value
    }

    fun setTheme(type: KeyboardThemeType) {
        _themeType.value = type
        _lastActionStatus.value = "Theme changed to ${type.displayName}"
    }

    fun toggleLaptopBar(visible: Boolean? = null) {
        _isLaptopBarVisible.value = visible ?: !_isLaptopBarVisible.value
        _lastActionStatus.value = if (_isLaptopBarVisible.value) "Laptop Keys Bar: Visible" else "Laptop Keys Bar: Hidden"
    }

    fun toggleSound(enabled: Boolean) {
        _isSoundEnabled.value = enabled
    }

    fun toggleHaptic(enabled: Boolean) {
        _isHapticEnabled.value = enabled
    }

    fun clearEditor() {
        saveUndoState()
        _editorValue.value = TextFieldValue("", TextRange.Zero)
        _lastActionStatus.value = "Document cleared"
    }

    private fun saveUndoState() {
        if (undoStack.size > 50) {
            undoStack.removeAt(0)
        }
        undoStack.add(_editorValue.value)
        redoStack.clear()
    }

    fun handleKeyboardAction(action: KeyAction) {
        when (action) {
            is KeyAction.InsertText -> insertText(action.text)
            is KeyAction.Space -> insertText(" ")
            is KeyAction.Backspace -> backspace()
            is KeyAction.DeleteForward -> deleteForward()
            is KeyAction.Enter -> insertText("\n")
            is KeyAction.Tab -> insertTab()
            is KeyAction.Escape -> {
                // Laptop ESC: clear selection or unfocus
                val current = _editorValue.value
                _editorValue.value = current.copy(selection = TextRange(current.selection.end))
                _lastActionStatus.value = "ESC pressed (selection cleared)"
            }
            is KeyAction.CursorLeft -> moveCursorLeft()
            is KeyAction.CursorRight -> moveCursorRight()
            is KeyAction.CursorUp -> moveCursorUp()
            is KeyAction.CursorDown -> moveCursorDown()
            is KeyAction.Home -> moveCursorHome()
            is KeyAction.End -> moveCursorEnd()
            is KeyAction.PageUp -> moveCursorPageUp()
            is KeyAction.PageDown -> moveCursorPageDown()
            is KeyAction.SelectAll -> selectAll()
            is KeyAction.Copy -> copyText()
            is KeyAction.Cut -> cutText()
            is KeyAction.Paste -> pasteText()
            is KeyAction.Undo -> undo()
            is KeyAction.Redo -> redo()
            is KeyAction.FunctionKey -> {
                _lastActionStatus.value = "F${action.fNumber} executed"
            }
            else -> {}
        }
    }

    private fun insertText(text: String) {
        saveUndoState()
        val current = _editorValue.value
        val originalText = current.text
        val selStart = current.selection.min
        val selEnd = current.selection.max

        val newText = originalText.substring(0, selStart) + text + originalText.substring(selEnd)
        val newCursor = selStart + text.length
        _editorValue.value = TextFieldValue(newText, TextRange(newCursor))
        _lastActionStatus.value = "Typed: \"$text\""
    }

    private fun insertTab() {
        saveUndoState()
        val current = _editorValue.value
        val tabSpaces = "    " // 4 spaces standard laptop tab indentation
        val originalText = current.text
        val selStart = current.selection.min
        val selEnd = current.selection.max

        val newText = originalText.substring(0, selStart) + tabSpaces + originalText.substring(selEnd)
        val newCursor = selStart + tabSpaces.length
        _editorValue.value = TextFieldValue(newText, TextRange(newCursor))
        _lastActionStatus.value = "TAB (↹) 4-space indent inserted"
    }

    private fun backspace() {
        val current = _editorValue.value
        val originalText = current.text
        val selStart = current.selection.min
        val selEnd = current.selection.max

        if (selStart != selEnd) {
            saveUndoState()
            val newText = originalText.substring(0, selStart) + originalText.substring(selEnd)
            _editorValue.value = TextFieldValue(newText, TextRange(selStart))
        } else if (selStart > 0) {
            saveUndoState()
            val newText = originalText.substring(0, selStart - 1) + originalText.substring(selStart)
            _editorValue.value = TextFieldValue(newText, TextRange(selStart - 1))
        }
    }

    private fun deleteForward() {
        val current = _editorValue.value
        val originalText = current.text
        val selStart = current.selection.min
        val selEnd = current.selection.max

        if (selStart != selEnd) {
            saveUndoState()
            val newText = originalText.substring(0, selStart) + originalText.substring(selEnd)
            _editorValue.value = TextFieldValue(newText, TextRange(selStart))
        } else if (selStart < originalText.length) {
            saveUndoState()
            val newText = originalText.substring(0, selStart) + originalText.substring(selStart + 1)
            _editorValue.value = TextFieldValue(newText, TextRange(selStart))
            _lastActionStatus.value = "DEL (Forward delete)"
        }
    }

    private fun moveCursorLeft() {
        val current = _editorValue.value
        val newPos = (current.selection.min - 1).coerceAtLeast(0)
        _editorValue.value = current.copy(selection = TextRange(newPos))
        _lastActionStatus.value = "Cursor Left ← (Index $newPos)"
    }

    private fun moveCursorRight() {
        val current = _editorValue.value
        val newPos = (current.selection.max + 1).coerceAtMost(current.text.length)
        _editorValue.value = current.copy(selection = TextRange(newPos))
        _lastActionStatus.value = "Cursor Right → (Index $newPos)"
    }

    private fun moveCursorUp() {
        val current = _editorValue.value
        val lines = current.text.split("\n")
        var charCount = 0
        var currentLineIndex = 0
        var colInLine = 0

        for (i in lines.indices) {
            val lineLen = lines[i].length + 1 // including \n
            if (current.selection.start in charCount until (charCount + lineLen)) {
                currentLineIndex = i
                colInLine = current.selection.start - charCount
                break
            }
            charCount += lineLen
        }

        if (currentLineIndex > 0) {
            val prevLineLen = lines[currentLineIndex - 1].length
            val targetCol = colInLine.coerceAtMost(prevLineLen)
            var targetCharIndex = 0
            for (i in 0 until currentLineIndex - 1) {
                targetCharIndex += lines[i].length + 1
            }
            targetCharIndex += targetCol
            _editorValue.value = current.copy(selection = TextRange(targetCharIndex))
            _lastActionStatus.value = "Cursor Up ↑"
        }
    }

    private fun moveCursorDown() {
        val current = _editorValue.value
        val lines = current.text.split("\n")
        var charCount = 0
        var currentLineIndex = 0
        var colInLine = 0

        for (i in lines.indices) {
            val lineLen = lines[i].length + 1
            if (current.selection.start in charCount until (charCount + lineLen)) {
                currentLineIndex = i
                colInLine = current.selection.start - charCount
                break
            }
            charCount += lineLen
        }

        if (currentLineIndex < lines.size - 1) {
            val nextLineLen = lines[currentLineIndex + 1].length
            val targetCol = colInLine.coerceAtMost(nextLineLen)
            var targetCharIndex = 0
            for (i in 0..currentLineIndex) {
                targetCharIndex += lines[i].length + 1
            }
            targetCharIndex += targetCol
            _editorValue.value = current.copy(selection = TextRange(targetCharIndex))
            _lastActionStatus.value = "Cursor Down ↓"
        }
    }

    private fun moveCursorHome() {
        val current = _editorValue.value
        val text = current.text
        val pos = current.selection.start
        val lineStart = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0))
        val newPos = if (lineStart == -1) 0 else lineStart + 1
        _editorValue.value = current.copy(selection = TextRange(newPos))
        _lastActionStatus.value = "HOME: Moved to line start"
    }

    private fun moveCursorEnd() {
        val current = _editorValue.value
        val text = current.text
        val pos = current.selection.start
        val lineEnd = text.indexOf('\n', pos)
        val newPos = if (lineEnd == -1) text.length else lineEnd
        _editorValue.value = current.copy(selection = TextRange(newPos))
        _lastActionStatus.value = "END: Moved to line end"
    }

    private fun moveCursorPageUp() {
        repeat(5) { moveCursorUp() }
        _lastActionStatus.value = "PGUP: Jumped up"
    }

    private fun moveCursorPageDown() {
        repeat(5) { moveCursorDown() }
        _lastActionStatus.value = "PGDN: Jumped down"
    }

    private fun selectAll() {
        val current = _editorValue.value
        _editorValue.value = current.copy(selection = TextRange(0, current.text.length))
        _lastActionStatus.value = "CTRL+A: All selected (${current.text.length} chars)"
    }

    private fun copyText() {
        val current = _editorValue.value
        val selectedText = if (current.selection.min != current.selection.max) {
            current.text.substring(current.selection.min, current.selection.max)
        } else {
            current.text
        }
        if (selectedText.isNotEmpty()) {
            val clip = ClipData.newPlainText("KeyPro Text", selectedText)
            clipboardManager.setPrimaryClip(clip)
            viewModelScope.launch {
                repository.saveToClipboardHistory(selectedText)
            }
            _lastActionStatus.value = "Copied to clipboard: \"${selectedText.take(20)}\""
        }
    }

    private fun cutText() {
        val current = _editorValue.value
        if (current.selection.min != current.selection.max) {
            saveUndoState()
            val selectedText = current.text.substring(current.selection.min, current.selection.max)
            val clip = ClipData.newPlainText("KeyPro Text", selectedText)
            clipboardManager.setPrimaryClip(clip)
            viewModelScope.launch {
                repository.saveToClipboardHistory(selectedText)
            }
            val newText = current.text.substring(0, current.selection.min) + current.text.substring(current.selection.max)
            _editorValue.value = TextFieldValue(newText, TextRange(current.selection.min))
            _lastActionStatus.value = "Cut to clipboard"
        }
    }

    private fun pasteText() {
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            if (text.isNotEmpty()) {
                insertText(text)
                _lastActionStatus.value = "Pasted: \"${text.take(20)}\""
            }
        }
    }

    private fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_editorValue.value)
            _editorValue.value = previous
            _lastActionStatus.value = "Undo (CTRL+Z)"
        } else {
            _lastActionStatus.value = "Nothing to undo"
        }
    }

    private fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_editorValue.value)
            _editorValue.value = next
            _lastActionStatus.value = "Redo (CTRL+Y)"
        } else {
            _lastActionStatus.value = "Nothing to redo"
        }
    }

    fun togglePin(snippet: SnippetEntity) {
        viewModelScope.launch {
            repository.togglePin(snippet.id, snippet.isPinned)
            _lastActionStatus.value = if (!snippet.isPinned) "Pinned: ${snippet.title}" else "Unpinned: ${snippet.title}"
        }
    }

    fun saveSnippet(title: String, content: String, isPinned: Boolean, category: String) {
        viewModelScope.launch {
            repository.insertSnippet(title, content, isPinned, category)
            _lastActionStatus.value = "Snippet saved: $title"
        }
    }

    fun deleteSnippet(id: Long) {
        viewModelScope.launch {
            repository.deleteSnippet(id)
            _lastActionStatus.value = "Snippet deleted"
        }
    }

    fun clearUnpinnedHistory() {
        viewModelScope.launch {
            repository.clearUnpinned()
            _lastActionStatus.value = "Cleared clipboard history"
        }
    }
}
