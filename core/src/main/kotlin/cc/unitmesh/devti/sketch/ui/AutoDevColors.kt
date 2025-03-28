package cc.unitmesh.devti.sketch.ui

import com.intellij.ui.JBColor

/**
 * Color constants used throughout the AutoDev UI
 */
object AutoDevColors {
    // Status colors
    val COMPLETED_STATUS = JBColor(0x59A869, 0x59A869) // Green
    val FAILED_STATUS = JBColor(0xD94F4F, 0xD94F4F) // Red
    val IN_PROGRESS_STATUS = JBColor(0x3592C4, 0x3592C4) // Blue
    val TODO_STATUS = JBColor(0x808080, 0x808080) // Gray

    // Text colors
    val COMPLETED_TEXT = JBColor(0x808080, 0x999999)
    val FAILED_TEXT = JBColor(0xD94F4F, 0xFF6B68)
    val IN_PROGRESS_TEXT = JBColor(0x3592C4, 0x589DF6)
    
    // UI elements
    val SEPARATOR_BORDER = JBColor(0xE5E5E5, 0x323232)
    val LINK_COLOR = JBColor(0x3366CC, 0x589DF6)
}
