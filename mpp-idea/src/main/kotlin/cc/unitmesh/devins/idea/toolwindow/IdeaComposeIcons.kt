package cc.unitmesh.devins.idea.toolwindow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icon provider for IntelliJ IDEA Compose UI.
 * These icons are defined using ImageVector paths to avoid dependency on Material Icons
 * which is not available in IntelliJ's Compose environment.
 */
object IdeaComposeIcons {

    /**
     * Settings icon (gear/cog)
     */
    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Gear icon path
                moveTo(19.14f, 12.94f)
                curveToRelative(0.04f, -0.31f, 0.06f, -0.63f, 0.06f, -0.94f)
                curveToRelative(0f, -0.32f, -0.02f, -0.64f, -0.07f, -0.94f)
                lineToRelative(2.03f, -1.58f)
                curveToRelative(0.18f, -0.14f, 0.23f, -0.41f, 0.12f, -0.61f)
                lineToRelative(-1.92f, -3.32f)
                curveToRelative(-0.12f, -0.22f, -0.37f, -0.29f, -0.59f, -0.22f)
                lineToRelative(-2.39f, 0.96f)
                curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
                lineToRelative(-0.36f, -2.54f)
                curveToRelative(-0.04f, -0.24f, -0.24f, -0.41f, -0.48f, -0.41f)
                horizontalLineToRelative(-3.84f)
                curveToRelative(-0.24f, 0f, -0.43f, 0.17f, -0.47f, 0.41f)
                lineToRelative(-0.36f, 2.54f)
                curveToRelative(-0.59f, 0.24f, -1.13f, 0.56f, -1.62f, 0.94f)
                lineToRelative(-2.39f, -0.96f)
                curveToRelative(-0.22f, -0.08f, -0.47f, 0f, -0.59f, 0.22f)
                lineTo(2.74f, 8.87f)
                curveToRelative(-0.12f, 0.21f, -0.08f, 0.47f, 0.12f, 0.61f)
                lineToRelative(2.03f, 1.58f)
                curveToRelative(-0.05f, 0.3f, -0.09f, 0.63f, -0.09f, 0.94f)
                curveToRelative(0f, 0.31f, 0.02f, 0.64f, 0.07f, 0.94f)
                lineToRelative(-2.03f, 1.58f)
                curveToRelative(-0.18f, 0.14f, -0.23f, 0.41f, -0.12f, 0.61f)
                lineToRelative(1.92f, 3.32f)
                curveToRelative(0.12f, 0.22f, 0.37f, 0.29f, 0.59f, 0.22f)
                lineToRelative(2.39f, -0.96f)
                curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
                lineToRelative(0.36f, 2.54f)
                curveToRelative(0.05f, 0.24f, 0.24f, 0.41f, 0.48f, 0.41f)
                horizontalLineToRelative(3.84f)
                curveToRelative(0.24f, 0f, 0.44f, -0.17f, 0.47f, -0.41f)
                lineToRelative(0.36f, -2.54f)
                curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
                lineToRelative(2.39f, 0.96f)
                curveToRelative(0.22f, 0.08f, 0.47f, 0f, 0.59f, -0.22f)
                lineToRelative(1.92f, -3.32f)
                curveToRelative(0.12f, -0.22f, 0.07f, -0.47f, -0.12f, -0.61f)
                lineToRelative(-2.01f, -1.58f)
                close()
                moveTo(12f, 15.6f)
                curveToRelative(-1.98f, 0f, -3.6f, -1.62f, -3.6f, -3.6f)
                reflectiveCurveToRelative(1.62f, -3.6f, 3.6f, -3.6f)
                reflectiveCurveToRelative(3.6f, 1.62f, 3.6f, 3.6f)
                reflectiveCurveToRelative(-1.62f, 3.6f, -3.6f, 3.6f)
                close()
            }
        }.build()
    }

    /**
     * Build/Tool icon (wrench)
     */
    val Build: ImageVector by lazy {
        ImageVector.Builder(
            name = "Build",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(22.7f, 19f)
                lineToRelative(-9.1f, -9.1f)
                curveToRelative(0.9f, -2.3f, 0.4f, -5f, -1.5f, -6.9f)
                curveToRelative(-2f, -2f, -5f, -2.4f, -7.4f, -1.3f)
                lineTo(9f, 6f)
                lineTo(6f, 9f)
                lineTo(1.6f, 4.7f)
                curveTo(0.4f, 7.1f, 0.9f, 10.1f, 2.9f, 12.1f)
                curveToRelative(1.9f, 1.9f, 4.6f, 2.4f, 6.9f, 1.5f)
                lineToRelative(9.1f, 9.1f)
                curveToRelative(0.4f, 0.4f, 1f, 0.4f, 1.4f, 0f)
                lineToRelative(2.3f, -2.3f)
                curveToRelative(0.5f, -0.4f, 0.5f, -1.1f, 0.1f, -1.4f)
                close()
            }
        }.build()
    }

    /**
     * Error icon (circle with X)
     */
    val Error: ImageVector by lazy {
        ImageVector.Builder(
            name = "Error",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(13f, 17f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
                moveTo(13f, 13f)
                horizontalLineToRelative(-2f)
                lineTo(11f, 7f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(6f)
                close()
            }
        }.build()
    }

    /**
     * CheckCircle icon (circle with checkmark)
     */
    val CheckCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "CheckCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(10f, 17f)
                lineToRelative(-5f, -5f)
                lineToRelative(1.41f, -1.41f)
                lineTo(10f, 14.17f)
                lineToRelative(7.59f, -7.59f)
                lineTo(19f, 8f)
                lineToRelative(-9f, 9f)
                close()
            }
        }.build()
    }

    /**
     * Refresh icon (circular arrow)
     */
    val Refresh: ImageVector by lazy {
        ImageVector.Builder(
            name = "Refresh",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(17.65f, 6.35f)
                curveTo(16.2f, 4.9f, 14.21f, 4f, 12f, 4f)
                curveToRelative(-4.42f, 0f, -7.99f, 3.58f, -7.99f, 8f)
                reflectiveCurveToRelative(3.57f, 8f, 7.99f, 8f)
                curveToRelative(3.73f, 0f, 6.84f, -2.55f, 7.73f, -6f)
                horizontalLineToRelative(-2.08f)
                curveToRelative(-0.82f, 2.33f, -3.04f, 4f, -5.65f, 4f)
                curveToRelative(-3.31f, 0f, -6f, -2.69f, -6f, -6f)
                reflectiveCurveToRelative(2.69f, -6f, 6f, -6f)
                curveToRelative(1.66f, 0f, 3.14f, 0.69f, 4.22f, 1.78f)
                lineTo(13f, 11f)
                horizontalLineToRelative(7f)
                lineTo(20f, 4f)
                lineToRelative(-2.35f, 2.35f)
                close()
            }
        }.build()
    }

    /**
     * Description icon (document)
     */
    val Description: ImageVector by lazy {
        ImageVector.Builder(
            name = "Description",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(14f, 2f)
                lineTo(6f, 2f)
                curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                lineTo(4f, 20f)
                curveToRelative(0f, 1.1f, 0.89f, 2f, 1.99f, 2f)
                lineTo(18f, 22f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                lineTo(20f, 8f)
                lineToRelative(-6f, -6f)
                close()
                moveTo(16f, 18f)
                lineTo(8f, 18f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(2f)
                close()
                moveTo(16f, 14f)
                lineTo(8f, 14f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(2f)
                close()
                moveTo(13f, 9f)
                lineTo(13f, 3.5f)
                lineTo(18.5f, 9f)
                lineTo(13f, 9f)
                close()
            }
        }.build()
    }

    /**
     * Code icon (angle brackets)
     */
    val Code: ImageVector by lazy {
        ImageVector.Builder(
            name = "Code",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(9.4f, 16.6f)
                lineTo(4.8f, 12f)
                lineToRelative(4.6f, -4.6f)
                lineTo(8f, 6f)
                lineToRelative(-6f, 6f)
                lineToRelative(6f, 6f)
                lineToRelative(1.4f, -1.4f)
                close()
                moveTo(14.6f, 16.6f)
                lineToRelative(4.6f, -4.6f)
                lineToRelative(-4.6f, -4.6f)
                lineTo(16f, 6f)
                lineToRelative(6f, 6f)
                lineToRelative(-6f, 6f)
                lineToRelative(-1.4f, -1.4f)
                close()
            }
        }.build()
    }

    /**
     * Delete icon (trash can)
     */
    val Delete: ImageVector by lazy {
        ImageVector.Builder(
            name = "Delete",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(6f, 19f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(8f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                lineTo(18f, 7f)
                lineTo(6f, 7f)
                verticalLineToRelative(12f)
                close()
                moveTo(19f, 4f)
                horizontalLineToRelative(-3.5f)
                lineToRelative(-1f, -1f)
                horizontalLineToRelative(-5f)
                lineToRelative(-1f, 1f)
                lineTo(5f, 4f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(14f)
                lineTo(19f, 4f)
                close()
            }
        }.build()
    }

    /**
     * PlayArrow icon (triangle pointing right)
     */
    val PlayArrow: ImageVector by lazy {
        ImageVector.Builder(
            name = "PlayArrow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(8f, 5f)
                verticalLineToRelative(14f)
                lineToRelative(11f, -7f)
                close()
            }
        }.build()
    }

    /**
     * ExpandLess icon (chevron up)
     */
    val ExpandLess: ImageVector by lazy {
        ImageVector.Builder(
            name = "ExpandLess",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(12f, 8f)
                lineToRelative(-6f, 6f)
                lineToRelative(1.41f, 1.41f)
                lineTo(12f, 10.83f)
                lineToRelative(4.59f, 4.58f)
                lineTo(18f, 14f)
                close()
            }
        }.build()
    }

    /**
     * ExpandMore icon (chevron down)
     */
    val ExpandMore: ImageVector by lazy {
        ImageVector.Builder(
            name = "ExpandMore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(16.59f, 8.59f)
                lineTo(12f, 13.17f)
                lineTo(7.41f, 8.59f)
                lineTo(6f, 10f)
                lineToRelative(6f, 6f)
                lineToRelative(6f, -6f)
                close()
            }
        }.build()
    }

    /**
     * ContentCopy icon (two overlapping rectangles)
     */
    val ContentCopy: ImageVector by lazy {
        ImageVector.Builder(
            name = "ContentCopy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(16f, 1f)
                lineTo(4f, 1f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                horizontalLineToRelative(2f)
                lineTo(4f, 3f)
                horizontalLineToRelative(12f)
                lineTo(16f, 1f)
                close()
                moveTo(19f, 5f)
                lineTo(8f, 5f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(11f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                lineTo(21f, 7f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(19f, 21f)
                lineTo(8f, 21f)
                lineTo(8f, 7f)
                horizontalLineToRelative(11f)
                verticalLineToRelative(14f)
                close()
            }
        }.build()
    }

    /**
     * Terminal icon (command prompt)
     */
    val Terminal: ImageVector by lazy {
        ImageVector.Builder(
            name = "Terminal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(20f, 4f)
                lineTo(4f, 4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(16f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                lineTo(22f, 6f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(20f, 18f)
                lineTo(4f, 18f)
                lineTo(4f, 8f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(10f)
                close()
                // Terminal prompt arrow
                moveTo(5.5f, 11.5f)
                lineToRelative(3f, 2.5f)
                lineToRelative(-3f, 2.5f)
                close()
                // Cursor line
                moveTo(10f, 15f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(1.5f)
                horizontalLineToRelative(-8f)
                close()
            }
        }.build()
    }

    /**
     * Stop icon (square)
     */
    val Stop: ImageVector by lazy {
        ImageVector.Builder(
            name = "Stop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(6f, 6f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(12f)
                lineTo(6f, 18f)
                close()
            }
        }.build()
    }

    /**
     * Send icon (paper plane / arrow pointing right)
     */
    val Send: ImageVector by lazy {
        ImageVector.Builder(
            name = "Send",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(2.01f, 21f)
                lineTo(23f, 12f)
                lineTo(2.01f, 3f)
                lineTo(2f, 10f)
                lineToRelative(15f, 2f)
                lineToRelative(-15f, 2f)
                close()
            }
        }.build()
    }

    /**
     * Folder icon
     */
    val Folder: ImageVector by lazy {
        ImageVector.Builder(
            name = "Folder",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(10f, 4f)
                lineTo(12f, 6f)
                lineTo(20f, 6f)
                curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
                verticalLineToRelative(10f)
                curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
                lineTo(4f, 20f)
                curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
                lineTo(2f, 6f)
                curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
                horizontalLineToRelative(6f)
                close()
            }
        }.build()
    }

    /**
     * AlternateEmail icon (@ symbol)
     */
    val AlternateEmail: ImageVector by lazy {
        ImageVector.Builder(
            name = "AlternateEmail",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                horizontalLineToRelative(5f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(-5f)
                curveToRelative(-4.34f, 0f, -8f, -3.66f, -8f, -8f)
                reflectiveCurveToRelative(3.66f, -8f, 8f, -8f)
                reflectiveCurveToRelative(8f, 3.66f, 8f, 8f)
                verticalLineToRelative(1.43f)
                curveToRelative(0f, 0.79f, -0.71f, 1.57f, -1.5f, 1.57f)
                reflectiveCurveToRelative(-1.5f, -0.78f, -1.5f, -1.57f)
                lineTo(17f, 12f)
                curveToRelative(0f, -2.76f, -2.24f, -5f, -5f, -5f)
                reflectiveCurveToRelative(-5f, 2.24f, -5f, 5f)
                reflectiveCurveToRelative(2.24f, 5f, 5f, 5f)
                curveToRelative(1.38f, 0f, 2.64f, -0.56f, 3.54f, -1.47f)
                curveToRelative(0.65f, 0.89f, 1.77f, 1.47f, 2.96f, 1.47f)
                curveToRelative(1.97f, 0f, 3.5f, -1.6f, 3.5f, -3.57f)
                lineTo(22f, 12f)
                curveToRelative(0f, -5.52f, -4.48f, -10f, -10f, -10f)
                close()
                moveTo(12f, 15f)
                curveToRelative(-1.66f, 0f, -3f, -1.34f, -3f, -3f)
                reflectiveCurveToRelative(1.34f, -3f, 3f, -3f)
                reflectiveCurveToRelative(3f, 1.34f, 3f, 3f)
                reflectiveCurveToRelative(-1.34f, 3f, -3f, 3f)
                close()
            }
        }.build()
    }

    /**
     * ArrowDropDown icon (dropdown arrow)
     */
    val ArrowDropDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowDropDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(7f, 10f)
                lineToRelative(5f, 5f)
                lineToRelative(5f, -5f)
                close()
            }
        }.build()
    }
}

