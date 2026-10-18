package com.example.ui.keyboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.keyboard.model.KeyItem
import com.example.ui.keyboard.model.KeyboardColors
import com.example.ui.keyboard.model.KeyboardLayoutGenerator

@Composable
fun LaptopKeysBar(
    colors: KeyboardColors,
    isCtrlActive: Boolean,
    isAltActive: Boolean,
    heightDp: Int = 44,
    horizontalPaddingDp: Float = 3.5f,
    verticalPaddingDp: Float = 3.5f,
    cornerRadiusDp: Int = 10,
    fontSize: Float = 13.5f,
    onKeyClick: (KeyItem) -> Unit,
    onKeyLongClick: ((KeyItem) -> Unit)? = null
) {
    val laptopRows = KeyboardLayoutGenerator.getLaptopKeyRows(isCtrlActive, isAltActive)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.laptopRowBackground)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        laptopRows.forEach { rowKeys ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = (verticalPaddingDp * 0.35f).dp)
            ) {
                rowKeys.forEach { key ->
                    KeyCap(
                        key = key,
                        colors = colors,
                        heightDp = heightDp,
                        characterFontSize = fontSize,
                        horizontalPaddingDp = horizontalPaddingDp,
                        verticalPaddingDp = verticalPaddingDp,
                        cornerRadiusDp = cornerRadiusDp,
                        onKeyClick = onKeyClick,
                        onKeyLongClick = onKeyLongClick
                    )
                }
            }
        }
    }
}
