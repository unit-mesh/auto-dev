package cc.unitmesh.devins.idea

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Icon provider for AutoDev Compose module.
 * Icons are loaded from resources for use in toolbars, tool windows, etc.
 */
object AutoDevIcons {
    /**
     * Tool window icon (13x13 for tool window, 16x16 for actions)
     */
    @JvmField
    val ToolWindow: Icon = IconLoader.getIcon("/icons/autodev-toolwindow.svg", AutoDevIcons::class.java)

    /**
     * Main AutoDev icon
     */
    @JvmField
    val AutoDev: Icon = IconLoader.getIcon("/icons/autodev.svg", AutoDevIcons::class.java)
}

