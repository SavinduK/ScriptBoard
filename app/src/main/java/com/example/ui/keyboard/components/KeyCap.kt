package com.example.ui.keyboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.ui.keyboard.model.KeyAction
import com.example.ui.keyboard.model.KeyItem
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.model.ToggleableKeys
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object NumberKeyAlternates {
    val map: Map<String, List<String>> = mapOf(
        "1" to listOf("~", "¹", "½", "⅓", "¼", "₁"),
        "2" to listOf("@", "²", "⅔", "₂"),
        "3" to listOf("#", "³", "¾", "⅜", "₃"),
        "4" to listOf("$", "⁴", "₹", "€", "£", "¥", "¢", "₄"),
        "5" to listOf("%", "⁵", "‰", "₅"),
        "6" to listOf("^", "⁶", "₆"),
        "7" to listOf("&", "⁷", "₇"),
        "8" to listOf("*", "⁸", "°", "₈"),
        "9" to listOf("(", "⁹", "[", "{", "₉"),
        "0" to listOf(")", "⁰", "]", "}", "ø", "₀")
    )
}

object KeyAlternatesProvider {
    val map: Map<String, List<String>> = mapOf(
        // Number keys (1 -> ~, etc.)
        "1" to listOf("~", "¹", "½", "⅓", "¼", "₁"),
        "2" to listOf("@", "²", "⅔", "₂"),
        "3" to listOf("#", "³", "¾", "⅜", "₃"),
        "4" to listOf("$", "⁴", "₹", "€", "£", "¥", "¢", "₄"),
        "5" to listOf("%", "⁵", "‰", "₅"),
        "6" to listOf("^", "⁶", "₆"),
        "7" to listOf("&", "⁷", "₇"),
        "8" to listOf("*", "⁸", "°", "₈"),
        "9" to listOf("(", "⁹", "[", "{", "₉"),
        "0" to listOf(")", "⁰", "]", "}", "ø", "₀"),

        // Symbol keys (@ -> $, ( -> <, ) -> >, etc.)
        "@" to listOf("$", "€", "£", "¥", "¢"),
        "#" to listOf("€", "№"),
        "£" to listOf("¥", "€", "$"),
        "_" to listOf("¢", "–", "—"),
        "&" to listOf("©", "§"),
        "-" to listOf("®", "—", "–", "_"),
        "+" to listOf("™", "±", "†"),
        "(" to listOf("<", "[", "{", "«"),
        ")" to listOf(">", "]", "}", "»"),
        "/" to listOf("÷", "\\", "⁄"),
        "*" to listOf("×", "•", "°", "★"),
        "\"" to listOf("«", "“", "”", "„"),
        "'" to listOf("»", "‘", "’", "`"),
        ":" to listOf("…", "‥"),
        ";" to listOf("§", "‡"),
        "!" to listOf("¡", "‼"),
        "?" to listOf("¿", "‽"),

        // Symbols 2 (reverse toggleables: ~ -> 1, $ -> @, < -> (, etc.)
        "~" to listOf("1", "≈", "≃"),
        "$" to listOf("@", "€", "£", "¥", "₹", "¢"),
        "<" to listOf("(", "≤", "«", "‹"),
        ">" to listOf(")", "≥", "»", "›"),
        "×" to listOf("*", "•"),
        "÷" to listOf("/", "\\"),
        "«" to listOf("\"", "“"),
        "»" to listOf("'", "‘"),
        "…" to listOf(":", "‥"),
        "§" to listOf(";", "‡"),
        "¡" to listOf("!", "‼"),
        "¿" to listOf("?", "‽"),
        "€" to listOf("#", "$", "£"),
        "¥" to listOf("£", "$", "€"),
        "¢" to listOf("_", "$"),
        "©" to listOf("&"),
        "®" to listOf("-"),
        "™" to listOf("+")
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.KeyCap(
    key: KeyItem,
    colors: KeyboardColors,
    heightDp: Int = 46,
    characterFontSize: Float = 24f,
    holdForSymbols: Boolean = true,
    onKeyClick: (KeyItem) -> Unit,
    onKeyLongClick: ((KeyItem) -> Unit)? = null
) {
    val targetBgColor = when {
        key.isAccent -> colors.enterKeyBackground
        key.isActiveModifier -> colors.laptopKeyActiveBackground
        key.isFunctional -> colors.functionKeyBackground
        else -> colors.letterKeyBackground
    }

    val animatedBg by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 120),
        label = "key_bg_color"
    )

    val textColor = when {
        key.isAccent -> colors.enterKeyTextColor
        key.isActiveModifier -> colors.laptopKeyActiveTextColor
        key.isFunctional -> colors.functionKeyTextColor
        else -> colors.letterKeyTextColor
    }

