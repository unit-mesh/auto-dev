package cc.unitmesh.devins.ui.compose.editor.completion

import androidx.compose.ui.graphics.vector.ImageVector
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons

/**
 * Maps icon names from completion items to actual Material Icons
 * This is used to replace emojis with proper icons for WASM compatibility
 */
object CompletionIconMapper {
    /**
     * Get Material Icon for a completion item icon name
     * Returns null if no mapping is found
     */
    fun getIcon(iconName: String?): ImageVector? {
        return when (iconName) {
            "help" -> AutoDevComposeIcons.Help
            "clear" -> AutoDevComposeIcons.Delete
            "exit" -> AutoDevComposeIcons.ExitToApp
            "config" -> AutoDevComposeIcons.Settings
            "model" -> AutoDevComposeIcons.SmartToy
            "init" -> AutoDevComposeIcons.RocketLaunch
            else -> null
        }
    }
}
