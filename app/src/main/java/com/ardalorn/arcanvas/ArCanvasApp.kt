package com.ardalorn.arcanvas

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Instant when the user has animations turned off in system settings, a short tween otherwise. */
private fun <T> motion(reduce: Boolean): FiniteAnimationSpec<T> = if (reduce) snap() else tween(450)

@Composable
fun ArCanvasApp(vm: CanvasViewModel) {
    val context = LocalContext.current
    val config = LocalConfiguration.current

    val reduceMotion =
        remember {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }
    val screenReaderOn =
        remember {
            context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                .let { it as? AccessibilityManager }?.isTouchExplorationEnabled == true
        }
    // A side toolbar only when there is room for all of it; landscape phones keep the bottom bar.
    val sideToolbar = config.screenWidthDp > 600 && config.screenHeightDp >= 480

    // Focus mode: Back is swallowed so the app cannot be closed by accident. It wakes the
    // island instead, so the way out is always one glance away.
    BackHandler(enabled = vm.focusMode) { vm.ping() }

    // The island fades out after a few idle seconds.
    LaunchedEffect(vm.islandVisible) {
        if (vm.islandVisible) {
            while (SystemClock.uptimeMillis() - vm.lastPing < ISLAND_IDLE_MS) delay(250)
            vm.hideIsland()
        }
    }

    Box(Modifier.fillMaxSize().background(Tokens.Paper)) {
        DrawingSurface(vm)

        AnimatedVisibility(
            visible = !vm.focusMode,
            enter = slideInVertically(motion<IntOffset>(reduceMotion)) { -it } + fadeIn(motion(reduceMotion)),
            exit = slideOutVertically(motion<IntOffset>(reduceMotion)) { -it } + fadeOut(motion(reduceMotion)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Header(vm)
        }

        if (sideToolbar) {
            AnimatedVisibility(
                visible = !vm.focusMode,
                enter = slideInHorizontally(motion<IntOffset>(reduceMotion)) { -it } + fadeIn(motion(reduceMotion)),
                exit = slideOutHorizontally(motion<IntOffset>(reduceMotion)) { -it } + fadeOut(motion(reduceMotion)),
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Toolbar(vm, vertical = true)
            }
        } else {
            AnimatedVisibility(
                visible = !vm.focusMode,
                enter = slideInVertically(motion<IntOffset>(reduceMotion)) { it } + fadeIn(motion(reduceMotion)),
                exit = slideOutVertically(motion<IntOffset>(reduceMotion)) { it } + fadeOut(motion(reduceMotion)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Toolbar(vm, vertical = false)
            }
        }

        // With a screen reader on, the exit control stays reachable instead of fading away.
        AnimatedVisibility(
            visible = vm.focusMode && (vm.islandVisible || screenReaderOn),
            enter = fadeIn(motion(reduceMotion)),
            exit = fadeOut(motion(reduceMotion)),
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .padding(top = 4.dp),
        ) {
            Island(vm, reduceMotion)
        }
    }
}

// ───────────── Drawing surface ─────────────

@Composable
private fun DrawingSurface(vm: CanvasViewModel) {
    val view = LocalView.current
    val density = LocalDensity.current.density
    val bitmap = vm.bitmap
    val image = remember(bitmap) { bitmap?.asImageBitmap() }

    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Drawing canvas" }
                .pointerInput(vm) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedCount = event.changes.count { it.pressed }
                            for (change in event.changes) {
                                // Mouse: primary button only. Touch and stylus always draw.
                                if (change.type == PointerType.Mouse && !event.buttons.isPrimaryPressed) continue
                                if (change.changedToDown()) {
                                    if (pressedCount == 1) {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    }
                                    vm.onDown(change.position.x, change.position.y)
                                    change.consume()
                                } else if (change.pressed && change.positionChanged()) {
                                    vm.onMove(
                                        change.previousPosition.x,
                                        change.previousPosition.y,
                                        change.position.x,
                                        change.position.y,
                                    )
                                    change.consume()
                                }
                            }
                        }
                    }
                }
                .onSizeChanged { vm.ensureSize(it.width, it.height, density) },
    ) {
        @Suppress("UNUSED_VARIABLE")
        val redrawTick = vm.version // reading it makes every stroke redraw this surface
        image?.let { drawImage(it) }
    }
}

// ───────────── Header and toolbar ─────────────

