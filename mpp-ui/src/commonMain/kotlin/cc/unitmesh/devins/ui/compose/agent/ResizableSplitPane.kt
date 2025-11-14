package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A resizable split pane that divides two composables horizontally
 *
 * @param modifier The modifier to apply to this layout
 * @param initialSplitRatio The initial split ratio (0.0 to 1.0) for the first pane
 * @param minRatio The minimum split ratio for the first pane
 * @param maxRatio The maximum split ratio for the first pane
 * @param dividerWidth The width of the divider
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
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    var splitRatio by remember { mutableStateOf(initialSplitRatio.coerceIn(minRatio, maxRatio)) }

    Layout(
        modifier = modifier,
        content = {
            Box(modifier = Modifier.fillMaxHeight()) {
                first()
            }

            Box(
                modifier =
                    Modifier
                        .width(dividerWidth.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val totalWidth = size.width
                                val delta = dragAmount.x / totalWidth
                                splitRatio = (splitRatio + delta).coerceIn(minRatio, maxRatio)
                            }
                        }
            )

            Box(modifier = Modifier.fillMaxHeight()) {
                second()
            }
        }
    ) { measurables, constraints ->
        val dividerWidthPx = dividerWidth.dp.roundToPx()
        val availableWidth = constraints.maxWidth - dividerWidthPx

        val firstWidth = (availableWidth * splitRatio).roundToInt()
        val secondWidth = availableWidth - firstWidth

        val firstPlaceable =
            measurables[0].measure(
                Constraints.fixed(firstWidth, constraints.maxHeight)
            )

        val dividerPlaceable =
            measurables[1].measure(
                Constraints.fixed(dividerWidthPx, constraints.maxHeight)
            )

        val secondPlaceable =
            measurables[2].measure(
                Constraints.fixed(secondWidth, constraints.maxHeight)
            )

        layout(constraints.maxWidth, constraints.maxHeight) {
            firstPlaceable.placeRelative(0, 0)
            dividerPlaceable.placeRelative(firstWidth, 0)
            secondPlaceable.placeRelative(firstWidth + dividerWidthPx, 0)
        }
    }
}
