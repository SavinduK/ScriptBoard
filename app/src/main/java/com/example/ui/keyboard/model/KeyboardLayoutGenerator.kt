package com.example.ui.keyboard.model

object ToggleableKeys {
    val pairs: Map<String, String> = mapOf(
        // Numbers <-> Symbols (Symbols 1 Row 1 <-> Symbols 2 Row 1)
        "1" to "~",
        "2" to "@",
        "3" to "#",
        "4" to "$",
        "5" to "%",
        "6" to "^",
        "7" to "&",
        "8" to "*",
        "9" to "(",
        "0" to ")",

        // Symbols 1 Row 2 & 3 <-> Symbols 2 Row 2 & 3
        "@" to "$",
        "#" to "€",
        "£" to "¥",
        "_" to "¢",
        "&" to "©",
        "-" to "®",
        "+" to "™",
        "(" to "<",
        ")" to ">",
        "/" to "÷",

        "*" to "×",
        "\"" to "«",
        "'" to "»",
        ":" to "…",
        ";" to "§",
        "!" to "¡",
        "?" to "¿",

        // Reverse toggleables (Symbols 2 <-> Symbols 1)
        "~" to "1",
        "$" to "@",
        "€" to "#",
        "¥" to "£",
        "¢" to "_",
        "©" to "&",
        "®" to "-",
        "™" to "+",
        "<" to "(",
        ">" to ")",
        "÷" to "/",

        "×" to "*",
        "«" to "\"",
        "»" to "'",
        "…" to ":",
        "§" to ";",
        "¡" to "!",
        "¿" to "?"
    )

    fun getToggleable(key: String): String? = pairs[key]
}

object KeyboardLayoutGenerator {

    fun isSinhalaConsonant(c: Char): Boolean {
        return c in '\u0D9A'..'\u0DC6'
    }

    fun isSinhalaConsonant(text: String): Boolean {
        if (text.isEmpty()) return false
        return isSinhalaConsonant(text.first()) || text == "ක්‍ෂ"
    }

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

    fun getDedicatedNumberRow(holdForSymbols: Boolean = true): List<KeyItem> {
        val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val alternates = listOf("~", "@", "#", "$", "%", "^", "&", "*", "(", ")")
        return digits.mapIndexed { idx, digit ->
            KeyItem(
                primaryText = digit,
                secondaryText = if (holdForSymbols) alternates[idx] else null,
                action = KeyAction.InsertText(digit),
                weight = 1f,
                testTag = "key_dedicated_num_$digit"
            )
        }
    }