@Composable
private fun Header(vm: CanvasViewModel) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Tokens.Paper.copy(alpha = 0.94f))
                .drawBehind {
                    val h = 2.dp.toPx()
                    drawRect(Tokens.Ink, Offset(0f, size.height - h), Size(size.width, h))
                }
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(
            text = "ArCanvas",
            style = TextStyle(color = Tokens.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.semantics { heading() },
        )
        val shape = RoundedCornerShape(4.dp)
        Pressable(
            onClick = vm::toggleFocus,
            description = "Enter focus mode",
            modifier =
                Modifier
                    .heightIn(min = TouchMin)
                    .clip(shape)
                    .background(Tokens.Ink)
                    .border(2.dp, Tokens.Ink, shape),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ringColor = Tokens.Paper,
        ) {
            BasicText(
                text = "Focus",
                style = TextStyle(color = Tokens.Paper, fontSize = 16.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun Toolbar(
    vm: CanvasViewModel,
    vertical: Boolean,
) {
    val shape: Shape =
        if (vertical) {
            RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
        } else {
            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
        }

    val items: @Composable () -> Unit = {
        ToolGroup("Size", vertical) {
            Slot(
                onClick = vm::cycleSize,
                description = "Brush size: ${BrushSizes[vm.sizeIndex].name}",
            ) {
                Box(
                    Modifier
                        .size(BrushSizes[vm.sizeIndex].dotDp.dp)
                        .clip(CircleShape)
                        .background(Tokens.Ink),
                )
            }
        }
        Divider(vertical)
        ToolGroup("Color", vertical) {
            val swatch = Swatches[vm.colorIndex]
            Slot(onClick = vm::cycleColor, description = "Color: ${swatch.name}") {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(swatch.color)
                        .border(2.dp, Tokens.Ink, CircleShape),
                )
            }
        }
        Divider(vertical)
        ToolGroup("Tool", vertical) {
            Slot(
                onClick = vm::toggleEraser,
                description = "Eraser",
                selected = vm.tool == CanvasViewModel.Tool.ERASER,
            ) { PathIcon(IconPaths.Eraser, 24f, 22.dp, 2f) }
            val stamp = Stamps.all[vm.stampIndex]
            Slot(
                onClick = vm::tapStamp,
                description = "Stamp: ${stamp.name}",
                selected = vm.tool == CanvasViewModel.Tool.STAMP,
            ) { PathIcon(listOf(stamp.pathData), 100f, 22.dp, 8f) }
        }
        Divider(vertical)
        ToolGroup("Actions", vertical) {
            ActionButton(vm::clearAll, "Clear all") { PathIcon(IconPaths.Trash, 24f, 20.dp, 2f) }
            ActionButton(vm::saveDrawing, "Save drawing") { PathIcon(IconPaths.Download, 24f, 20.dp, 2f) }
        }
    }

    val base = if (vertical) Modifier else Modifier.fillMaxWidth()
    Box(
        modifier =
            base
                .clip(shape)
                .background(Tokens.Paper.copy(alpha = 0.96f))
                .drawBehind {
                    val t = 2.dp.toPx()
                    drawRect(Tokens.Ink, Offset.Zero, Size(size.width, t)) // top edge
                    if (vertical) {
                        drawRect(Tokens.Ink, Offset(0f, size.height - t), Size(size.width, t)) // bottom edge
                        drawRect(Tokens.Ink, Offset(size.width - t, 0f), Size(t, size.height)) // right edge
                    }
                }
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(
                    if (vertical) {
                        PaddingValues(horizontal = 10.dp, vertical = 18.dp)
                    } else {
                        PaddingValues(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 14.dp)
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (vertical) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { items() }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) { items() }
        }
    }
}

/** A labelled group of tools. The label is only shown in the side toolbar, where there is room. */
@Composable
private fun ToolGroup(
    label: String,
    vertical: Boolean,
    content: @Composable () -> Unit,
) {
    if (vertical) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                text = label.uppercase(),
                style =
                    TextStyle(
                        color = Tokens.Soft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    ),
            )
            content()
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }
}

@Composable
private fun Divider(vertical: Boolean) {
    Box(
        Modifier
            .size(width = if (vertical) 28.dp else 1.dp, height = if (vertical) 1.dp else 28.dp)
            .background(Tokens.Edge),
    )
}

/** Tool slot: outlined square; the selected one gets a paper tint and a heavier bottom edge. */
@Composable
private fun Slot(
    onClick: () -> Unit,
    description: String,
    selected: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    Pressable(
        onClick = onClick,
        description = description,
        stateText = if (selected) "Selected" else null,
        modifier =
            Modifier
                .size(TouchMin)
                .clip(shape)
                .background(if (selected) Tokens.Paper2 else Tokens.Paper.copy(alpha = 0f))
                .border(2.dp, if (selected) Tokens.Ink else Tokens.Edge, shape)
                .drawBehind {
                    if (selected) {
                        val h = 4.dp.toPx()
                        drawRect(Tokens.Ink, Offset(0f, size.height - h), Size(size.width, h))
                    }
                },
    ) { content() }
}

@Composable
private fun ActionButton(
    onClick: () -> Unit,
    description: String,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    Pressable(
        onClick = onClick,
        description = description,
        modifier =
            Modifier
                .size(TouchMin)
                .clip(shape)
                .border(2.dp, Tokens.Ink, shape),
    ) { content() }
}

// ───────────── Focus-mode island ─────────────

@Composable
private fun Island(
    vm: CanvasViewModel,
    reduceMotion: Boolean,
) {
    val transition = rememberInfiniteTransition(label = "island")
    val breathe =
        transition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
            label = "breathe",
        )
    val pulseAlpha =
        transition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
            label = "pulse-alpha",
        )
    val pulseScale =
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
            label = "pulse-scale",
        )
    val reminder = vm.sessionPulse

    Pressable(
        onClick = vm::toggleFocus,
        description =
            if (reminder) {
                "Exit focus mode. You have been drawing for over 10 minutes."
            } else {
                "Exit focus mode"
            },
        modifier = Modifier.size(56.dp),
    ) {
        Box(
            Modifier
                .size(width = 32.dp, height = 16.dp)
                .graphicsLayer {
                    alpha =
                        when {
                            reduceMotion -> 1f
                            reminder -> pulseAlpha.value
                            else -> breathe.value
                        }
                    val s = if (reduceMotion || !reminder) 1f else pulseScale.value
                    scaleX = s
                    scaleY = s
                }
                .clip(RoundedCornerShape(8.dp))
                .background(if (reminder) Tokens.Soft else Tokens.Ink),
        )
    }
}
