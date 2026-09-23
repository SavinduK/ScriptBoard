package com.example.ui.keyboard.model

object KeyboardLayoutGenerator {

    fun getLaptopKeyRows(isCtrlActive: Boolean, isAltActive: Boolean): List<List<KeyItem>> {
        val row1 = listOf(
            KeyItem("ESC", action = KeyAction.Escape, weight = 1.1f, isFunctional = true, testTag = "key_laptop_esc"),
            KeyItem("/", action = KeyAction.InsertText("/"), weight = 0.9f, testTag = "key_laptop_slash"),
            KeyItem("—", action = KeyAction.InsertText("—"), weight = 0.9f, testTag = "key_laptop_dash"),
            KeyItem("HOME", action = KeyAction.Home, weight = 1.25f, isFunctional = true, testTag = "key_laptop_home"),
            KeyItem("↑", action = KeyAction.CursorUp, weight = 0.95f, isFunctional = true, testTag = "key_laptop_up"),
            KeyItem("END", action = KeyAction.End, weight = 1.25f, isFunctional = true, testTag = "key_laptop_end"),
            KeyItem("PGUP", action = KeyAction.PageUp, weight = 1.25f, isFunctional = true, testTag = "key_laptop_pgup")
        )

        val row2 = listOf(
            KeyItem("↹", action = KeyAction.Tab, weight = 1.1f, isFunctional = true, testTag = "key_laptop_tab"),
            KeyItem("CTRL", action = KeyAction.ToggleCtrl, weight = 1.1f, isFunctional = true, isActiveModifier = isCtrlActive, testTag = "key_laptop_ctrl"),
            KeyItem("ALT", action = KeyAction.ToggleAlt, weight = 1.1f, isFunctional = true, isActiveModifier = isAltActive, testTag = "key_laptop_alt"),
            KeyItem("←", action = KeyAction.CursorLeft, weight = 1.0f, isFunctional = true, testTag = "key_laptop_left"),
            KeyItem("↓", action = KeyAction.CursorDown, weight = 0.95f, isFunctional = true, testTag = "key_laptop_down"),
            KeyItem("→", action = KeyAction.CursorRight, weight = 1.0f, isFunctional = true, testTag = "key_laptop_right"),
            KeyItem("PGDN", action = KeyAction.PageDown, weight = 1.25f, isFunctional = true, testTag = "key_laptop_pgdn")
        )

        return listOf(row1, row2)
    }

    fun getQwertyRows(shiftState: ShiftState): List<List<KeyItem>> {
        val isUpper = shiftState != ShiftState.OFF
        val row1Letters = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val row1Digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val row1 = row1Letters.mapIndexed { idx, char ->
            val text = if (isUpper) char.uppercase() else char
            KeyItem(
                primaryText = text,
                secondaryText = row1Digits[idx],
                action = KeyAction.InsertText(text),
                weight = 1f,
                testTag = "key_char_$char"
            )
        }

        val row2Letters = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val row2 = row2Letters.map { char ->
            val text = if (isUpper) char.uppercase() else char
            KeyItem(
                primaryText = text,
                action = KeyAction.InsertText(text),
                weight = 1f,
                testTag = "key_char_$char"
            )
        }

        val shiftLabel = when (shiftState) {
            ShiftState.OFF -> "⇧"
            ShiftState.ONCE -> "⇪"
            ShiftState.CAPS_LOCKED -> "▲"
        }
        val row3Letters = listOf("z", "x", "c", "v", "b", "n", "m")
        val row3Middle = row3Letters.map { char ->
            val text = if (isUpper) char.uppercase() else char
            KeyItem(
                primaryText = text,
                action = KeyAction.InsertText(text),
                weight = 1f,
                testTag = "key_char_$char"
            )
        }

        val shiftKey = KeyItem(
            primaryText = shiftLabel,
            action = KeyAction.ToggleShift,
            weight = 1.4f,
            isFunctional = true,
            isActiveModifier = shiftState != ShiftState.OFF,
            testTag = "key_shift"
        )
        val backspaceKey = KeyItem(
            primaryText = "⌫",
            action = KeyAction.Backspace,
            weight = 1.4f,
            isFunctional = true,
            testTag = "key_backspace"
        )
        val row3 = listOf(shiftKey) + row3Middle + listOf(backspaceKey)

        val row4 = listOf(
            KeyItem("?123", action = KeyAction.SwitchToSymbols, weight = 1.35f, isFunctional = true, testTag = "key_switch_symbols"),
            KeyItem(",", action = KeyAction.InsertText(","), weight = 1.0f, isFunctional = true, testTag = "key_comma"),
            KeyItem("☺", action = KeyAction.SwitchToEmoji, weight = 1.0f, isFunctional = true, testTag = "key_emoji"),
            KeyItem("English", action = KeyAction.Space, weight = 3.9f, testTag = "key_space"),
            KeyItem(".", action = KeyAction.InsertText("."), weight = 1.0f, isFunctional = true, testTag = "key_period"),
            KeyItem("↵", action = KeyAction.Enter, weight = 1.45f, isAccent = true, testTag = "key_enter")
        )

        return listOf(row1, row2, row3, row4)
    }

