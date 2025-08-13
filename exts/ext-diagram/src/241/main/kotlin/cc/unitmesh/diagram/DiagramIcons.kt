package cc.unitmesh.diagram

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object DiagramIcons {
    @JvmField
    val DIAGRAM_ADD: Icon = IconLoader.getIcon("/icons/add.svg", DiagramIcons::class.java)

    @JvmField
    val DIAGRAM_REMOVE: Icon = IconLoader.getIcon("/icons/remote.svg", DiagramIcons::class.java)
}
