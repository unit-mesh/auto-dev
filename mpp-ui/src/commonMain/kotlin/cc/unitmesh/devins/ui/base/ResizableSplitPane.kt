package cc.unitmesh.devins.ui.base

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A high-performance resizable split pane that divides two composables horizontally
 * Optimized for smooth dragging and excellent visual feedback
 *
 * @param modifier The modifier to apply to this layout
 * @param initialSplitRatio The initial split ratio (0.0 to 1.0) for the first pane
 * @param minRatio The minimum split ratio for the first pane
 * @param maxRatio The maximum split ratio for the first pane
 * @param dividerWidth The width of the divider in dp
 * @param saveKey Optional key for saving/restoring split ratio across app sessions. If null, state is not persisted.
 * @param first The first composable (left side)
 * @param second The second composable (right side)
 */
@Composable
fun ResizableSplitPane(
    modifier: Modifier = Modifier,
    initialSplitRatio: Float = 0.5f,
    minRatio: Float = 0.2f,
    maxRatio: Float = 0.8f,
    dividerWidth: Int = 8,
    saveKey: String? = null,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    // Use rememberSaveable when saveKey is provided for persistent state
    var splitRatio by if (saveKey != null) {
        rememberSaveable(key = saveKey) { mutableStateOf(initialSplitRatio.coerceIn(minRatio, maxRatio)) }
    } else {
        remember { mutableStateOf(initialSplitRatio.coerceIn(minRatio, maxRatio)) }
    }
    var isDragging by remember { mutableStateOf(false) }
    var containerWidth by remember { mutableStateOf(0) }

    // Track hover state for smooth animations
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Smooth opacity animation for hover effect
    val dividerAlpha by animateFloatAsState(
        targetValue = when {
            isDragging -> 1f
            isHovered -> 0.8f
            else -> 0.4f
        },
        animationSpec = tween(durationMillis = 150),
        label = "dividerAlpha"
    )

    // Smooth scale animation for better visual feedback
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
            // First pane
            Box(modifier = Modifier.fillMaxHeight()) {
                first()
            }

            // Enhanced divider with visual feedback
            Box(
                modifier = Modifier
                    .width(dividerWidth.dp)
                    .fillMaxHeight()
                    .hoverable(interactionSource)
                    .pointerHoverIcon(PointerIcon.Crosshair) // Better cursor for resizing
            ) {
                // Background layer with subtle gradient effect
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                )

                // Highlighted center strip for grabbing
                Spacer(
                    modifier = Modifier
                        .width((dividerWidth * dividerScale).dp)
                        .fillMaxHeight()
                        .alpha(dividerAlpha)
                        .background(
                            when {
                                isDragging -> MaterialTheme.colorScheme.primary
                                isHovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                        .pointerInput(containerWidth) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                },
                                onDragEnd = {
                                    isDragging = false
                                },
                                onDragCancel = {
                                    isDragging = false
                                }
                            ) { change, dragAmount ->
                                change.consume()

                                // Calculate delta based on total container width for accurate dragging
                                if (containerWidth > 0) {
                                    val delta = dragAmount.x / containerWidth

                                    // Optimize: Only update if change is meaningful (reduces recomposition)
                                    val newRatio = (splitRatio + delta).coerceIn(minRatio, maxRatio)
                                    if (abs(newRatio - splitRatio) > 0.001f) {
                                        splitRatio = newRatio
                                    }
                                }
                            }
                        }
                )
            }

            // Second pane
            Box(modifier = Modifier.fillMaxHeight()) {
                second()
            }
        }
    ) { measurables, constraints ->
        // Store container width for accurate drag calculations
        containerWidth = constraints.maxWidth

        // Pre-calculate dimensions once for better performance
        val dividerWidthPx = (dividerWidth.dp).roundToPx()
        val availableWidth = constraints.maxWidth - dividerWidthPx

        // Calculate pane widths
        val firstWidth = (availableWidth * splitRatio).roundToInt().coerceAtLeast(0)
        val secondWidth = (availableWidth - firstWidth).coerceAtLeast(0)

        // Measure all children with fixed constraints
        val firstPlaceable = measurables[0].measure(
            Constraints.fixed(firstWidth, constraints.maxHeight)
        )

        val dividerPlaceable = measurables[1].measure(
            Constraints.fixed(dividerWidthPx, constraints.maxHeight)
        )

        val secondPlaceable = measurables[2].measure(
            Constraints.fixed(secondWidth, constraints.maxHeight)
        )

        // Place children in layout
        layout(constraints.maxWidth, constraints.maxHeight) {
            firstPlaceable.placeRelative(0, 0)
            dividerPlaceable.placeRelative(firstWidth, 0)
            secondPlaceable.placeRelative(firstWidth + dividerWidthPx, 0)
        }
    }
}
