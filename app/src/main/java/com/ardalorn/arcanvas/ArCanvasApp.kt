package com.ardalorn.arcanvas

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.RoundedCorner
import android.view.View
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Instant when the user has animations turned off in system settings, a short tween otherwise. */
private fun <T> motion(reduce: Boolean): FiniteAnimationSpec<T> = if (reduce) snap() else tween(450)

@Composable
fun ArCanvasApp(vm: CanvasViewModel) {
    val context = LocalContext.current
    val config = LocalConfiguration.current

    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val screenReaderOn = remember {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            .let { it as? AccessibilityManager }?.isTouchExplorationEnabled == true
    }
    // A side toolbar only when there is room for all of it; landscape phones keep the bottom bar.
    val sideToolbar = config.screenWidthDp > 600 && config.screenHeightDp >= 480

    // Focus mode: Back is swallowed so the app cannot be closed by accident. It wakes the
    // island instead, so the way out is always one glance away.
    BackHandler(enabled = vm.focusMode) { vm.ping() }

    // Focus mode pins the app (Android screen pinning), which blocks the notification shade, quick settings
    // and the swipe-up Home and Recents gestures. Leaving Focus mode, or the system unpinning, ends it.
    LaunchedEffect(vm.focusMode) { context.findActivity()?.setPinned(vm.focusMode) }

    // The island fades out after a few idle seconds.
    LaunchedEffect(vm.islandVisible) {
        if (vm.islandVisible) {
            while (SystemClock.uptimeMillis() - vm.lastPing < ISLAND_IDLE_MS) delay(250)
            vm.hideIsland()
        }
    }

    // The canvas paper is fixed (Tokens.Paper); only the interface on top of it follows Material You.
    Surface(Modifier.fillMaxSize(), color = Tokens.Paper, contentColor = MaterialTheme.colorScheme.onSurface) {
        Box(Modifier.fillMaxSize()) {
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
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(
                        WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                    )
                    .padding(top = 4.dp),
            ) {
                Island(vm, reduceMotion)
            }
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
        modifier = Modifier
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

/** How far the title and the Focus button reach in from each side of the header. */
private const val HEADER_EDGE_ZONE_DP = 150

/**
 * Space to keep clear above the header for a camera cutout. A hole in the empty middle of the header, the usual
 * place, needs no gap. A hole near a corner would sit behind the title or the Focus button, so it gets the full
 * cutout height.
 */
@Composable
private fun headerTopClearance(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val cutoutTop = WindowInsets.displayCutout.getTop(density)
    return remember(cutoutTop, widthDp) {
        val zone = HEADER_EDGE_ZONE_DP * density.density
        val width = widthDp * density.density
        val rects = ViewCompat.getRootWindowInsets(view)?.displayCutout?.boundingRects.orEmpty()
        val behindContent = rects.any { it.top < cutoutTop && (it.left < zone || it.right > width - zone) }
        if (cutoutTop > 0 && behindContent) with(density) { cutoutTop.toDp() } else 0.dp
    }
}

/** Gap kept between a control and the curve of a rounded display corner. */
private val CornerMargin = 8.dp

/**
 * Distance in px that a control must keep from the side edge so it stays inside the rounded display corner at
 * [position]. [offset] is how far the control sits from the top or bottom edge. Below Android 12 the system does not
 * report corner radii, so no extra space is added there.
 */
private fun cornerClearancePx(view: View, position: Int, offset: Float, margin: Float): Float {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return 0f
    val radius = view.rootWindowInsets?.getRoundedCorner(position)?.radius?.toFloat() ?: return 0f
    if (offset >= radius) return 0f
    val rise = radius - offset
    return radius - sqrt(radius * radius - rise * rise) + margin
}

/**
 * Pads a full-width bar sideways so that controls at its ends are not clipped by the rounded top corners of the
 * display. The display cutout insets only cover camera holes, never the screen's own corners.
 *
 * [controlTop] is the distance from the top of the screen to the top of the controls, and [alreadyPadded] is the
 * sideways padding the bar already has, which counts towards the clearance.
 */
private fun Modifier.avoidTopRoundedCorners(view: View, controlTop: Dp, alreadyPadded: Dp): Modifier =
    layout { measurable, constraints ->
        val offset = controlTop.toPx()
        val have = alreadyPadded.toPx()
        val margin = CornerMargin.toPx()
        val left = (cornerClearancePx(view, RoundedCorner.POSITION_TOP_LEFT, offset, margin) - have)
            .coerceAtLeast(0f).roundToInt()
        val right = (cornerClearancePx(view, RoundedCorner.POSITION_TOP_RIGHT, offset, margin) - have)
            .coerceAtLeast(0f).roundToInt()
        val placeable = measurable.measure(constraints.offset(horizontal = -(left + right)))
        layout(constraints.constrainWidth(placeable.width + left + right), placeable.height) {
            placeable.place(left, 0)
        }
    }

@Composable
private fun Header(vm: CanvasViewModel) {
    val colors = MaterialTheme.colorScheme
    val topClearance = headerTopClearance()
    val view = LocalView.current
    val padHorizontal = 14.dp
    val padVertical = 10.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainer.copy(alpha = 0.94f))
            .drawBehind {
                val h = 2.dp.toPx()
                drawRect(colors.onSurface, Offset(0f, size.height - h), Size(size.width, h))
            }
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .avoidTopRoundedCorners(view, controlTop = topClearance + padVertical, alreadyPadded = padHorizontal)
            .padding(top = topClearance)
            .padding(horizontal = padHorizontal, vertical = padVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "ArCanvas",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        val shape = RoundedCornerShape(4.dp)
        Pressable(
            onClick = vm::toggleFocus,
            description = "Enter focus mode",
            modifier = Modifier
                .heightIn(min = TouchMin)
                .clip(shape)
                .background(colors.primary)
                .border(2.dp, colors.primary, shape),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ringColor = colors.onPrimary,
        ) {
            Text(
                text = "Focus",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.onPrimary,
            )
        }
    }
}

@Composable
private fun Toolbar(vm: CanvasViewModel, vertical: Boolean) {
    val colors = MaterialTheme.colorScheme
    val shape: Shape = if (vertical) {
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
                        .background(colors.onSurface),
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
                        .border(2.dp, colors.onSurface, CircleShape),
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

    // Only the cutout edges this bar can actually touch. A bar at the bottom must not take the top cutout.
    val cutoutSides = if (vertical) {
        WindowInsetsSides.Start
    } else {
        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
    }
    val base = if (vertical) Modifier else Modifier.fillMaxWidth()
    Box(
        modifier = base
            .clip(shape)
            .background(colors.surfaceContainer.copy(alpha = 0.96f))
            .drawBehind {
                val t = 2.dp.toPx()
                drawRect(colors.onSurface, Offset.Zero, Size(size.width, t)) // top edge
                if (vertical) {
                    drawRect(colors.onSurface, Offset(0f, size.height - t), Size(size.width, t)) // bottom edge
                    drawRect(colors.onSurface, Offset(size.width - t, 0f), Size(t, size.height)) // right edge
                }
            }
            .windowInsetsPadding(WindowInsets.displayCutout.only(cutoutSides))
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
private fun ToolGroup(label: String, vertical: Boolean, content: @Composable () -> Unit) {
    if (vertical) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .background(MaterialTheme.colorScheme.outline),
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
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(4.dp)
    Pressable(
        onClick = onClick,
        description = description,
        stateText = if (selected) "Selected" else null,
        modifier = Modifier
            .size(TouchMin)
            .clip(shape)
            .background(if (selected) colors.secondaryContainer else Color.Transparent)
            .border(2.dp, if (selected) colors.onSurface else colors.outline, shape)
            .drawBehind {
                if (selected) {
                    val h = 4.dp.toPx()
                    drawRect(colors.onSurface, Offset(0f, size.height - h), Size(size.width, h))
                }
            },
    ) {
        // Icons draw in the current content colour, so the selected tint pairs with its container.
        val iconColor = if (selected) colors.onSecondaryContainer else colors.onSurface
        CompositionLocalProvider(LocalContentColor provides iconColor) { content() }
    }
}

@Composable
private fun ActionButton(onClick: () -> Unit, description: String, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(4.dp)
    Pressable(
        onClick = onClick,
        description = description,
        modifier = Modifier
            .size(TouchMin)
            .clip(shape)
            .border(2.dp, colors.onSurface, shape),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onSurface) { content() }
    }
}

// ───────────── Focus-mode island ─────────────

@Composable
private fun Island(vm: CanvasViewModel, reduceMotion: Boolean) {
    val transition = rememberInfiniteTransition(label = "island")
    val breathe = transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "breathe",
    )
    val pulseAlpha = transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse-alpha",
    )
    val pulseScale = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse-scale",
    )
    val reminder = vm.sessionPulse
    val colors = MaterialTheme.colorScheme

    Pressable(
        onClick = vm::toggleFocus,
        description = if (reminder) {
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
                    alpha = when {
                        reduceMotion -> 1f
                        reminder -> pulseAlpha.value
                        else -> breathe.value
                    }
                    val s = if (reduceMotion || !reminder) 1f else pulseScale.value
                    scaleX = s
                    scaleY = s
                }
                .clip(RoundedCornerShape(8.dp))
                .background(if (reminder) colors.onSurfaceVariant else colors.onSurface),
        )
    }
}

// ───────────── Screen pinning ─────────────

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Pins or unpins the app. Pinning shows the system's own confirmation the first time, and the person can
 * still unpin with the system gesture (hold Back and Overview, or swipe up and hold). An app that is not a
 * device owner cannot lock the device any harder than this.
 */
private fun Activity.setPinned(pinned: Boolean) {
    val manager = getSystemService(ActivityManager::class.java) ?: return
    val locked = manager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    if (pinned && !locked) {
        startLockTask()
    } else if (!pinned && locked) {
        stopLockTask()
    }
}
