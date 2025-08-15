package cc.unitmesh.diagram.diagram

import cc.unitmesh.diagram.model.*
import com.intellij.diagram.AbstractDiagramElementManager
import com.intellij.diagram.DiagramBuilder
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager
import com.intellij.ui.SimpleColoredText
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ArrayUtil
import com.intellij.util.PlatformIcons
import javax.swing.Icon

/**
 * Element manager for Graphviz diagrams
 * Similar to JdlUmlElementManager in JHipster UML implementation
 */
class CodeTopologyElementManager : AbstractDiagramElementManager<GraphNodeData>() {
    override fun findInDataContext(dataContext: DataContext): GraphNodeData? {
        val psiFile = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return null
        val virtualFile = psiFile.virtualFile ?: return null
        if (!isDotFile(virtualFile)) return null
        return getRootData(psiFile.project, virtualFile)
    }
    
    override fun isAcceptableAsNode(element: Any?): Boolean {
        return element is GraphEntityNodeData || element is GraphSimpleNodeData
    }

    /**
     * Get node items (fields for entities, attributes for simple nodes)
     */
    override fun getNodeItems(nodeElement: GraphNodeData): Array<Any> {
        return when (nodeElement) {
            is GraphEntityNodeData -> nodeElement.getFields().toTypedArray()
            is GraphSimpleNodeData -> {
                nodeElement.getAttributes().map { (key, value) ->
                    GraphAttributeItem(key, value)
                }.toTypedArray()
            }
            else -> ArrayUtil.EMPTY_OBJECT_ARRAY
        }
    }
    
    override fun getElementTitle(element: GraphNodeData): String? {
        return element.getName()
    }
    
    override fun getNodeTooltip(element: GraphNodeData): String? {
        return when (element) {
            is GraphEntityNodeData -> {
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
            is GraphSimpleNodeData -> {
                buildString {
                    val nodeType = when (element.getNodeType()) {
                        GraphvizNodeType.CLUSTER -> "Subgraph/Cluster"
                        GraphvizNodeType.RECORD -> "Record Node"
                        GraphvizNodeType.REGULAR -> "Node"
                    }
                    append("$nodeType: ${element.getName()}")
                    if (element.getDisplayLabel() != element.getName()) {
                        append("\nLabel: ${element.getDisplayLabel()}")
                    }
                    append("\nShape: ${element.getShape()}")
                    element.getColor()?.let { append("\nColor: $it") }
                    element.getStyle()?.let { append("\nStyle: $it") }

                    // Show additional info for cluster nodes
                    if (element.getNodeType() == GraphvizNodeType.CLUSTER) {
                        element.getAttribute("bgcolor")?.let { append("\nBackground: $it") }
                    }
                }
            }
            else -> element.getName()
        }
    }

    override fun canBeBuiltFrom(element: Any?): Boolean {
        return element is GraphDiagramRootData || super.canBeBuiltFrom(element)
    }

    override fun getItemName(
        nodeElement: GraphNodeData?,
        nodeItem: Any?,
        builder: DiagramBuilder
    ): SimpleColoredText? {
        return when (nodeItem) {
            is GraphNodeField -> {
                val displayName = nodeItem.getDisplayName()
                val attributes = when (nodeItem.changeStatus) {
                    ChangeStatus.ADDED -> SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                    ChangeStatus.REMOVED -> SimpleTextAttributes.GRAYED_ATTRIBUTES
                    ChangeStatus.UNCHANGED -> SimpleTextAttributes.REGULAR_ATTRIBUTES
                }
                SimpleColoredText(displayName, attributes)
            }
            is GraphAttributeItem -> SimpleColoredText(nodeItem.key, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            else -> null
        }
    }

    override fun getItemType(
        nodeElement: GraphNodeData?,
        nodeItem: Any?,
        builder: DiagramBuilder?
    ): SimpleColoredText? {
        return when (nodeItem) {
            is GraphNodeField -> {
                nodeItem.type?.let {
                    SimpleColoredText(it, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }
            is GraphAttributeItem -> SimpleColoredText(nodeItem.value, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            else -> null
        }
    }

    override fun getItemIcon(
        nodeElement: GraphNodeData?,
        nodeItem: Any?,
        builder: DiagramBuilder?
    ): Icon? {
        return when (nodeItem) {
            is GraphNodeField -> {
                if (nodeItem.isRequired()) {
                    PlatformIcons.FIELD_ICON // Could use a different icon for required fields
                } else {
                    PlatformIcons.FIELD_ICON
                }
            }
            is GraphAttributeItem -> PlatformIcons.METHOD_ICON
            else -> null
        }
    }
    
    companion object Companion {
        /**
         * Create root data for a DOT file
         */
        fun getRootData(project: Project, virtualFile: VirtualFile): GraphDiagramRootData {
            val disposable = project.getService(CodeTopologyDiagramService::class.java)
            val filePointer = VirtualFilePointerManager.getInstance()
                .create(virtualFile, disposable, null)
            
            return GraphDiagramRootData(filePointer)
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
