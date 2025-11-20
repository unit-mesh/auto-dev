package cc.unitmesh.devins.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

/**
 * 窗口控制按钮（macOS 风格）
 * 
 * 三个圆形按钮：关闭（红）、最小化（黄）、最大化（绿）
 */
@Composable
fun WindowControls(
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    style: WindowControlsStyle = WindowControlsStyle.MACOS
) {
    val isDark = isSystemInDarkTheme()
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (style) {
            WindowControlsStyle.MACOS -> {
                // macOS 风格：红黄绿圆点（根据主题调整颜色）
                MacOSControlButton(
                    color = if (isDark) AutoDevColors.Red.c400 else AutoDevColors.Red.c600,
                    onClick = onClose,
                    icon = AutoDevComposeIcons.Close
                )
                MacOSControlButton(
                    color = if (isDark) AutoDevColors.Amber.c300 else AutoDevColors.Amber.c500,
                    onClick = onMinimize,
                    icon = AutoDevComposeIcons.Remove
                )
                MacOSControlButton(
                    color = if (isDark) AutoDevColors.Green.c400 else AutoDevColors.Green.c600,
                    onClick = onMaximize,
                    icon = AutoDevComposeIcons.Fullscreen
                )
            }
            WindowControlsStyle.WINDOWS -> {
                // Windows 风格：图标按钮
                WindowsControlButton(
                    onClick = onMinimize,
                    icon = AutoDevComposeIcons.Remove
                )
                WindowsControlButton(
                    onClick = onMaximize,
                    icon = AutoDevComposeIcons.Fullscreen
                )
                WindowsControlButton(
                    onClick = onClose,
                    icon = AutoDevComposeIcons.Close,
                    isClose = true
                )
            }
        }
    }
}

/**
 * macOS 风格的控制按钮（圆点）
 */
@Composable
private fun MacOSControlButton(
    color: Color,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isDark = isSystemInDarkTheme()
    
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
            .hoverable(interactionSource)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Hover 时显示图标
        if (isHovered) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(8.dp),
                tint = if (isDark) {
                    Color.Black.copy(alpha = 0.7f)
                } else {
                    Color.Black.copy(alpha = 0.6f)
                }
            )
        }
    }
}

/**
 * Windows 风格的控制按钮
 */
@Composable
private fun WindowsControlButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isClose: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isDark = isSystemInDarkTheme()
    
    Box(
        modifier = modifier
            .size(32.dp, 24.dp)
            .background(
                if (isHovered) {
                    if (isClose) {
                        if (isDark) AutoDevColors.Red.c600 else AutoDevColors.Red.c700
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                } else {
                    Color.Transparent
                }
            )
            .hoverable(interactionSource)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = if (isHovered && isClose) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

enum class WindowControlsStyle {
    MACOS,
    WINDOWS
}

