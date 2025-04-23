package cc.unitmesh.devti

import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import java.awt.Color

object AutoDevColors {
    val COMPLETED_STATUS = JBColor(0x59A869, 0x59A869) // Green
    val FAILED_STATUS = JBColor(0xD94F4F, 0xD94F4F) // Red
    val IN_PROGRESS_STATUS = JBColor(0x3592C4, 0x3592C4) // Blue
    val TODO_STATUS = JBColor(0x808080, 0x808080) // Gray

    val USER_ROLE_BG = JBColor(Gray._240, Gray._10)

    // Text colors
    val COMPLETED_TEXT = JBColor(0x808080, 0x999999)
    val FAILED_TEXT = JBColor(0xD94F4F, 0xFF6B68)
    val IN_PROGRESS_TEXT = JBColor(0x3592C4, 0x589DF6)

    val SEPARATOR_BORDER = JBColor(0xE5E5E5, 0x323232)
    val LINK_COLOR = JBColor(0x3366CC, 0x589DF6)

    // Diff and UI specific colors
    // Color for diff new line highlighter (as an Int used in TextAttributes)
    val DIFF_NEW_LINE_COLOR_SHADOW = JBColor(0x3000FF00, 0x3000FF00)
    val DIFF_NEW_LINE_COLOR: Int = 0x3000FF00
    // Background color for deletion inlay
    val DELETION_INLAY_COLOR: JBColor = JBColor(0x30FF0000, 0x30FF0000)
    // Reject button background color
    val REJECT_BUTTON_COLOR: JBColor = JBColor(Color(255, 0, 0, 153), Color(255, 0, 0, 153))
    // Accept button background color
    val ACCEPT_BUTTON_COLOR: JBColor = JBColor(0x8AA653, 0x8AA653)
    
    // Additional colors extracted from SingleFileDiffSketch
    val FILE_HOVER_COLOR = JBColor(0x4A7EB3, 0x589DF6) // Blue color for hover state
    val ADD_LINE_COLOR = JBColor(0x00FF00, 0x00FF00)
    val REMOVE_LINE_COLOR = JBColor(0xFF0000, 0xFF0000)

    // Execution result colors
    val EXECUTION_SUCCESS_BACKGROUND = JBColor(Color(233, 255, 233), Color(0, 77, 0))
    val EXECUTION_ERROR_BACKGROUND = JBColor(Color(255, 233, 233), Color(77, 0, 0))
    val EXECUTION_SUCCESS_BORDER = JBColor(Color(0, 128, 0), Color(0, 100, 0))
    val EXECUTION_RUNNING_BORDER = JBColor(Color(0, 0, 255), Color(0, 0, 200))
    val EXECUTION_WARNING_BORDER = JBColor(Color(255, 165, 0), Color(200, 100, 0))
    val EXECUTION_ERROR_BORDER = JBColor(Color(128, 0, 0), Color(100, 0, 0))

    // Loading panel colors
    object LoadingPanel {
        // Background colors
        val BACKGROUND = JBColor(Color(245, 247, 250), Color(30, 32, 40))
        val FOREGROUND = JBColor(Gray._50, Gray._220)
        val BORDER = JBColor(Color(200, 210, 230), Color(60, 65, 80))

        // Progress bar colors
        val PROGRESS_COLOR = JBColor(Color(59, 130, 246), Color(59, 130, 246))
        val PROGRESS_BACKGROUND = JBColor(Color(229, 231, 235), Color(55, 65, 81))

        // Gradient colors
        val GRADIENT_COLOR1 = JBColor(Color(240, 245, 255, 200), Color(30, 40, 70, 200))
        val GRADIENT_COLOR2 = JBColor(Color(245, 240, 255, 200), Color(40, 30, 70, 200))
    }
}
