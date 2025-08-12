package cc.unitmesh.diagram

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service

/**
 * Service for managing Graphviz diagram resources
 * Similar to JdlDiagramService in JHipster UML implementation
 */
@Service(Service.Level.PROJECT)
class GraphvizDiagramService : Disposable {
    
    override fun dispose() {
        // Clean up any resources if needed
    }
}
