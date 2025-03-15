package cc.unitmesh.devti.sketch.ui.plan

import com.intellij.openapi.diagnostic.logger
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.accept
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.ast.visitors.RecursiveVisitor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

/**
 * Markdown计划解析器，负责将markdown格式的计划文本解析为PlanItem对象列表
 *
 * 示例：
 *
 * ```markdown
 * 1. 领域模型重构：
 *   - 将BlogPost实体合并到Blog聚合根，建立完整的领域对象
 *   - 添加领域行为方法（发布、审核、评论等）
 * 2. 分层结构调整：
 *   - 清理entity层冗余对象
 * ```
*/
object MarkdownPlanParser {
    private val LOG = logger<MarkdownPlanParser>()
    private val ROOT_ELEMENT_TYPE = IElementType("ROOT")
    private val CHECKMARK = "✓"

    /**
     * 解析markdown文本为计划项列表
     * @param content markdown格式的计划文本
     * @return 解析得到的计划项列表，若解析失败则返回空列表
     */
    fun parse(content: String): List<PlanItem> {
        try {
            val flavour = GFMFlavourDescriptor()
            val parsedTree = MarkdownParser(flavour).parse(ROOT_ELEMENT_TYPE, content)
            return parsePlanItems(parsedTree, content)
        } catch (e: Exception) {
            LOG.warn("Failed to parse markdown plan content", e)
            return emptyList()
        }
    }

    private fun parsePlanItems(node: ASTNode, content: String): List<PlanItem> {
        val planItems = mutableListOf<PlanItem>()
        var currentSectionTitle = ""
        var currentSectionItems = mutableListOf<String>()
        var currentSectionCompleted = false

        node.accept(object : RecursiveVisitor() {
            override fun visitNode(node: ASTNode) {
                when (node.type) {
                    MarkdownElementTypes.ORDERED_LIST -> {
                        node.children.forEach { listItemNode ->
                            if (listItemNode.type == MarkdownElementTypes.LIST_ITEM) {
                                // Extract just the first line for the section title
                                val listItemFullText = listItemNode.getTextInNode(content).toString().trim()
                                val firstLineEnd = listItemFullText.indexOf('\n')
                                val listItemFirstLine = if (firstLineEnd > 0) {
                                    listItemFullText.substring(0, firstLineEnd).trim()
                                } else {
                                    listItemFullText
                                }
                                
                                // Extract the title and completion status
                                val titleMatch = "^(\\d+)\\.\\s*(.+?)(?:\\s*$CHECKMARK)?$".toRegex().find(listItemFirstLine)

                                if (titleMatch != null) {
                                    // Save previous section if exists
                                    if (currentSectionTitle.isNotEmpty() && currentSectionItems.isNotEmpty()) {
                                        planItems.add(PlanItem(
                                            currentSectionTitle, 
                                            currentSectionItems.toList(),
                                            currentSectionCompleted
                                        ))
                                        currentSectionItems = mutableListOf()
                                    }

                                    // Extract the title without the checkmark
                                    currentSectionTitle = titleMatch.groupValues[2].trim()
                                    // Check if section is marked as completed
                                    currentSectionCompleted = listItemFirstLine.contains(CHECKMARK)

                                    // Process child nodes for tasks
                                    listItemNode.children.forEach { childNode ->
                                        if (childNode.type == MarkdownElementTypes.UNORDERED_LIST) {
                                            processTaskItems(childNode, content, currentSectionItems)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    MarkdownElementTypes.UNORDERED_LIST -> {
                        processTaskItems(node, content, currentSectionItems)
                    }
                }

                super.visitNode(node)
            }

            private fun processTaskItems(listNode: ASTNode, content: String, itemsList: MutableList<String>) {
                listNode.children.forEach { taskItemNode ->
                    if (taskItemNode.type == MarkdownElementTypes.LIST_ITEM) {
                        val taskText = taskItemNode.getTextInNode(content).toString().trim()
                        // Extract just the first line for the task
                        val firstLineEnd = taskText.indexOf('\n')
                        val taskFirstLine = if (firstLineEnd > 0) {
                            taskText.substring(0, firstLineEnd).trim()
                        } else {
                            taskText
                        }
                        
                        // Process task text and retain the checkmark in the text
                        val cleanTaskText = taskFirstLine.replace(Regex("^[\\-\\*]\\s+"), "").trim()
                        if (cleanTaskText.isNotEmpty()) {
                            itemsList.add(cleanTaskText)
                        }
                    }
                }
            }
        })

        // 添加最后一个章节（如果有）
        if (currentSectionTitle.isNotEmpty() && currentSectionItems.isNotEmpty()) {
            planItems.add(PlanItem(
                currentSectionTitle, 
                currentSectionItems.toList(),
                currentSectionCompleted
            ))
        }

        return planItems
    }
    
    // For debugging purposes
    private fun printNodeStructure(node: ASTNode, content: String, indent: String = "") {
        println("$indent${node.type}: ${node.getTextInNode(content)}")
        node.children.forEach { child ->
            printNodeStructure(child, content, "$indent  ")
        }
    }
}

data class PlanItem(
    val title: String,
    val tasks: List<String>,
    val completed: Boolean = false,
    val taskCompleted: MutableList<Boolean> = MutableList(tasks.size) { false }
) {
    init {
        // Parse task completion status for each task
        tasks.forEachIndexed { index, task ->
            taskCompleted[index] = task.contains("✓")
        }
    }
}