package cc.unitmesh.diagram

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object CodeDiagramIcons {
    @JvmField
    val DIAGRAM_ADD: Icon = IconLoader.getIcon("/icons/add.svg", CodeDiagramIcons::class.java)

    @JvmField
    val DIAGRAM_REMOVE: Icon = IconLoader.getIcon("/icons/remote.svg", CodeDiagramIcons::class.java)
}