    // Matching screenshot row for ?123
    fun getSymbols1Rows(): List<List<KeyItem>> {
        val row1Chars = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val row1 = row1Chars.map {
            KeyItem(it, action = KeyAction.InsertText(it), weight = 1f, testTag = "key_num_$it")
        }

        val row2Chars = listOf("@", "#", "£", "_", "&", "-", "+", "(", ")", "/")
        val row2 = row2Chars.map {
            KeyItem(it, action = KeyAction.InsertText(it), weight = 1f, testTag = "key_sym_$it")
        }

        val row3Chars = listOf("*", "\"", "'", ":", ";", "!", "?")
        val row3Middle = row3Chars.map {
            KeyItem(it, action = KeyAction.InsertText(it), weight = 1f, testTag = "key_sym_$it")
        }
        val altSymbolsKey = KeyItem("=\\<", action = KeyAction.SwitchToMoreSymbols, weight = 1.4f, isFunctional = true, testTag = "key_more_symbols")
        val backspaceKey = KeyItem("⌫", action = KeyAction.Backspace, weight = 1.4f, isFunctional = true, testTag = "key_backspace")
        val row3 = listOf(altSymbolsKey) + row3Middle + listOf(backspaceKey)

        val row4 = listOf(
            KeyItem("ABC", action = KeyAction.SwitchToLetters, weight = 1.35f, isFunctional = true, testTag = "key_switch_abc"),
            KeyItem(",", action = KeyAction.InsertText(","), weight = 0.95f, isFunctional = true, testTag = "key_comma"),
            KeyItem("12\n34", action = KeyAction.SwitchToNumpad, weight = 1.0f, isFunctional = true, testTag = "key_switch_numpad"),
            KeyItem("English", action = KeyAction.Space, weight = 3.8f, testTag = "key_space"),
            KeyItem(".", action = KeyAction.InsertText("."), weight = 0.95f, isFunctional = true, testTag = "key_period"),
            KeyItem("↵", action = KeyAction.Enter, weight = 1.45f, isAccent = true, testTag = "key_enter")
        )

        return listOf(row1, row2, row3, row4)
    }

    fun getSymbols2Rows(): List<List<KeyItem>> {
        val row1Chars = listOf("~", "`", "|", "\\", "{", "}", "[", "]", "%", "^", "°")
        val row1 = row1Chars.map {
            KeyItem(it, action = KeyAction.InsertText(it), weight = 1f, testTag = "key_sym2_$it")
        }

        val row2Chars = listOf("$", "€", "¥", "¢", "©", "®", "™", "§", "<", ">")
        val row2 = row2Chars.map {
            KeyItem(it, action = KeyAction.InsertText(it), weight = 1f, testTag = "key_sym2_$it")
        }

        val row3Chars = listOf("_", "…", "¿", "¡", "«", "»", "×", "÷")
        val row3Middle = row3Chars.map {
            KeyItem(it, action = KeyAction.InsertText(it), weight = 1f, testTag = "key_sym2_$it")
        }
        val symbols1Key = KeyItem("?123", action = KeyAction.SwitchToSymbols, weight = 1.4f, isFunctional = true, testTag = "key_switch_sym1")
        val backspaceKey = KeyItem("⌫", action = KeyAction.Backspace, weight = 1.4f, isFunctional = true, testTag = "key_backspace")
        val row3 = listOf(symbols1Key) + row3Middle + listOf(backspaceKey)

        val row4 = listOf(
            KeyItem("ABC", action = KeyAction.SwitchToLetters, weight = 1.35f, isFunctional = true, testTag = "key_switch_abc"),
            KeyItem(",", action = KeyAction.InsertText(","), weight = 0.95f, isFunctional = true, testTag = "key_comma"),
            KeyItem("12\n34", action = KeyAction.SwitchToNumpad, weight = 1.0f, isFunctional = true, testTag = "key_switch_numpad"),
            KeyItem("English", action = KeyAction.Space, weight = 3.8f, testTag = "key_space"),
            KeyItem(".", action = KeyAction.InsertText("."), weight = 0.95f, isFunctional = true, testTag = "key_period"),
            KeyItem("↵", action = KeyAction.Enter, weight = 1.45f, isAccent = true, testTag = "key_enter")
        )

        return listOf(row1, row2, row3, row4)
    }