    fun getQwertyRows(
        shiftState: ShiftState,
        holdForSymbols: Boolean = true,
        languageDisplayName: String = "English",
        includeNumberRow: Boolean = false,
        showLanguageSwitchKey: Boolean = false
    ): List<List<KeyItem>> {
        val isUpper = shiftState != ShiftState.OFF
        val row1Letters = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val row1Digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val row1 = row1Letters.mapIndexed { idx, char ->
            val text = if (isUpper) char.uppercase() else char
            KeyItem(
                primaryText = text,
                secondaryText = if (holdForSymbols) row1Digits[idx] else null,
                action = KeyAction.InsertText(text),
                weight = 1f,
                testTag = "key_char_$char"
            )
        }

        val row2Letters = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val row2Symbols = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")")
        val row2 = row2Letters.mapIndexed { idx, char ->
            val text = if (isUpper) char.uppercase() else char
            KeyItem(
                primaryText = text,
                secondaryText = if (holdForSymbols) row2Symbols[idx] else null,
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
        val row3Symbols = listOf("*", "\"", "'", ":", ";", "!", "?")
        val row3Middle = row3Letters.mapIndexed { idx, char ->
            val text = if (isUpper) char.uppercase() else char
            KeyItem(
                primaryText = text,
                secondaryText = if (holdForSymbols) row3Symbols[idx] else null,
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

        val row4Base = mutableListOf<KeyItem>()
        row4Base.add(KeyItem("?123", action = KeyAction.SwitchToSymbols, weight = 1.35f, isFunctional = true, testTag = "key_switch_symbols"))
        row4Base.add(KeyItem(",", action = KeyAction.InsertText(","), weight = 0.95f, isFunctional = true, testTag = "key_comma"))
        row4Base.add(KeyItem("☺", action = KeyAction.SwitchToEmoji, weight = 0.95f, isFunctional = true, testTag = "key_emoji"))
        if (showLanguageSwitchKey) {
            row4Base.add(KeyItem("🌐", action = KeyAction.SwitchLanguage, weight = 0.95f, isFunctional = true, testTag = "key_switch_lang"))
        }
        row4Base.add(KeyItem(languageDisplayName, action = KeyAction.Space, weight = if (showLanguageSwitchKey) 2.95f else 3.9f, testTag = "key_space"))
        row4Base.add(KeyItem(".", secondaryText = if (holdForSymbols) "/" else null, action = KeyAction.InsertText("."), weight = 0.95f, isFunctional = true, testTag = "key_period"))
        row4Base.add(KeyItem("↵", action = KeyAction.Enter, weight = 1.45f, isAccent = true, testTag = "key_enter"))

        val topRows = if (includeNumberRow) listOf(getDedicatedNumberRow(holdForSymbols)) else emptyList()
        return topRows + listOf(row1, row2, row3, row4Base)
    }

    // Matching Sinhala layout as given in screenshot (5 rows + 1 bottom row)
    // Dynamically updates vowel modifier keys (පිල්ලම්) based on the selected consonant
    fun getSinhalaRows(
        selectedLetter: String = "ව",
        showLanguageSwitchKey: Boolean = false
    ): List<List<KeyItem>> {
        val sel = selectedLetter.ifEmpty { "ව" }

        // Row 1 (from screenshot)
        val row1 = listOf(
            KeyItem("${sel}ු", action = KeyAction.InsertSinhalaPillam("${sel}ු", "ු"), testTag = "key_si_vu"),
            KeyItem("${sel}ඃ", action = KeyAction.InsertSinhalaPillam("${sel}ඃ", "ඃ"), testTag = "key_si_visarga"),
            KeyItem("${sel}ැ", action = KeyAction.InsertSinhalaPillam("${sel}ැ", "ැ"), testTag = "key_si_vae"),
            KeyItem("${sel}ො", action = KeyAction.InsertSinhalaPillam("${sel}ො", "ො"), testTag = "key_si_vo"),
            KeyItem("ර", action = KeyAction.InsertText("ර"), isActiveModifier = sel == "ර", testTag = "key_si_ra"),
            KeyItem("හ", action = KeyAction.InsertText("හ"), isActiveModifier = sel == "හ", testTag = "key_si_ha"),
            KeyItem("ම", action = KeyAction.InsertText("ම"), isActiveModifier = sel == "ම", testTag = "key_si_ma"),
            KeyItem("ස", action = KeyAction.InsertText("ස"), isActiveModifier = sel == "ස", testTag = "key_si_sa"),
            KeyItem("ද", action = KeyAction.InsertText("ද"), isActiveModifier = sel == "ද", testTag = "key_si_da"),
            KeyItem("ච", action = KeyAction.InsertText("ච"), isActiveModifier = sel == "ච", testTag = "key_si_cha")
        )

        // Row 2 (from screenshot)
        val row2 = listOf(
            KeyItem("${sel}ූ", action = KeyAction.InsertSinhalaPillam("${sel}ූ", "ූ"), testTag = "key_si_vuu"),
            KeyItem("${sel}ා", action = KeyAction.InsertSinhalaPillam("${sel}ා", "ා"), testTag = "key_si_vaa"),
            KeyItem("${sel}ෑ", action = KeyAction.InsertSinhalaPillam("${sel}ෑ", "ෑ"), testTag = "key_si_vaae"),
            KeyItem("${sel}ෙ", action = KeyAction.InsertSinhalaPillam("${sel}ෙ", "ෙ"), testTag = "key_si_ve"),
            KeyItem("ෆ", action = KeyAction.InsertText("ෆ"), isActiveModifier = sel == "ෆ", testTag = "key_si_fa"),
            KeyItem("ශ", action = KeyAction.InsertText("ශ"), isActiveModifier = sel == "ශ", testTag = "key_si_sha"),
            KeyItem("ඹ", action = KeyAction.InsertText("ඹ"), isActiveModifier = sel == "ඹ", testTag = "key_si_mba"),
            KeyItem("ෂ", action = KeyAction.InsertText("ෂ"), isActiveModifier = sel == "ෂ", testTag = "key_si_ssha"),
            KeyItem("ධ", action = KeyAction.InsertText("ධ"), isActiveModifier = sel == "ධ", testTag = "key_si_dha"),
            KeyItem("ක්‍ෂ", action = KeyAction.InsertText("ක්‍ෂ"), isActiveModifier = sel == "ක්‍ෂ", testTag = "key_si_ksha")
        )

        // Row 3 (from screenshot)
        val row3 = listOf(
            KeyItem("${sel}ි", action = KeyAction.InsertSinhalaPillam("${sel}ි", "ි"), testTag = "key_si_vi"),
            KeyItem("${sel}ී", action = KeyAction.InsertSinhalaPillam("${sel}ී", "ී"), testTag = "key_si_vii"),
            KeyItem("${sel}ෘ", action = KeyAction.InsertSinhalaPillam("${sel}ෘ", "ෘ"), testTag = "key_si_vru"),
            KeyItem("${sel}ෟ", action = KeyAction.InsertSinhalaPillam("${sel}ෟ", "ෟ"), testTag = "key_si_vlu"),
            KeyItem("ය", action = KeyAction.InsertText("ය"), isActiveModifier = sel == "ය", testTag = "key_si_ya"),
            KeyItem("ව", action = KeyAction.InsertText("ව"), isActiveModifier = sel == "ව", testTag = "key_si_va"),
            KeyItem("න", action = KeyAction.InsertText("න"), isActiveModifier = sel == "න", testTag = "key_si_na"),
            KeyItem("ක", action = KeyAction.InsertText("ක"), isActiveModifier = sel == "ක", testTag = "key_si_ka"),
            KeyItem("ත", action = KeyAction.InsertText("ත"), isActiveModifier = sel == "ත", testTag = "key_si_ta"),
            KeyItem("${sel}ං", action = KeyAction.InsertSinhalaPillam("${sel}ං", "ං"), testTag = "key_si_anusvara")
        )

        // Row 4 (from screenshot)
        val row4 = listOf(
            KeyItem("${sel}ං", action = KeyAction.InsertSinhalaPillam("${sel}ං", "ං"), testTag = "key_si_vam"),
            KeyItem("${sel}්", action = KeyAction.InsertSinhalaPillam("${sel}්", "්"), testTag = "key_si_hal"),
            KeyItem("${sel}ෛ", action = KeyAction.InsertSinhalaPillam("${sel}ෛ", "ෛ"), testTag = "key_si_vai"),
            KeyItem("${sel}්‍ය", action = KeyAction.InsertSinhalaPillam("${sel}්‍ය", "්‍ය"), testTag = "key_si_yansaya"),
            KeyItem("ට", action = KeyAction.InsertText("ට"), isActiveModifier = sel == "ට", testTag = "key_si_tta"),
            KeyItem("ළු", action = KeyAction.InsertText("ළු"), isActiveModifier = sel == "ළු" || sel == "ළ", testTag = "key_si_lu"),
            KeyItem("ණ", action = KeyAction.InsertText("ණ"), isActiveModifier = sel == "ණ", testTag = "key_si_nna"),
            KeyItem("බ", action = KeyAction.InsertText("බ"), isActiveModifier = sel == "බ", testTag = "key_si_ba"),
            KeyItem("ථ", action = KeyAction.InsertText("ථ"), isActiveModifier = sel == "ථ", testTag = "key_si_tha"),
            KeyItem("ග", action = KeyAction.InsertText("ග"), isActiveModifier = sel == "ග", testTag = "key_si_ga")
        )

        // Row 5 (from screenshot)
        val row5 = listOf(
            KeyItem("${sel}ෞ", action = KeyAction.InsertSinhalaPillam("${sel}ෞ", "ෞ"), testTag = "key_si_vau"),
            KeyItem("ජ", action = KeyAction.InsertText("ජ"), isActiveModifier = sel == "ජ", testTag = "key_si_ja"),
            KeyItem("ඩ", action = KeyAction.InsertText("ඩ"), isActiveModifier = sel == "ඩ", testTag = "key_si_dda"),
            KeyItem("ඪ", action = KeyAction.InsertText("ඪ"), isActiveModifier = sel == "ඪ", testTag = "key_si_ddha"),
            KeyItem("ඛ", action = KeyAction.InsertText("ඛ"), isActiveModifier = sel == "ඛ", testTag = "key_si_kha"),
            KeyItem("භ", action = KeyAction.InsertText("භ"), isActiveModifier = sel == "භ", testTag = "key_si_bha"),
            KeyItem("ප", action = KeyAction.InsertText("ප"), isActiveModifier = sel == "ප", testTag = "key_si_pa"),
            KeyItem("ල", action = KeyAction.InsertText("ල"), isActiveModifier = sel == "ල", testTag = "key_si_la"),
            KeyItem("ළ", action = KeyAction.InsertText("ළ"), isActiveModifier = sel == "ළ", testTag = "key_si_lla"),
            KeyItem("⌫", action = KeyAction.Backspace, weight = 1.0f, isFunctional = true, testTag = "key_backspace")
        )

        // Row 6: Bottom row (from screenshot: ?123, comma, emoji, [optional 🌐], spacebar with "සිංහල", period, enter)
        val row6 = mutableListOf<KeyItem>()
        row6.add(KeyItem("?123", action = KeyAction.SwitchToSymbols, weight = 1.35f, isFunctional = true, testTag = "key_switch_symbols"))
        row6.add(KeyItem(",", action = KeyAction.InsertText(","), weight = 0.95f, isFunctional = true, testTag = "key_comma"))
        row6.add(KeyItem("☺", action = KeyAction.SwitchToEmoji, weight = 0.95f, isFunctional = true, testTag = "key_emoji"))
        if (showLanguageSwitchKey) {
            row6.add(KeyItem("🌐", action = KeyAction.SwitchLanguage, weight = 0.95f, isFunctional = true, testTag = "key_switch_lang"))
        }
        row6.add(KeyItem("සිංහල", action = KeyAction.Space, weight = if (showLanguageSwitchKey) 2.95f else 3.8f, testTag = "key_space"))
        row6.add(KeyItem(".", action = KeyAction.InsertText("."), weight = 0.95f, isFunctional = true, testTag = "key_period"))
        row6.add(KeyItem("↵", action = KeyAction.Enter, weight = 1.45f, isAccent = true, testTag = "key_enter"))

        return listOf(row1, row2, row3, row4, row5, row6)
    }

    // Number key group and symbols with long-press toggleable keys:
    // 1 <-> ~, @ <-> $, ( <-> <, etc.
    fun getSymbols1Rows(
        languageDisplayName: String = "English",
        holdForSymbols: Boolean = true
    ): List<List<KeyItem>> {
        val row1Chars = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val row1 = row1Chars.map { num ->
            val toggle = if (holdForSymbols) ToggleableKeys.getToggleable(num) else null
            KeyItem(
                primaryText = num,
                secondaryText = toggle,
                action = KeyAction.InsertText(num),
                weight = 1f,
                testTag = "key_num_$num"
            )
        }

        val row2Chars = listOf("@", "#", "£", "_", "&", "-", "+", "(", ")", "/")
        val row2 = row2Chars.map { sym ->
            val toggle = if (holdForSymbols) ToggleableKeys.getToggleable(sym) else null
            KeyItem(
                primaryText = sym,
                secondaryText = toggle,
                action = KeyAction.InsertText(sym),
                weight = 1f,
                testTag = "key_sym_$sym"
            )
        }

        val row3Chars = listOf("*", "\"", "'", ":", ";", "!", "?")
        val row3Middle = row3Chars.map { sym ->
            val toggle = if (holdForSymbols) ToggleableKeys.getToggleable(sym) else null
            KeyItem(
                primaryText = sym,
                secondaryText = toggle,
                action = KeyAction.InsertText(sym),
                weight = 1f,
                testTag = "key_sym_$sym"
            )
        }
        val altSymbolsKey = KeyItem("=\\<", action = KeyAction.SwitchToMoreSymbols, weight = 1.4f, isFunctional = true, testTag = "key_more_symbols")
        val backspaceKey = KeyItem("⌫", action = KeyAction.Backspace, weight = 1.4f, isFunctional = true, testTag = "key_backspace")
        val row3 = listOf(altSymbolsKey) + row3Middle + listOf(backspaceKey)

        val row4 = listOf(
            KeyItem("ABC", action = KeyAction.SwitchToLetters, weight = 1.35f, isFunctional = true, testTag = "key_switch_abc"),
            KeyItem(",", action = KeyAction.InsertText(","), weight = 0.95f, isFunctional = true, testTag = "key_comma"),
            KeyItem("12\n34", action = KeyAction.SwitchToNumpad, weight = 1.0f, isFunctional = true, testTag = "key_switch_numpad"),
            KeyItem(languageDisplayName, action = KeyAction.Space, weight = 3.8f, testTag = "key_space"),
            KeyItem(".", action = KeyAction.InsertText("."), weight = 0.95f, isFunctional = true, testTag = "key_period"),
            KeyItem("↵", action = KeyAction.Enter, weight = 1.45f, isAccent = true, testTag = "key_enter")
        )

        return listOf(row1, row2, row3, row4)
    }

    fun getSymbols2Rows(
        languageDisplayName: String = "English",
        holdForSymbols: Boolean = true
    ): List<List<KeyItem>> {
        val row1Chars = listOf("~", "`", "|", "\\", "{", "}", "[", "]", "%", "^", "°")
        val row1 = row1Chars.map { sym ->
            val toggle = if (holdForSymbols) ToggleableKeys.getToggleable(sym) else null
            KeyItem(
                primaryText = sym,
                secondaryText = toggle,
                action = KeyAction.InsertText(sym),
                weight = 1f,
                testTag = "key_sym2_$sym"
            )
        }

        val row2Chars = listOf("$", "€", "¥", "¢", "©", "®", "™", "§", "<", ">")
        val row2 = row2Chars.map { sym ->
            val toggle = if (holdForSymbols) ToggleableKeys.getToggleable(sym) else null
            KeyItem(
                primaryText = sym,
                secondaryText = toggle,
                action = KeyAction.InsertText(sym),
                weight = 1f,
                testTag = "key_sym2_$sym"
            )
        }

        val row3Chars = listOf("×", "«", "»", "…", "÷", "¡", "¿")
        val row3Middle = row3Chars.map { sym ->
            val toggle = if (holdForSymbols) ToggleableKeys.getToggleable(sym) else null
            KeyItem(
                primaryText = sym,
                secondaryText = toggle,
                action = KeyAction.InsertText(sym),
                weight = 1f,
                testTag = "key_sym2_$sym"
            )
        }
        val symbols1Key = KeyItem("?123", action = KeyAction.SwitchToSymbols, weight = 1.4f, isFunctional = true, testTag = "key_switch_sym1")
        val backspaceKey = KeyItem("⌫", action = KeyAction.Backspace, weight = 1.4f, isFunctional = true, testTag = "key_backspace")
        val row3 = listOf(symbols1Key) + row3Middle + listOf(backspaceKey)

        val row4 = listOf(
            KeyItem("ABC", action = KeyAction.SwitchToLetters, weight = 1.35f, isFunctional = true, testTag = "key_switch_abc"),
            KeyItem(",", action = KeyAction.InsertText(","), weight = 0.95f, isFunctional = true, testTag = "key_comma"),
            KeyItem("12\n34", action = KeyAction.SwitchToNumpad, weight = 1.0f, isFunctional = true, testTag = "key_switch_numpad"),
            KeyItem(languageDisplayName, action = KeyAction.Space, weight = 3.8f, testTag = "key_space"),
            KeyItem(".", action = KeyAction.InsertText("."), weight = 0.95f, isFunctional = true, testTag = "key_period"),
            KeyItem("↵", action = KeyAction.Enter, weight = 1.45f, isAccent = true, testTag = "key_enter")
        )

        return listOf(row1, row2, row3, row4)
    }

    fun getNumpadRows(holdForSymbols: Boolean = true): List<List<KeyItem>> {
        val row1 = listOf(
            KeyItem("7", secondaryText = if (holdForSymbols) "&" else null, action = KeyAction.InsertText("7"), weight = 1f, testTag = "np_7"),
            KeyItem("8", secondaryText = if (holdForSymbols) "*" else null, action = KeyAction.InsertText("8"), weight = 1f, testTag = "np_8"),
            KeyItem("9", secondaryText = if (holdForSymbols) "(" else null, action = KeyAction.InsertText("9"), weight = 1f, testTag = "np_9"),
            KeyItem("/", action = KeyAction.InsertText("/"), weight = 1f, isFunctional = true, testTag = "np_div"),
            KeyItem("⌫", action = KeyAction.Backspace, weight = 1f, isFunctional = true, testTag = "np_backspace")
        )
        val row2 = listOf(
            KeyItem("4", secondaryText = if (holdForSymbols) "$" else null, action = KeyAction.InsertText("4"), weight = 1f, testTag = "np_4"),
            KeyItem("5", secondaryText = if (holdForSymbols) "%" else null, action = KeyAction.InsertText("5"), weight = 1f, testTag = "np_5"),
            KeyItem("6", secondaryText = if (holdForSymbols) "^" else null, action = KeyAction.InsertText("6"), weight = 1f, testTag = "np_6"),
            KeyItem("*", action = KeyAction.InsertText("*"), weight = 1f, isFunctional = true, testTag = "np_mul"),
            KeyItem("DEL", action = KeyAction.DeleteForward, weight = 1f, isFunctional = true, testTag = "np_del")
        )
        val row3 = listOf(
            KeyItem("1", secondaryText = if (holdForSymbols) "~" else null, action = KeyAction.InsertText("1"), weight = 1f, testTag = "np_1"),
            KeyItem("2", secondaryText = if (holdForSymbols) "@" else null, action = KeyAction.InsertText("2"), weight = 1f, testTag = "np_2"),
            KeyItem("3", secondaryText = if (holdForSymbols) "#" else null, action = KeyAction.InsertText("3"), weight = 1f, testTag = "np_3"),
            KeyItem("-", action = KeyAction.InsertText("-"), weight = 1f, isFunctional = true, testTag = "np_minus"),
            KeyItem("(", action = KeyAction.InsertText("("), weight = 1f, isFunctional = true, testTag = "np_op")
        )
        val row4 = listOf(
            KeyItem("ABC", action = KeyAction.SwitchToLetters, weight = 1.2f, isFunctional = true, testTag = "np_abc"),
            KeyItem("0", secondaryText = if (holdForSymbols) ")" else null, action = KeyAction.InsertText("0"), weight = 1f, testTag = "np_0"),
            KeyItem(".", action = KeyAction.InsertText("."), weight = 1f, testTag = "np_dot"),
            KeyItem("+", action = KeyAction.InsertText("+"), weight = 1f, isFunctional = true, testTag = "np_plus"),
            KeyItem("↵", action = KeyAction.Enter, weight = 1.2f, isAccent = true, testTag = "np_enter")
        )
        return listOf(row1, row2, row3, row4)
    }

    fun getExtendedPcRows(languageDisplayName: String = "English"): List<List<KeyItem>> {
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
            KeyItem(languageDisplayName, action = KeyAction.Space, weight = 2.4f, testTag = "key_space"),
            KeyItem("↵", action = KeyAction.Enter, weight = 1.35f, isAccent = true, testTag = "key_enter")
        )

        return listOf(row1FKeys, row2, row3, row4)
    }
}
