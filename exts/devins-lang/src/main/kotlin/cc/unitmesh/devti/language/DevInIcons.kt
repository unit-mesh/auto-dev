package cc.unitmesh.devti.language

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object DevInIcons {
    @JvmField
    val DEFAULT: Icon = IconLoader.getIcon("/icons/devin.svg", DevInIcons::class.java)
    @JvmField
    val COMMAND: Icon = IconLoader.getIcon("/icons/devins-command.svg", DevInIcons::class.java)
    @JvmField
    val Terminal: Icon = IconLoader.getIcon("/icons/terminal.svg", DevInIcons::class.java)
}