    fun getNumpadRows(): List<List<KeyItem>> {
        val row1 = listOf(
            KeyItem("7", action = KeyAction.InsertText("7"), weight = 1f, testTag = "np_7"),
            KeyItem("8", action = KeyAction.InsertText("8"), weight = 1f, testTag = "np_8"),
            KeyItem("9", action = KeyAction.InsertText("9"), weight = 1f, testTag = "np_9"),
            KeyItem("/", action = KeyAction.InsertText("/"), weight = 1f, isFunctional = true, testTag = "np_div"),
            KeyItem("⌫", action = KeyAction.Backspace, weight = 1f, isFunctional = true, testTag = "np_backspace")
        )
        val row2 = listOf(
            KeyItem("4", action = KeyAction.InsertText("4"), weight = 1f, testTag = "np_4"),
            KeyItem("5", action = KeyAction.InsertText("5"), weight = 1f, testTag = "np_5"),
            KeyItem("6", action = KeyAction.InsertText("6"), weight = 1f, testTag = "np_6"),
            KeyItem("*", action = KeyAction.InsertText("*"), weight = 1f, isFunctional = true, testTag = "np_mul"),
            KeyItem("DEL", action = KeyAction.DeleteForward, weight = 1f, isFunctional = true, testTag = "np_del")
        )
        val row3 = listOf(
            KeyItem("1", action = KeyAction.InsertText("1"), weight = 1f, testTag = "np_1"),
            KeyItem("2", action = KeyAction.InsertText("2"), weight = 1f, testTag = "np_2"),
            KeyItem("3", action = KeyAction.InsertText("3"), weight = 1f, testTag = "np_3"),
            KeyItem("-", action = KeyAction.InsertText("-"), weight = 1f, isFunctional = true, testTag = "np_minus"),
            KeyItem("(", action = KeyAction.InsertText("("), weight = 1f, isFunctional = true, testTag = "np_op")
        )
        val row4 = listOf(
            KeyItem("ABC", action = KeyAction.SwitchToLetters, weight = 1.2f, isFunctional = true, testTag = "np_abc"),
            KeyItem("0", action = KeyAction.InsertText("0"), weight = 1f, testTag = "np_0"),
            KeyItem(".", action = KeyAction.InsertText("."), weight = 1f, testTag = "np_dot"),
            KeyItem("+", action = KeyAction.InsertText("+"), weight = 1f, isFunctional = true, testTag = "np_plus"),
            KeyItem("↵", action = KeyAction.Enter, weight = 1.2f, isAccent = true, testTag = "np_enter")
        )
        return listOf(row1, row2, row3, row4)
    }

    fun getExtendedPcRows(): List<List<KeyItem>> {
        val row1FKeys = (1..12).map { num ->
            KeyItem("F$num", action = KeyAction.FunctionKey(num), weight = 1f, isFunctional = true, testTag = "key_f$num")
        }

        val row2 = listOf(
            KeyItem("ESC", action = KeyAction.Escape, weight = 1f, isFunctional = true, testTag = "pc_esc"),
            KeyItem("DEL", action = KeyAction.DeleteForward, weight = 1.1f, isFunctional = true, testTag = "pc_del"),
            KeyItem("INS", action = KeyAction.InsertText(""), weight = 0.9f, isFunctional = true, testTag = "pc_ins"),
            KeyItem("HOME", action = KeyAction.Home, weight = 1.15f, isFunctional = true, testTag = "pc_home"),
            KeyItem("END", action = KeyAction.End, weight = 1.15f, isFunctional = true, testTag = "pc_end"),
            KeyItem("PGUP", action = KeyAction.PageUp, weight = 1.15f, isFunctional = true, testTag = "pc_pgup"),
            KeyItem("PGDN", action = KeyAction.PageDown, weight = 1.15f, isFunctional = true, testTag = "pc_pgdn"),
            KeyItem("PRTSC", action = KeyAction.Copy, weight = 1.1f, isFunctional = true, testTag = "pc_prtsc")
        )

        val row3Chars = listOf("`", "~", "|", "\\", "^", "{", "}", "[", "]")
        val row3Middle = row3Chars.map {
            KeyItem(it, action = KeyAction.InsertText(it), weight = 1f, testTag = "pc_sym_$it")
        }
        val backspaceKey = KeyItem("⌫", action = KeyAction.Backspace, weight = 1.4f, isFunctional = true, testTag = "key_backspace")
        val tabKey = KeyItem("↹", action = KeyAction.Tab, weight = 1.1f, isFunctional = true, testTag = "pc_tab")
        val row3 = listOf(tabKey) + row3Middle + listOf(backspaceKey)

        val row4 = listOf(
            KeyItem("ABC", action = KeyAction.SwitchToLetters, weight = 1.35f, isFunctional = true, testTag = "key_switch_abc"),
            KeyItem("Ctrl+A", action = KeyAction.SelectAll, weight = 1.05f, isFunctional = true, testTag = "pc_ctrl_a"),
            KeyItem("Ctrl+C", action = KeyAction.Copy, weight = 1.05f, isFunctional = true, testTag = "pc_ctrl_c"),
            KeyItem("Ctrl+V", action = KeyAction.Paste, weight = 1.05f, isFunctional = true, testTag = "pc_ctrl_v"),
            KeyItem("Ctrl+Z", action = KeyAction.Undo, weight = 1.05f, isFunctional = true, testTag = "pc_ctrl_z"),
            KeyItem("English", action = KeyAction.Space, weight = 2.4f, testTag = "key_space"),
            KeyItem("↵", action = KeyAction.Enter, weight = 1.35f, isAccent = true, testTag = "key_enter")
        )

        return listOf(row1FKeys, row2, row3, row4)
    }
}
