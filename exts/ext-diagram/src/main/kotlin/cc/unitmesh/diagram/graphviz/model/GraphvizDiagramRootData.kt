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
    
    /**
     * Get the virtual file associated with this root data
     */
    fun getVirtualFile(): VirtualFile? = virtualFilePointer.file
    
    /**
     * Get the file pointer
     */
    fun getFilePointer(): VirtualFilePointer = virtualFilePointer
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GraphvizDiagramRootData) return false
        return virtualFilePointer == other.virtualFilePointer
    }
    
    override fun hashCode(): Int {
        return virtualFilePointer.hashCode()
    }
    
    override fun toString(): String {
        return "GraphvizDiagramRootData(name='$name')"
    }
}
