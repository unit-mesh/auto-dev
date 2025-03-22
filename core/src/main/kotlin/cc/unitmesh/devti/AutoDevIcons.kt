package cc.unitmesh.devti

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.AnimatedIcon
import javax.swing.Icon

object AutoDevIcons {
    @JvmField
    val AI_COPILOT: Icon = IconLoader.getIcon("/icons/ai-copilot.svg", AutoDevIcons::class.java)

    @JvmField
    val DARK: Icon = IconLoader.getIcon("/icons/autodev-dark.svg", AutoDevIcons::class.java)

    @JvmField
    val ERROR: Icon = IconLoader.getIcon("/icons/autodev-error.svg", AutoDevIcons::class.java)

    @JvmField
    val COMMAND: Icon = IconLoader.getIcon("/icons/devins-command.svg", AutoDevIcons::class.java)

    @JvmField
    val InProgress = AnimatedIcon.Default()

    @JvmField
    val Send: Icon = IconLoader.getIcon("/icons/send.svg", AutoDevIcons::class.java)

    @JvmField
    val InsertCode: Icon = IconLoader.getIcon("/icons/insert-code.svg", AutoDevIcons::class.java)

    @JvmField
    val Run: Icon = IconLoader.getIcon("/icons/run.svg", AutoDevIcons::class.java)

    @JvmField
    val Copy: Icon = IconLoader.getIcon("/icons/copy.svg", AutoDevIcons::class.java)

    @JvmField
    val Idea: Icon = IconLoader.getIcon("/icons/idea.svg", AutoDevIcons::class.java)

    @JvmField
    val View: Icon = IconLoader.getIcon("/icons/view.svg", AutoDevIcons::class.java)

    @JvmField
    val Terminal: Icon = IconLoader.getIcon("/icons/terminal.svg", AutoDevIcons::class.java)

    @JvmField
    val Stop: Icon = IconLoader.getIcon("/icons/stop.svg", AutoDevIcons::class.java)

    @JvmField
    val TOOLCHAIN: Icon = IconLoader.getIcon("/icons/toolchain.svg", AutoDevIcons::class.java)

    @JvmField
    val REVIEWER: Icon = IconLoader.getIcon("/icons/reviewer.svg", AutoDevIcons::class.java)

    @JvmField
    val PLANNER: Icon = IconLoader.getIcon("/icons/planner.svg", AutoDevIcons::class.java)
}
