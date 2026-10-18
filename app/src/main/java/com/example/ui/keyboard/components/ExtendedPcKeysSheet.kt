package com.example.ui.keyboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.keyboard.model.KeyAction
import com.example.ui.keyboard.model.KeyboardColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExtendedPcKeysSheet(
    colors: KeyboardColors,
    onDismiss: () -> Unit,
    onAction: (KeyAction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💻 Extended PC / Laptop Keys",
                    color = colors.letterKeyTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_extended_pc")) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.toolbarIconTint
                    )
                }
            }

            Text(
                text = "Special keys found on standard physical laptops and desktop keyboards",
                color = colors.letterKeySecondaryTextColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Function Keys F1 - F12
            Text(
                text = "FUNCTION KEYS (F1 - F12)",
                color = colors.enterKeyBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..12).forEach { num ->
                    PcKeyButton(
                        label = "F$num",
                        colors = colors,
                        onClick = {
                            onAction(KeyAction.FunctionKey(num))
                            onDismiss()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation & Control Keys
            Text(
                text = "NAVIGATION & EDITING",
                color = colors.enterKeyBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PcKeyButton("ESC", colors) { onAction(KeyAction.Escape) }
                PcKeyButton("TAB (↹)", colors) { onAction(KeyAction.Tab) }
                PcKeyButton("HOME", colors) { onAction(KeyAction.Home) }
                PcKeyButton("END", colors) { onAction(KeyAction.End) }
                PcKeyButton("PG UP", colors) { onAction(KeyAction.PageUp) }
                PcKeyButton("PG DN", colors) { onAction(KeyAction.PageDown) }
                PcKeyButton("DEL (Forward)", colors) { onAction(KeyAction.DeleteForward) }
                PcKeyButton("CTRL + A (Select All)", colors) { onAction(KeyAction.SelectAll); onDismiss() }
                PcKeyButton("CTRL + C (Copy)", colors) { onAction(KeyAction.Copy); onDismiss() }
                PcKeyButton("CTRL + V (Paste)", colors) { onAction(KeyAction.Paste); onDismiss() }
                PcKeyButton("CTRL + X (Cut)", colors) { onAction(KeyAction.Cut); onDismiss() }
                PcKeyButton("CTRL + Z (Undo)", colors) { onAction(KeyAction.Undo) }
                PcKeyButton("CTRL + Y (Redo)", colors) { onAction(KeyAction.Redo) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Developer / Laptop Coding Symbols
            Text(
                text = "DEVELOPER & LAPTOP SYMBOLS",
                color = colors.enterKeyBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val symbols = listOf("`", "~", "|", "\\", "^", "=>", "===", "!==", "&&", "||", "++", "--", "\${}", "/*", "*/", "->")
                symbols.forEach { sym ->
                    PcKeyButton(sym, colors) { onAction(KeyAction.InsertText(sym)) }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PcKeyButton(
    label: String,
    colors: KeyboardColors,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.functionKeyBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = colors.functionKeyTextColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}