    val coroutineScope = rememberCoroutineScope()
    val isSmallFont = key.primaryText.length > 2 || key.primaryText == "English" || key.primaryText == "සිංහල"
    val isMono = key.primaryText in listOf("ESC", "CTRL", "ALT", "HOME", "END", "PGUP", "PGDN", "↹")
    val isDeleteKey = key.action is KeyAction.Backspace || key.action is KeyAction.DeleteForward

    var showAlternatesPopup by remember { mutableStateOf(false) }

    val hasLongClickAction = holdForSymbols || onKeyLongClick != null
    val clickModifier = if (isDeleteKey) {
        Modifier.pointerInput(key) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                onKeyClick(key)

                val repeatJob = coroutineScope.launch {
                    delay(350)
                    while (isActive) {
                        onKeyClick(key)
                        delay(50)
                    }
                }

                waitForUpOrCancellation()
                repeatJob.cancel()
            }
        }
    } else {
        Modifier.combinedClickable(
            onClick = { onKeyClick(key) },
            onLongClick = if (hasLongClickAction) {
                {
                    var handledBySymbols = false
                    if (holdForSymbols) {
                        val alternates = KeyAlternatesProvider.map[key.primaryText] ?: NumberKeyAlternates.map[key.primaryText]
                        val toggleable = alternates?.firstOrNull() ?: ToggleableKeys.getToggleable(key.primaryText) ?: key.secondaryText
                        if (toggleable != null) {
                            // For long hold of number keys and symbols: replace with their corresponding toggleable key (1/~, @/$, (/<)
                            onKeyClick(key.copy(action = KeyAction.InsertText(toggleable)))
                            if (alternates != null && alternates.size > 1) {
                                showAlternatesPopup = true
                                coroutineScope.launch {
                                    delay(2500)
                                    showAlternatesPopup = false
                                }
                            }
                            handledBySymbols = true
                        }
                    }
                    if (!handledBySymbols) {
                        onKeyLongClick?.invoke(key)
                    }
                }
            } else null
        )
    }

    Box(
        modifier = Modifier
            .weight(key.weight)
            .height(heightDp.dp)
            .padding(horizontal = 2.5.dp, vertical = 3.dp)
            .shadow(1.dp, RoundedCornerShape(8.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(8.dp))
            .background(animatedBg)
            .then(clickModifier)
            .testTag(key.testTag),
        contentAlignment = Alignment.Center
    ) {
        // Optional secondary superscript symbol/digit
        if (key.secondaryText != null) {
            Text(
                text = key.secondaryText,
                color = colors.letterKeySecondaryTextColor,
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp, top = 2.dp)
            )
        }

        // Primary text
        val isFunctionKey = key.action is KeyAction.FunctionKey || key.primaryText.matches(Regex("F\\d+"))
        val isSinhalaChar = key.primaryText.any { it in '\u0D80'..'\u0DFF' }

        Text(
            text = key.primaryText,
            color = textColor,
            fontSize = when {
                isFunctionKey -> 11.5.sp // Matches F12 label size for all function keys
                key.primaryText == "English" || key.primaryText == "සිංහල" -> 13.sp
                isSinhalaChar -> if (key.primaryText.length > 2) 15.sp else 17.5.sp
                key.primaryText.length == 1 && key.primaryText[0].isLetter() -> characterFontSize.sp
                key.primaryText.length == 1 && key.primaryText[0].isDigit() -> (characterFontSize * 0.85f).sp
                key.primaryText == "12\n34" -> 10.sp
                isSmallFont -> 11.5.sp
                key.primaryText.length == 1 -> (characterFontSize * 0.8f).sp
                else -> 17.sp
            },
            fontWeight = if (key.isAccent || key.isFunctional || key.isActiveModifier) FontWeight.Bold else FontWeight.Normal,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            textAlign = TextAlign.Center,
            lineHeight = if (key.primaryText == "12\n34") 11.sp else 18.sp
        )

        // Alternate characters popup bubble when long-pressing number/symbol keys
        if (showAlternatesPopup) {
            val alternates = KeyAlternatesProvider.map[key.primaryText] ?: NumberKeyAlternates.map[key.primaryText]
            if (alternates != null) {
                Popup(
                    alignment = Alignment.TopCenter,
                    offset = IntOffset(0, -115),
                    onDismissRequest = { showAlternatesPopup = false },
                    properties = PopupProperties(focusable = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.popupBackground,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, colors.enterKeyBackground.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            alternates.forEachIndexed { index, altChar ->
                                val isPrimary = index == 0
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isPrimary) colors.enterKeyBackground else colors.letterKeyBackground)
                                        .clickable {
                                            onKeyClick(key.copy(action = KeyAction.InsertText(altChar)))
                                            showAlternatesPopup = false
                                        }
                                        .testTag("alt_char_${key.primaryText}_$altChar"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = altChar,
                                        color = if (isPrimary) colors.enterKeyTextColor else colors.popupTextColor,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
