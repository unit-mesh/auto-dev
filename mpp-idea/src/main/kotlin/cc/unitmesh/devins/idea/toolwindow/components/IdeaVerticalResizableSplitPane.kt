package cc.unitmesh.devins.idea.toolwindow.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A high-performance vertical resizable split pane for IntelliJ IDEA plugin using Jewel theming.
 * Divides two composables vertically with smooth drag handling and visual feedback.
 *
 * @param modifier The modifier to apply to this layout
 * @param initialSplitRatio The initial split ratio (0.0 to 1.0) for the top pane
 * @param minRatio The minimum split ratio for the top pane
 * @param maxRatio The maximum split ratio for the top pane
 * @param dividerHeight The height of the divider in dp
 * @param top The first composable (top side)
 * @param bottom The second composable (bottom side)
 */
@Composable
fun IdeaVerticalResizableSplitPane(
    modifier: Modifier = Modifier,
    initialSplitRatio: Float = 0.5f,
    minRatio: Float = 0.2f,
    maxRatio: Float = 0.8f,
    dividerHeight: Int = 4,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit
) {
    var splitRatio by remember { mutableStateOf(initialSplitRatio.coerceIn(minRatio, maxRatio)) }
    var isDragging by remember { mutableStateOf(false) }
    var containerHeight by remember { mutableStateOf(0) }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val dividerAlpha by animateFloatAsState(
        targetValue = when {
            isDragging -> 1f
            isHovered -> 0.8f
            else -> 0.4f
        },
        animationSpec = tween(durationMillis = 150),
        label = "dividerAlpha"
    )

    val dividerScale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.2f
            isHovered -> 1.1f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 150),
        label = "dividerScale"
    )

    Layout(
        modifier = modifier,
        content = {
            Box(modifier = Modifier.fillMaxWidth()) { top() }

            Box(
                modifier = Modifier
                    .height(dividerHeight.dp)
                    .fillMaxWidth()
                    .hoverable(interactionSource)
                    .pointerHoverIcon(PointerIcon.Crosshair)
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.3f))
                )
                Spacer(
                    modifier = Modifier
                        .height((dividerHeight * dividerScale).dp)
                        .fillMaxWidth()
                        .alpha(dividerAlpha)
                        .background(
                            when {
                                isDragging -> JewelTheme.globalColors.outlines.focused
                                isHovered -> JewelTheme.globalColors.outlines.focused.copy(alpha = 0.7f)
                                else -> JewelTheme.globalColors.borders.normal
                            }
                        )
                        .pointerInput(containerHeight) {
                            detectDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false }
                            ) { change, dragAmount ->
                                change.consume()
                                if (containerHeight > 0) {
                                    val delta = dragAmount.y / containerHeight
                                    val newRatio = (splitRatio + delta).coerceIn(minRatio, maxRatio)
                                    if (abs(newRatio - splitRatio) > 0.001f) {
                                        splitRatio = newRatio
                                    }
                                }
                            }
                        }
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) { bottom() }
        }
    ) { measurables, constraints ->
        containerHeight = constraints.maxHeight
        val dividerHeightPx = (dividerHeight.dp).roundToPx()
        val availableHeight = constraints.maxHeight - dividerHeightPx
        val topHeight = (availableHeight * splitRatio).roundToInt().coerceAtLeast(0)
        val bottomHeight = (availableHeight - topHeight).coerceAtLeast(0)

        val topPlaceable = measurables[0].measure(Constraints.fixed(constraints.maxWidth, topHeight))
        val dividerPlaceable = measurables[1].measure(Constraints.fixed(constraints.maxWidth, dividerHeightPx))
        val bottomPlaceable = measurables[2].measure(Constraints.fixed(constraints.maxWidth, bottomHeight))

        layout(constraints.maxWidth, constraints.maxHeight) {
            topPlaceable.placeRelative(0, 0)
            dividerPlaceable.placeRelative(0, topHeight)
            bottomPlaceable.placeRelative(0, topHeight + dividerHeightPx)
        }
    }
}

