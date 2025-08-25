package cc.unitmesh.diagram.diagram

import com.intellij.diagram.DiagramVfsResolver
import com.intellij.openapi.project.Project
import cc.unitmesh.diagram.model.GraphNodeData

/**
 * VFS resolver for Graphviz diagrams
 * Similar to JdlUmlVfsResolver in JHipster UML implementation
 */
class CodeTopologyVfsResolver : DiagramVfsResolver<GraphNodeData> {
    
    override fun getQualifiedName(data: GraphNodeData?): String? = data?.getName()
    
    override fun resolveElementByFQN(fqn: String, project: Project): GraphNodeData? {
        // For now, we don't support resolving elements by FQN
        // This could be implemented to support navigation and cross-references
        return null
    }
}
