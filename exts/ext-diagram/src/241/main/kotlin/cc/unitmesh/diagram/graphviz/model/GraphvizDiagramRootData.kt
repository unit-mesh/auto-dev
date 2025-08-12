package cc.unitmesh.diagram.graphviz.model

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.pointers.VirtualFilePointer
import com.intellij.util.PlatformIcons
import javax.swing.Icon

/**
 * Root data for Graphviz diagram, representing a DOT file
 * Similar to JdlDiagramRootData in JHipster UML implementation
 */
class GraphvizDiagramRootData(
    private val virtualFilePointer: VirtualFilePointer
) : GraphvizNodeData {
    
    private val name: String = virtualFilePointer.fileName ?: "Unknown"
    
    override fun getName(): String = name
    
    override fun getIcon(): Icon = PlatformIcons.FILE_ICON

    fun getVirtualFile(): VirtualFile? = virtualFilePointer.file
}
