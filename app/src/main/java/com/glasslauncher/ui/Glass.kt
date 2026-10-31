package com.glasslauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * "Vidro" estilo iOS: tint escuro leve + gradiente branco translúcido + borda com
 * reflexo de luz (mais forte em cima/esquerda, some embaixo).
 * O blur real do fundo é feito pela janela (ver WindowBlur) no overlay.
 */
fun Modifier.glass(shape: Shape, strength: Float = 1f): Modifier = this
    .clip(shape)
    .background(Color.Black.copy(alpha = 0.10f * strength))
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.30f * strength),
                Color.White.copy(alpha = 0.12f * strength),
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.70f),
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.35f),
            )
        ),
        shape = shape,
    )

/** Clique sem ripple/indicação. */
fun Modifier.plainClick(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}
