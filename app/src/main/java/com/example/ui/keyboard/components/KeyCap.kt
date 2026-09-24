package com.example.ui.keyboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.keyboard.model.KeyAction
import com.example.ui.keyboard.model.KeyItem
import com.example.ui.keyboard.model.KeyboardColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.KeyCap(
    key: KeyItem,
    colors: KeyboardColors,
    heightDp: Int = 46,
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
    val isSmallFont = key.primaryText.length > 2 || key.primaryText == "English"
    val isMono = key.primaryText in listOf("ESC", "CTRL", "ALT", "HOME", "END", "PGUP", "PGDN", "↹")
    val isDeleteKey = key.action is KeyAction.Backspace || key.action is KeyAction.DeleteForward

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
            onLongClick = {
                if (key.secondaryText != null) {
                    onKeyClick(key.copy(action = KeyAction.InsertText(key.secondaryText)))
                } else if (onKeyLongClick != null) {
                    onKeyLongClick(key)
                }
            }
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

        Text(
            text = key.primaryText,
            color = textColor,
            fontSize = when {
                isFunctionKey -> 12.sp
                key.primaryText.length == 1 && key.primaryText[0].isLetter() -> 22.sp
                key.primaryText.length == 1 && key.primaryText[0].isDigit() -> 20.sp
                key.primaryText == "English" -> 14.sp
                key.primaryText == "12\n34" -> 10.sp
                isSmallFont -> 12.sp
                else -> 17.sp
            },
            fontWeight = if (key.isAccent || key.isFunctional || key.isActiveModifier) FontWeight.Bold else FontWeight.Normal,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            textAlign = TextAlign.Center,
            lineHeight = if (key.primaryText == "12\n34") 11.sp else 18.sp
        )
    }
}

