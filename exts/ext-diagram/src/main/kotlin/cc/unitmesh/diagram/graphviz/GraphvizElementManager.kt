package cc.unitmesh.diagram.graphviz

import com.intellij.diagram.AbstractDiagramElementManager
import com.intellij.diagram.DiagramBuilder
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager
import com.intellij.psi.PsiManager
import com.intellij.ui.SimpleColoredText
import com.intellij.ui.SimpleTextAttributes
import cc.unitmesh.diagram.graphviz.model.GraphvizNodeData
import cc.unitmesh.diagram.graphviz.model.GraphvizDiagramRootData
import cc.unitmesh.diagram.graphviz.model.GraphvizSimpleNodeData
import cc.unitmesh.diagram.graphviz.model.GraphvizEntityNodeData
import cc.unitmesh.diagram.graphviz.model.GraphvizNodeField
import cc.unitmesh.diagram.graphviz.model.GraphvizAttributeItem
import com.intellij.util.ArrayUtil
import javax.swing.Icon

/**
 * Element manager for Graphviz diagrams
 * Similar to JdlUmlElementManager in JHipster UML implementation
 */
class GraphvizElementManager : AbstractDiagramElementManager<GraphvizNodeData>() {
    override fun findInDataContext(dataContext: DataContext): GraphvizNodeData? {
        val psiFile = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return null
        val virtualFile = psiFile.virtualFile ?: return null
        if (!isDotFile(virtualFile)) return null
        return getRootData(psiFile.project, virtualFile)
    }
    
    override fun isAcceptableAsNode(element: Any?): Boolean {
        return element is GraphvizEntityNodeData || element is GraphvizSimpleNodeData
    }

    /**
     * Get node items (fields for entities, attributes for simple nodes)
     */
    override fun getNodeItems(nodeElement: GraphvizNodeData): Array<Any> {
        return when (nodeElement) {
            is GraphvizEntityNodeData -> nodeElement.getFields().toTypedArray()
            is GraphvizSimpleNodeData -> {
                // Convert attributes to GraphvizAttributeItem objects
                nodeElement.getAttributes().map { (key, value) ->
                    GraphvizAttributeItem(key, value)
                }.toTypedArray()
            }
            else -> ArrayUtil.EMPTY_OBJECT_ARRAY
        }
    }
    
    override fun getElementTitle(element: GraphvizNodeData): String? {
        return element.getName()
    }
    
    override fun getNodeTooltip(element: GraphvizNodeData): String? {
        return when (element) {
            is GraphvizEntityNodeData -> {
                buildString {
                    append("Entity: ${element.getName()}")
                    append("\nFields: ${element.getFields().size}")
                    if (element.hasFields()) {
                        append("\n")
                        element.getFields().take(3).forEach { field ->
                            append("\n  ${field.name}")
                            field.type?.let { append(": $it") }
                        }
                        if (element.getFields().size > 3) {
                            append("\n  ...")
                        }
                    }
                }
            }
            is GraphvizSimpleNodeData -> {
                buildString {
                    append("Node: ${element.getName()}")
                    if (element.getDisplayLabel() != element.getName()) {
                        append("\nLabel: ${element.getDisplayLabel()}")
                    }
                    append("\nShape: ${element.getShape()}")
                    element.getColor()?.let { append("\nColor: $it") }
                    element.getStyle()?.let { append("\nStyle: $it") }
                }
            }
            else -> element.getName()
        }
    }
    
    override fun getItemName(
        nodeElement: GraphvizNodeData?,
        nodeItem: Any?,
        builder: DiagramBuilder
    ): SimpleColoredText? {
        return when (nodeItem) {
            is GraphvizNodeField -> SimpleColoredText(nodeItem.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            is GraphvizAttributeItem -> SimpleColoredText(nodeItem.key, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            else -> null
        }
    }

    override fun getItemType(
        nodeElement: GraphvizNodeData?,
        nodeItem: Any?,
        builder: DiagramBuilder?
    ): SimpleColoredText? {
        return when (nodeItem) {
            is GraphvizNodeField -> {
                nodeItem.type?.let {
                    SimpleColoredText(it, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }
            is GraphvizAttributeItem -> SimpleColoredText(nodeItem.value, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            else -> null
        }
    }

    override fun getItemIcon(
        nodeElement: GraphvizNodeData?,
        nodeItem: Any?,
        builder: DiagramBuilder?
    ): Icon? {
        return when (nodeItem) {
            is GraphvizNodeField -> {
                if (nodeItem.isRequired()) {
                    com.intellij.util.PlatformIcons.FIELD_ICON // Could use a different icon for required fields
                } else {
                    com.intellij.util.PlatformIcons.FIELD_ICON
                }
            }
            is GraphvizAttributeItem -> com.intellij.util.PlatformIcons.METHOD_ICON
            else -> null
        }
    }
    
    companion object {
        /**
         * Create root data for a DOT file
         */
        fun getRootData(project: Project, virtualFile: VirtualFile): GraphvizDiagramRootData {
            val disposable = project.getService(GraphvizDiagramService::class.java)
            val filePointer = VirtualFilePointerManager.getInstance()
                .create(virtualFile, disposable, null)
            
            return GraphvizDiagramRootData(filePointer)
        }
        
        /**
         * Check if a file is a DOT file
         */
        private fun isDotFile(virtualFile: VirtualFile): Boolean {
            val extension = virtualFile.extension?.lowercase()
            return extension == "dot" || extension == "gv" || extension == "graphviz"
        }
    }
}
