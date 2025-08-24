package cc.unitmesh.diagram.diagram

import cc.unitmesh.diagram.CodeDiagramIcons
import com.intellij.diagram.AbstractDiagramNodeContentManager
import com.intellij.diagram.DiagramBuilder
import com.intellij.diagram.DiagramCategory
import cc.unitmesh.diagram.model.GraphNodeField
import cc.unitmesh.diagram.model.GraphAttributeItem
import com.intellij.icons.AllIcons

/**
 * Node content manager for Graphviz diagrams
 * Similar to JdlUmlCategoryManager in JHipster UML implementation
 */
class CodeTopologyNodeCategoryManager : AbstractDiagramNodeContentManager() {
    companion object Companion {
        private val FIELDS_CATEGORY = DiagramCategory(
            "Fields",
            AllIcons.Nodes.Field,
            true,
            false
        )

        private val METHODS_CATEGORY = DiagramCategory(
            "Methods",
            AllIcons.Nodes.Method,
            true,
            false
        )

        private val ATTRIBUTES_CATEGORY = DiagramCategory(
            "Attributes",
            AllIcons.Nodes.Property,
            true,
            false
        )

        private val ADDED_FIELDS_CATEGORY = DiagramCategory(
            "Added Fields",
            CodeDiagramIcons.DIAGRAM_ADD,
            true,
            false
        )

        private val REMOVED_FIELDS_CATEGORY = DiagramCategory(
            "Removed Fields",
            CodeDiagramIcons.DIAGRAM_REMOVE,
            true,
            false
        )

        private val ADDED_METHODS_CATEGORY = DiagramCategory(
            "Added Methods",
            CodeDiagramIcons.DIAGRAM_ADD,
            true,
            false
        )

        private val REMOVED_METHODS_CATEGORY = DiagramCategory(
            "Removed Methods",
            CodeDiagramIcons.DIAGRAM_REMOVE,
            true,
            false
        )
    }
    
    override fun getContentCategories(): Array<DiagramCategory> {
        return arrayOf(
            FIELDS_CATEGORY,
            METHODS_CATEGORY,
            ATTRIBUTES_CATEGORY,
            ADDED_FIELDS_CATEGORY,
            REMOVED_FIELDS_CATEGORY,
            ADDED_METHODS_CATEGORY,
            REMOVED_METHODS_CATEGORY
        )
    }
    
    override fun isInCategory(
        nodeElement: Any?,
        item: Any?,
        category: DiagramCategory,
        builder: DiagramBuilder?
    ): Boolean {
        return when (item) {
            is GraphNodeField -> {
                when (category) {
                    FIELDS_CATEGORY -> !item.isMethod() && item.isUnchanged()
                    METHODS_CATEGORY -> item.isMethod() && item.isUnchanged()
                    ADDED_FIELDS_CATEGORY -> !item.isMethod() && item.isAdded()
                    REMOVED_FIELDS_CATEGORY -> !item.isMethod() && item.isRemoved()
                    ADDED_METHODS_CATEGORY -> item.isMethod() && item.isAdded()
                    REMOVED_METHODS_CATEGORY -> item.isMethod() && item.isRemoved()
                    else -> false
                }
            }
            is GraphAttributeItem -> category == ATTRIBUTES_CATEGORY
            else -> super.isInCategory(nodeElement, item, category, builder)
        }
    }
}
