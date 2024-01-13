package cc.unitmesh.devti.statusbar

import cc.unitmesh.devti.AutoDevIcons
import javax.swing.Icon

enum class AutoDevStatus {
    Ready,
    InProgress,
    Error;

    val icon: Icon
        get() {
            return when (this) {
                Ready -> AutoDevIcons.DARK
                InProgress -> AutoDevIcons.IntProgress
                Error -> AutoDevIcons.ERROR
            }
        }
}
