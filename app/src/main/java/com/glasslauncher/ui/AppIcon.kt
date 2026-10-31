package com.glasslauncher.ui

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasslauncher.AppInfo

/** Tremedeira do "modo edição" do iOS: cada ícone com fase diferente. */
fun Modifier.jiggle(enabled: Boolean, seed: Int): Modifier = composed {
    if (!enabled) {
        Modifier
    } else {
        val transition = rememberInfiniteTransition(label = "jiggle")
        val angle = transition.animateFloat(
            initialValue = -2.2f,
            targetValue = 2.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 120 + (seed % 5) * 8, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset((seed * 17) % 120),
            ),
            label = "angle",
        )
        Modifier.graphicsLayer { rotationZ = angle.value }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconCell(
    app: AppInfo,
    editMode: Boolean,
    seed: Int,
    onClick: (Rect?) -> Unit,
    onLongPress: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    var bounds by remember { mutableStateOf<Rect?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val shape = RoundedCornerShape(percent = 24)

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .jiggle(editMode, seed)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = { if (!editMode) onClick(bounds) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier
                    .size(60.dp)
                    .shadow(6.dp, shape, clip = false)
                    .clip(shape)
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), shape)
                    .onGloballyPositioned { bounds = it.boundsInWindow() },
            )
            if (editMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-6).dp, y = (-6).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .glass(CircleShape, strength = 2.4f)
                        .plainClick(onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "−",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(5.dp))
            BasicText(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(Color.Black.copy(alpha = 0.55f), blurRadius = 6f),
                ),
                modifier = Modifier.width(72.dp),
            )
        }
    }
}

/** Abre o app com a animação de zoom saindo do ícone (igual iOS). */
fun launchApp(ctx: Context, view: View, app: AppInfo, bounds: Rect?) {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setClassName(app.packageName, app.activityName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    val options = bounds?.let {
        ActivityOptions.makeScaleUpAnimation(
            view,
            it.left.toInt(),
            it.top.toInt(),
            it.width.toInt(),
            it.height.toInt(),
        ).toBundle()
    }
    try {
        ctx.startActivity(intent, options)
    } catch (_: Exception) {
        // app removido/desativado entre o refresh e o toque
    }
}

fun uninstallApp(ctx: Context, app: AppInfo) {
    val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        ctx.startActivity(intent)
    } catch (_: Exception) {
    }
}
