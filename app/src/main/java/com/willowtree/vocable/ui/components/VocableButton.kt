package com.willowtree.vocable.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.willowtree.vocable.ui.theme.ColorPrimary
import com.willowtree.vocable.ui.theme.ColorPrimaryDark
import com.willowtree.vocable.ui.theme.ColorPrimaryDisabled
import com.willowtree.vocable.ui.theme.ColorAccent
import com.willowtree.vocable.ui.theme.SelectedColor
import com.willowtree.vocable.ui.theme.TextColor
import com.willowtree.vocable.ui.theme.TextColorDisabled

@Composable
fun VocableButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    backgroundColor: Color = ColorPrimary,
    textColor: Color = TextColor,
    content: @Composable RowScope.() -> Unit,
    modifier: Modifier
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Highlight green on press only. Hover shows an amber outline.
    val containerColor = if (isPressed) SelectedColor else backgroundColor
    val contentColor = if (isPressed) ColorPrimaryDark else textColor
    val border = if (isHovered && !isPressed) BorderStroke(4.dp, ColorAccent) else null

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = ColorPrimaryDisabled,
            disabledContentColor = TextColorDisabled
        ),
        border = border,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(0.dp),
        content = content
    )
}
