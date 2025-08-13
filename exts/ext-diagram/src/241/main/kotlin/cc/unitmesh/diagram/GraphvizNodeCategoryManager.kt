package cc.unitmesh.diagram

import com.intellij.diagram.AbstractDiagramNodeContentManager
import com.intellij.diagram.DiagramBuilder
import com.intellij.diagram.DiagramCategory
import com.intellij.util.PlatformIcons
import cc.unitmesh.diagram.model.GraphvizNodeField
import cc.unitmesh.diagram.model.GraphvizAttributeItem
import com.intellij.icons.AllIcons

/**
 * Node content manager for Graphviz diagrams
 * Similar to JdlUmlCategoryManager in JHipster UML implementation
 */
class GraphvizNodeCategoryManager : AbstractDiagramNodeContentManager() {
    
    companion object Companion {
        private val FIELDS_CATEGORY = DiagramCategory(
            "Fields",
            AllIcons.Nodes.Field,
            true,
            false
        )

        private val ATTRIBUTES_CATEGORY = DiagramCategory(
            "Attributes",
            AllIcons.Nodes.Method,
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
        return when (item) {
            is GraphvizNodeField -> category == FIELDS_CATEGORY
            is GraphvizAttributeItem -> category == ATTRIBUTES_CATEGORY
            else -> super.isInCategory(nodeElement, item, category, builder)
        }
    }
}
