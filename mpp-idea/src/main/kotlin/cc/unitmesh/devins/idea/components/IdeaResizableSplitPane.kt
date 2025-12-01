package cc.unitmesh.devins.idea.components

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
 * A high-performance resizable split pane for IntelliJ IDEA plugin using Jewel theming.
 * Divides two composables horizontally with smooth drag handling and visual feedback.
 *
 * @param modifier The modifier to apply to this layout
 * @param initialSplitRatio The initial split ratio (0.0 to 1.0) for the first pane
 * @param minRatio The minimum split ratio for the first pane
 * @param maxRatio The maximum split ratio for the first pane
 * @param dividerWidth The width of the divider in dp
 * @param first The first composable (left side)
 * @param second The second composable (right side)
 */
@Composable
fun IdeaResizableSplitPane(
    modifier: Modifier = Modifier,
    initialSplitRatio: Float = 0.5f,
    minRatio: Float = 0.2f,
    maxRatio: Float = 0.8f,
    dividerWidth: Int = 4,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    var splitRatio by remember { mutableStateOf(initialSplitRatio.coerceIn(minRatio, maxRatio)) }
    var isDragging by remember { mutableStateOf(false) }
    var containerWidth by remember { mutableStateOf(0) }

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
            Box(modifier = Modifier.fillMaxHeight()) { first() }

            Box(
                modifier = Modifier
                    .width(dividerWidth.dp)
                    .fillMaxHeight()
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
                        .width((dividerWidth * dividerScale).dp)
                        .fillMaxHeight()
                        .alpha(dividerAlpha)
                        .background(
                            when {
                                isDragging -> JewelTheme.globalColors.outlines.focused
                                isHovered -> JewelTheme.globalColors.outlines.focused.copy(alpha = 0.7f)
                                else -> JewelTheme.globalColors.borders.normal
                            }
                        )
                        .pointerInput(containerWidth) {
                            detectDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false }
                            ) { change, dragAmount ->
                                change.consume()
                                if (containerWidth > 0) {
                                    val delta = dragAmount.x / containerWidth
                                    val newRatio = (splitRatio + delta).coerceIn(minRatio, maxRatio)
                                    if (abs(newRatio - splitRatio) > 0.001f) {
                                        splitRatio = newRatio
                                    }
                                }
                            }
                        }
                )
            }

            Box(modifier = Modifier.fillMaxHeight()) { second() }
        }
    ) { measurables, constraints ->
        containerWidth = constraints.maxWidth
        val dividerWidthPx = (dividerWidth.dp).roundToPx()
        val availableWidth = constraints.maxWidth - dividerWidthPx
        val firstWidth = (availableWidth * splitRatio).roundToInt().coerceAtLeast(0)
        val secondWidth = (availableWidth - firstWidth).coerceAtLeast(0)

        val firstPlaceable = measurables[0].measure(Constraints.fixed(firstWidth, constraints.maxHeight))
        val dividerPlaceable = measurables[1].measure(Constraints.fixed(dividerWidthPx, constraints.maxHeight))
        val secondPlaceable = measurables[2].measure(Constraints.fixed(secondWidth, constraints.maxHeight))

        layout(constraints.maxWidth, constraints.maxHeight) {
            firstPlaceable.placeRelative(0, 0)
            dividerPlaceable.placeRelative(firstWidth, 0)
            secondPlaceable.placeRelative(firstWidth + dividerWidthPx, 0)
        }
    }
}

