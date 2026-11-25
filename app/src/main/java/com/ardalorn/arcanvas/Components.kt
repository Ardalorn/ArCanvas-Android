package com.ardalorn.arcanvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser

/** Minimum touch target, as in the web version. */
val TouchMin = 44.dp

/**
 * A button with the web version's feedback: it shrinks slightly while pressed, and shows a clear
 * inner ring when focused by keyboard or D-pad. The click area is [modifier] (size, border, etc.);
 * [contentPadding] is applied inside it so padding never shrinks the touch target.
 */
@Composable
fun Pressable(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    stateText: String? = null,
    ringColor: Color = Tokens.Ink,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()

    Box(
        modifier =
        Modifier
            .graphicsLayer {
                val s = if (pressed) 0.92f else 1f
                scaleX = s
                scaleY = s
            }
            .then(modifier)
            .drawWithContent {
                drawContent()
                if (focused) {
                    val w = 3.dp.toPx()
                    drawRoundRect(
                        color = ringColor,
                        topLeft = Offset(w / 2, w / 2),
                        size = Size(size.width - w, size.height - w),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = Stroke(w),
                    )
                }
            }
            .semantics {
                contentDescription = description
                if (stateText != null) stateDescription = stateText
            }
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Draws one or more SVG paths as an outline icon, scaled from a square [viewBox] grid. */
@Composable
fun PathIcon(
    pathData: List<String>,
    viewBox: Float,
    iconSize: Dp,
    strokeUnits: Float,
    tint: Color = Tokens.Ink,
) {
    val paths =
        remember(pathData) {
            pathData.mapNotNull { PathParser.createPathFromPathData(it)?.asComposePath() }
        }
    Canvas(Modifier.size(iconSize)) {
        val k = this.size.minDimension / viewBox
        scale(k, pivot = Offset.Zero) {
            paths.forEach { path ->
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = strokeUnits, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

object IconPaths {
    val Eraser =
        listOf(
            "m19.5 12-6 6a2.12 2.12 0 0 1-3 0L6 13.5a2.12 2.12 0 0 1 0-3l6-6a2.12 2.12 0 0 1 3 0l4.5 4.5a2.12 2.12 0 0 1 0 3Z",
            "M9 15l-4 4",
        )
    val Trash =
        listOf(
            "M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m3 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14Z",
        )
    val Download =
        listOf(
            "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3",
        )
}
