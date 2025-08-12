package cc.unitmesh.diagram

import com.intellij.diagram.DiagramVfsResolver
import com.intellij.openapi.project.Project
import cc.unitmesh.diagram.graphviz.model.GraphvizNodeData

/**
 * VFS resolver for Graphviz diagrams
 * Similar to JdlUmlVfsResolver in JHipster UML implementation
 */
class GraphvizVfsResolver : DiagramVfsResolver<GraphvizNodeData> {
    
    override fun getQualifiedName(data: GraphvizNodeData?): String? {
        if (data == null) return null
        
        val name = data.getName()
        return if (name.isNotEmpty()) name else null
    }
    
    override fun resolveElementByFQN(fqn: String, project: Project): GraphvizNodeData? {
        // For now, we don't support resolving elements by FQN
        // This could be implemented to support navigation and cross-references
        return null
    }
}
