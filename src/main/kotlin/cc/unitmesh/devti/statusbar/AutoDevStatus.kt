package cc.unitmesh.devti.statusbar

import cc.unitmesh.devti.AutoDevIcons
import javax.swing.Icon

enum class AutoDevStatus {
    WAITING,
    Ready,
    InProgress,
    Error,
    Done;

    val icon: Icon
        get() {
            return when (this) {
                WAITING -> AutoDevIcons.DARK
                Ready -> AutoDevIcons.AI_COPILOT
                InProgress -> AutoDevIcons.IntProgress
                Error -> AutoDevIcons.ERROR
                Done -> AutoDevIcons.AI_COPILOT
            }
        }
}
