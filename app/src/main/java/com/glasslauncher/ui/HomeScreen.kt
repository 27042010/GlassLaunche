package com.glasslauncher.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.glasslauncher.AppInfo
import com.glasslauncher.LauncherViewModel
import kotlin.math.absoluteValue

private const val COLS = 4
private const val ROWS = 6
private const val PAGE_SIZE = COLS * ROWS

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val dockApps = vm.dockApps
    val gridApps = vm.gridApps
    val pages = remember(gridApps) { gridApps.chunked(PAGE_SIZE).ifEmpty { listOf(emptyList()) } }
    val pagerState = rememberPagerState { pages.size }

    var editMode by remember { mutableStateOf(false) }
    var overlayOpen by remember { mutableStateOf(false) }
    var focusSearch by remember { mutableStateOf(false) }

    // Home apertado: fecha tudo e volta pra primeira página
    LaunchedEffect(vm.homeSignal) {
        if (vm.homeSignal > 0) {
            editMode = false
            overlayOpen = false
            pagerState.animateScrollToPage(0)
        }
    }

    BackHandler(enabled = editMode || overlayOpen) {
        editMode = false
        overlayOpen = false
    }

    // Blur real do wallpaper (Android 12+) quando o overlay abre
    WindowBlur(if (overlayOpen) 80 else 0)

    val onLaunch: (AppInfo, Rect?) -> Unit = { app, bounds ->
        launchApp(ctx, view, app, bounds)
        overlayOpen = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(editMode, overlayOpen) {
                if (editMode || overlayOpen) return@pointerInput
                var drag = 0f
                detectVerticalDragGestures(
                    onDragStart = { drag = 0f },
                    onVerticalDrag = { _, dy -> drag += dy },
                    onDragEnd = {
                        when {
                            drag > 140f -> {
                                focusSearch = true
                                overlayOpen = true
                            }
                            drag < -140f -> {
                                focusSearch = false
                                overlayOpen = true
                            }
                        }
                    },
                )
            },
    ) {
        // Scrim pra manter legibilidade em wallpapers claros
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                        )
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            // Barra do topo: botão "OK" no modo edição
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                AnimatedVisibility(
                    visible = editMode,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    Box(
                        Modifier
                            .glass(RoundedCornerShape(50), strength = 1.6f)
                            .plainClick { editMode = false }
                            .padding(horizontal = 18.dp, vertical = 7.dp)
                    ) {
                        BasicText("OK", style = TextStyle(color = Color.White, fontSize = 15.sp))
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                editMode = true
                            },
                            onTap = { if (editMode) editMode = false },
                        )
                    },
            ) { page ->
                PageGrid(
                    apps = pages[page],
                    editMode = editMode,
                    onClick = onLaunch,
                    onLongPress = { editMode = true },
                    onRemove = { uninstallApp(ctx, it) },
                    modifier = Modifier.graphicsLayer {
                        // parallax estilo iOS: página que sai encolhe e desbota
                        val offset = ((pagerState.currentPage - page) +
                            pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                        alpha = 1f - offset * 0.65f
                        scaleX = 1f - offset * 0.12f
                        scaleY = 1f - offset * 0.12f
                    },
                )
            }

            if (pages.size > 1) {
                PageDots(
                    count = pages.size,
                    current = pagerState.currentPage,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Spacer(Modifier.height(10.dp))

            // Dock de vidro
            Row(
                Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(34.dp))
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                dockApps.forEachIndexed { i, app ->
                    AppIconCell(
                        app = app,
                        editMode = editMode,
                        seed = 50 + i,
                        showLabel = false,
                        onClick = { onLaunch(app, it) },
                        onLongPress = { editMode = true },
                        onRemove = { uninstallApp(ctx, app) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Overlay: busca / biblioteca de apps
        AnimatedVisibility(
            visible = overlayOpen,
            enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 1.08f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 1.08f),
        ) {
            LibraryOverlay(
                apps = vm.apps,
                focusSearch = focusSearch,
                onLaunch = onLaunch,
                onClose = { overlayOpen = false },
            )
        }
    }
}

@Composable
private fun PageGrid(
    apps: List<AppInfo>,
    editMode: Boolean,
    onClick: (AppInfo, Rect?) -> Unit,
    onLongPress: () -> Unit,
    onRemove: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        apps.chunked(COLS).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(COLS) { c ->
                    val app = row.getOrNull(c)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (app != null) {
                            AppIconCell(
                                app = app,
                                editMode = editMode,
                                seed = r * COLS + c,
                                onClick = { onClick(app, it) },
                                onLongPress = onLongPress,
                                onRemove = { onRemove(app) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .glass(RoundedCornerShape(50), strength = 0.9f)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val size by animateDpAsState(if (i == current) 8.dp else 6.dp, label = "dot")
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (i == current) 1f else 0.45f))
            )
        }
    }
}

@Composable
private fun LibraryOverlay(
    apps: List<AppInfo>,
    focusSearch: Boolean,
    onLaunch: (AppInfo, Rect?) -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val results = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        if (focusSearch) focus.requestFocus()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(16.dp), strength = 1.3f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = rememberVectorPainter(Icons.Rounded.Search),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.8f)),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box {
                    if (query.isEmpty()) {
                        BasicText(
                            "Pesquisar",
                            style = TextStyle(color = Color.White.copy(alpha = 0.6f), fontSize = 17.sp),
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(COLS),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(results, key = { it.key }) { app ->
                    AppIconCell(
                        app = app,
                        editMode = false,
                        seed = 0,
                        onClick = { onLaunch(app, it) },
                        onLongPress = {},
                        onRemove = {},
                    )
                }
            }
        }
    }
}

/** Blur do que está atrás da janela (wallpaper). Só funciona no Android 12+ com blur habilitado. */
@Composable
private fun WindowBlur(target: Int) {
    val ctx = LocalContext.current
    val radius by animateIntAsState(target, tween(300), label = "blur")
    SideEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.findActivity()?.window?.setBackgroundBlurRadius(radius)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
