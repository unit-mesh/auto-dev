package cc.unitmesh.diagram.graphviz

import com.intellij.diagram.AbstractDiagramNodeContentManager
import com.intellij.diagram.DiagramBuilder
import com.intellij.diagram.DiagramCategory
import com.intellij.util.PlatformIcons
import cc.unitmesh.diagram.graphviz.model.GraphvizNodeData
import cc.unitmesh.diagram.graphviz.model.GraphvizSimpleNodeData

/**
 * Node content manager for Graphviz diagrams
 * Similar to JdlUmlCategoryManager in JHipster UML implementation
 */
class GraphvizNodeContentManager : AbstractDiagramNodeContentManager() {
    
    companion object {
        private val ATTRIBUTES_CATEGORY = DiagramCategory(
            "Attributes",
            PlatformIcons.PROPERTY_ICON,
            true,
            true
        )
    }
    
    override fun getContentCategories(): Array<DiagramCategory> {
        return arrayOf(ATTRIBUTES_CATEGORY)
    }
    
    override fun isInCategory(
        nodeElement: Any?,
        item: Any?,
        category: DiagramCategory,
        builder: DiagramBuilder?
    ): Boolean {
        return category == ATTRIBUTES_CATEGORY && item is GraphvizAttributeItem
    }
}

/**
 * Represents a node attribute as a diagram item
 */
data class GraphvizAttributeItem(
    val key: String,
    val value: String
) {
    override fun toString(): String {
        return "$key = $value"
    }
}
