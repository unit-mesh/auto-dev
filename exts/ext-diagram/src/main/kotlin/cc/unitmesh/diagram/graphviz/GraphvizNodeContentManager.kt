package cc.unitmesh.diagram.graphviz

import com.intellij.diagram.AbstractDiagramNodeContentManager
import com.intellij.diagram.DiagramBuilder
import com.intellij.diagram.DiagramCategory
import com.intellij.util.PlatformIcons
import cc.unitmesh.diagram.graphviz.model.GraphvizNodeData
import cc.unitmesh.diagram.graphviz.model.GraphvizSimpleNodeData
import cc.unitmesh.diagram.graphviz.model.GraphvizEntityNodeData
import cc.unitmesh.diagram.graphviz.model.GraphvizNodeField
import cc.unitmesh.diagram.graphviz.model.GraphvizAttributeItem
import com.intellij.icons.AllIcons

/**
 * Node content manager for Graphviz diagrams
 * Similar to JdlUmlCategoryManager in JHipster UML implementation
 */
class GraphvizNodeContentManager : AbstractDiagramNodeContentManager() {
    
    companion object {
        private val FIELDS_CATEGORY = DiagramCategory(
            "Fields",
            AllIcons.Nodes.Field,
            true,
            false
        )

        private val ATTRIBUTES_CATEGORY = DiagramCategory(
            "Attributes",
            PlatformIcons.PROPERTY_ICON,
            true,
            false
        )
    }
    
    override fun getContentCategories(): Array<DiagramCategory> {
        return arrayOf(FIELDS_CATEGORY, ATTRIBUTES_CATEGORY)
    }
    
    override fun isInCategory(
        nodeElement: Any?,
        item: Any?,
        category: DiagramCategory,
        builder: DiagramBuilder?
    ): Boolean {
        return when {
            item is GraphvizNodeField -> category == FIELDS_CATEGORY
            item is GraphvizAttributeItem -> category == ATTRIBUTES_CATEGORY
            else -> super.isInCategory(nodeElement, item, category, builder)
        }
    }
}
