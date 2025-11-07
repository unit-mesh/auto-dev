package cc.unitmesh.devins.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom icons for AutoDev
 */
object CustomIcons {
    
    /**
     * AI Star icon (256x256)
     * A sparkle/star icon representing AI functionality
     */
    val AI: ImageVector by lazy {
        ImageVector.Builder(
            name = "AI",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            // Main star shape path
            path(
                fill = SolidColor(Color(0xFF6366F1)), // Indigo-500
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                // Outer star path (simplified from the SVG)
                moveTo(230.05859f, 112.96289f)
                lineTo(166.24316f, 89.75684f)
                lineTo(143.03716f, 25.94141f)
                arcToRelative(16.001f, 16.001f, 0f, isMoreThanHalf = false, isPositiveArc = false, -30.07422f, 0f)
                lineTo(89.75684f, 89.75684f)
                lineTo(25.94141f, 112.96289f)
                arcToRelative(16.001f, 16.001f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 30.07422f)
                lineTo(89.75684f, 166.24316f)
                lineTo(112.96289f, 230.05859f)
                arcToRelative(16.001f, 16.001f, 0f, isMoreThanHalf = false, isPositiveArc = false, 30.07422f, 0f)
                lineTo(166.24316f, 166.24316f)
                lineTo(230.05859f, 143.03711f)
                arcToRelative(16.001f, 16.001f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, -30.07422f)
                close()
                
                // Inner hollow part
                moveTo(160.77637f, 151.20605f)
                arcToRelative(15.95685f, 15.95685f, 0f, isMoreThanHalf = false, isPositiveArc = false, -9.57032f, 9.57032f)
                lineTo(127.99707f, 224.58399f)
                lineTo(104.794f, 160.77637f)
                arcToRelative(15.95872f, 15.95872f, 0f, isMoreThanHalf = false, isPositiveArc = false, -9.56836f, -9.57032f)
                lineTo(31.418f, 127.99707f)
                lineTo(95.22363f, 104.794f)
                arcToRelative(15.95872f, 15.95872f, 0f, isMoreThanHalf = false, isPositiveArc = false, 9.57032f, -9.56836f)
                lineTo(127.99707f, 31.41601f)
                lineTo(151.20019f, 95.22167f)
                arcToRelative(15.95872f, 15.95872f, 0f, isMoreThanHalf = false, isPositiveArc = false, 9.56836f, 9.57032f)
                lineTo(224.57781f, 127.99707f)
                close()
            }
        }.build()
    }
    
    /**
     * MCP (Model Context Protocol) icon (24x24)
     * Represents the Model Context Protocol integration
     */
    val MCP: ImageVector by lazy {
        ImageVector.Builder(
            name = "MCP",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // First path
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(15.688f, 2.343f)
                arcToRelative(2.588f, 2.588f, 0f, isMoreThanHalf = false, isPositiveArc = false, -3.61f, 0f)
                lineToRelative(-9.626f, 9.44f)
                arcToRelative(0.863f, 0.863f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1.203f, 0f)
                arcToRelative(0.823f, 0.823f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, -1.18f)
                lineToRelative(9.626f, -9.44f)
                arcToRelative(4.313f, 4.313f, 0f, isMoreThanHalf = false, isPositiveArc = true, 6.016f, 0f)
                arcToRelative(4.116f, 4.116f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1.204f, 3.54f)
                arcToRelative(4.3f, 4.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3.609f, 1.18f)
                lineToRelative(0.05f, 0.05f)
                arcToRelative(4.115f, 4.115f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, 5.9f)
                lineToRelative(-8.706f, 8.537f)
                arcToRelative(0.274f, 0.274f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 0.393f)
                lineToRelative(1.788f, 1.754f)
                arcToRelative(0.823f, 0.823f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, 1.18f)
                arcToRelative(0.863f, 0.863f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1.203f, 0f)
                lineToRelative(-1.788f, -1.753f)
                arcToRelative(1.92f, 1.92f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, -2.754f)
                lineToRelative(8.706f, -8.538f)
                arcToRelative(2.47f, 2.47f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, -3.54f)
                lineToRelative(-0.05f, -0.049f)
                arcToRelative(2.588f, 2.588f, 0f, isMoreThanHalf = false, isPositiveArc = false, -3.607f, -0.003f)
                lineToRelative(-7.172f, 7.034f)
                lineToRelative(-0.002f, 0.002f)
                lineToRelative(-0.098f, 0.097f)
                arcToRelative(0.863f, 0.863f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1.204f, 0f)
                arcToRelative(0.823f, 0.823f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, -1.18f)
                lineToRelative(7.273f, -7.133f)
                arcToRelative(2.47f, 2.47f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.003f, -3.537f)
                close()
            }
            
            // Second path
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(14.485f, 4.703f)
                arcToRelative(0.823f, 0.823f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, -1.18f)
                arcToRelative(0.863f, 0.863f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1.204f, 0f)
                lineToRelative(-7.119f, 6.982f)
                arcToRelative(4.115f, 4.115f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 5.9f)
                arcToRelative(4.314f, 4.314f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6.016f, 0f)
                lineToRelative(7.12f, -6.982f)
                arcToRelative(0.823f, 0.823f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, -1.18f)
                arcToRelative(0.863f, 0.863f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1.204f, 0f)
                lineToRelative(-7.119f, 6.982f)
                arcToRelative(2.588f, 2.588f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3.61f, 0f)
                arcToRelative(2.47f, 2.47f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, -3.54f)
                lineToRelative(7.12f, -6.982f)
                close()
            }
        }.build()
    }
}

