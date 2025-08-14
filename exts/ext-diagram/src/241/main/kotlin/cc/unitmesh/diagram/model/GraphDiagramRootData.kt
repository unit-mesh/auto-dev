package cc.unitmesh.diagram.model

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.pointers.VirtualFilePointer
import com.intellij.util.PlatformIcons
import javax.swing.Icon

/**
 * Root data for Graphviz diagram, representing a DOT file
 * Similar to JdlDiagramRootData in JHipster UML implementation
 */
class GraphDiagramRootData(
    private val virtualFilePointer: VirtualFilePointer
) : GraphNodeData {
    
    private val name: String = virtualFilePointer.fileName ?: "Unknown"
    
    override fun getName(): String = name
    
    override fun getIcon(): Icon = PlatformIcons.FILE_ICON

    fun getVirtualFile(): VirtualFile? = virtualFilePointer.file
}
