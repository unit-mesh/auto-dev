package cc.unitmesh.devti.observer.plan

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
 *   - [✓] 将BlogPost实体合并到Blog聚合根，建立完整的领域对象
 *   - [*] 添加领域行为方法（发布、审核、评论等）
 *   - [!] 修复数据模型冲突
 *   - [ ] 实现新增的领域服务
 * 2. 分层结构调整：
 *   - [ ] 清理entity层冗余对象
 * ```
 */
object MarkdownPlanParser {
    private val LOG = logger<MarkdownPlanParser>()
    private val ROOT_ELEMENT_TYPE = IElementType("ROOT")
    private val CHECKMARK = "✓"
    private val GITHUB_TODO_PATTERN = Regex("^\\s*-\\s*\\[\\s*([xX!*✓]?)\\s*\\]\\s*(.*)")
    private val GITHUB_TODO_COMPLETED = listOf("x", "X", "✓")
    private val GITHUB_TODO_FAILED = listOf("!")
    private val GITHUB_TODO_IN_PROGRESS = listOf("*")
    
    // 提取为常量并添加注释，增强语义性
    /** 匹配有序列表项的标题和状态标记，例如 "1. [x] 标题" 或 "1. 标题 [x]" */
    private val SECTION_HEADER_PATTERN = Regex("^(\\d+)\\.\\s*(?:\\[([xX!*✓]?)\\]\\s*)?(.+?)(?:\\s*\\[([xX!*✓]?)\\])?$")

    /**
     * 解析markdown文本为计划项列表
     * @param content markdown格式的计划文本
     * @return 解析得到的计划项列表，若解析失败则返回空列表
     */
    fun parse(content: String): List<AgentPlan> {
        try {
            val flavour = GFMFlavourDescriptor()
            val parsedTree = MarkdownParser(flavour).parse(ROOT_ELEMENT_TYPE, content)
            return parsePlanItems(parsedTree, content)
        } catch (e: Exception) {
            LOG.warn("Failed to parse markdown plan content", e)
            return emptyList()
        }
    }

    private fun parsePlanItems(node: ASTNode, content: String): List<AgentPlan> {
        val agentPlans = mutableListOf<AgentPlan>()
        val topLevelOrderedLists = findTopLevelOrderedLists(node)
        if (topLevelOrderedLists.isNotEmpty() && isFlatOrderedList(topLevelOrderedLists.first(), content)) {
            processFlatOrderedList(topLevelOrderedLists.first(), content, agentPlans)
        } else {
            processSectionedList(node, content, agentPlans)
        }

        return agentPlans
    }

    private fun findTopLevelOrderedLists(node: ASTNode): List<ASTNode> {
        val orderedLists = mutableListOf<ASTNode>()

        node.children.forEach { child ->
            if (child.type == MarkdownElementTypes.ORDERED_LIST) {
                orderedLists.add(child)
            }
        }

        return orderedLists
    }

    private fun processFlatOrderedList(node: ASTNode, content: String, agentPlans: MutableList<AgentPlan>) {
        node.children.forEach { listItemNode ->
            if (listItemNode.type == MarkdownElementTypes.LIST_ITEM) {
                val listItemText = listItemNode.getTextInNode(content).toString().trim()
                val titleMatch = "^(\\d+)\\.\\s*(.+?)(?:\\s*$CHECKMARK)?$".toRegex().find(listItemText)

                if (titleMatch != null) {
                    val title = titleMatch.groupValues[2].trim()
                    val completed = listItemText.contains(CHECKMARK)
                    agentPlans.add(AgentPlan(title, emptyList(), completed))
                }
            }
        }
    }

    private fun processSectionedList(node: ASTNode, content: String, agentPlans: MutableList<AgentPlan>) {
        var currentSectionTitle = ""
        var currentSectionItems = mutableListOf<PlanTask>()
        var currentSectionCompleted = false
        var currentSectionStatus = TaskStatus.TODO

        node.accept(object : RecursiveVisitor() {
            override fun visitNode(node: ASTNode) {
                when (node.type) {
                    MarkdownElementTypes.ORDERED_LIST -> {
                        node.children.forEach { listItemNode ->
                            if (listItemNode.type == MarkdownElementTypes.LIST_ITEM) {
                                // 处理计划项的标题行
                                val sectionHeaderInfo = extractSectionHeaderInfo(listItemNode, content)
                                
                                if (sectionHeaderInfo != null) {
                                    // 保存之前的计划章节（如果存在）
                                    if (currentSectionTitle.isNotEmpty()) {
                                        finalizeSectionAndAddToPlan(
                                            agentPlans,
                                            currentSectionTitle,
                                            currentSectionItems,
                                            currentSectionCompleted,
                                            currentSectionStatus
                                        )
                                        currentSectionItems = mutableListOf()
                                    }

                                    // 设置当前章节的标题和状态
                                    currentSectionTitle = sectionHeaderInfo.title
                                    currentSectionCompleted = sectionHeaderInfo.completed
                                    currentSectionStatus = sectionHeaderInfo.status

                                    // 处理子任务列表
                                    processNestedTaskLists(listItemNode, content, currentSectionItems)
                                }
                            }
                        }
                        // 跳过递归处理以避免重复
                    }

                    MarkdownElementTypes.UNORDERED_LIST -> {
                        processTaskItems(node, content, currentSectionItems)
                        // 跳过递归处理以避免重复
                    }

                    else -> {
                        // 仅对其他节点类型继续递归
                        super.visitNode(node)
                    }
                }
            }
        })

        // 添加最后一个章节（如果有）
        if (currentSectionTitle.isNotEmpty()) {
            finalizeSectionAndAddToPlan(
                agentPlans,
                currentSectionTitle,
                currentSectionItems,
                currentSectionCompleted,
                currentSectionStatus
            )
        }
    }

    /**
     * 从列表项节点中提取章节标题和状态信息
     * @return 包含标题、完成状态和任务状态的数据类，如果不匹配则返回null
     */
    private fun extractSectionHeaderInfo(listItemNode: ASTNode, content: String): SectionHeaderInfo? {
        val listItemFullText = listItemNode.getTextInNode(content).toString().trim()
        val firstLineEnd = listItemFullText.indexOf('\n')
        val headerLine = if (firstLineEnd > 0) {
            listItemFullText.substring(0, firstLineEnd).trim()
        } else {
            listItemFullText
        }

        val headerMatch = SECTION_HEADER_PATTERN.find(headerLine)
        
        return headerMatch?.let {
            // 提取不带状态标记的标题
            val title = it.groupValues[3].trim()
            
            // 获取状态标记（可能在开头或结尾）
            val startStatusMarker = it.groupValues[2]
            val endStatusMarker = it.groupValues[4]
            val statusMarker = if (startStatusMarker.isNotEmpty()) startStatusMarker else endStatusMarker
            
            val completed = statusMarker in GITHUB_TODO_COMPLETED
            val status = determineTaskStatus(statusMarker)
            
            SectionHeaderInfo(title, completed, status)
        }
    }
    
    /**
     * 根据状态标记确定任务状态
     */
    private fun determineTaskStatus(statusMarker: String): TaskStatus {
        return when (statusMarker) {
            in GITHUB_TODO_COMPLETED -> TaskStatus.COMPLETED
            in GITHUB_TODO_FAILED -> TaskStatus.FAILED
            in GITHUB_TODO_IN_PROGRESS -> TaskStatus.IN_PROGRESS
            else -> TaskStatus.TODO
        }
    }
    
    /**
     * 处理列表项中的嵌套任务列表
     */
    private fun processNestedTaskLists(listItemNode: ASTNode, content: String, tasks: MutableList<PlanTask>) {
        listItemNode.children.forEach { childNode ->
            if (childNode.type == MarkdownElementTypes.UNORDERED_LIST) {
                processTaskItems(childNode, content, tasks)
            }
        }
    }
    
    /**
     * 完成章节处理并添加到计划列表中
     */
    private fun finalizeSectionAndAddToPlan(
        plans: MutableList<AgentPlan>,
        title: String,
        tasks: List<PlanTask>,
        completed: Boolean,
        status: TaskStatus
    ) {
        val plan = AgentPlan(title, tasks.toList(), completed, status)
        plan.updateCompletionStatus()
        plans.add(plan)
    }

    private fun isFlatOrderedList(node: ASTNode, content: String): Boolean {
        var hasNestedLists = false

        node.children.forEach { listItemNode ->
            if (listItemNode.type == MarkdownElementTypes.LIST_ITEM) {
                listItemNode.children.forEach { childNode ->
                    if (childNode.type == MarkdownElementTypes.UNORDERED_LIST) {
                        hasNestedLists = true
                    }
                }
            }
        }

        return !hasNestedLists
    }

    private fun processTaskItems(listNode: ASTNode, content: String, itemsList: MutableList<PlanTask>) {
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

                // Check for GitHub style TODO
                val githubTodoMatch = GITHUB_TODO_PATTERN.find(taskFirstLine)
                if (githubTodoMatch != null) {
                    // Extract the task text and preserve the checkbox status
                    val checkState = githubTodoMatch.groupValues[1]
                    val todoText = githubTodoMatch.groupValues[2].trim()

                    // Determine task status based on marker
                    val task = when (checkState) {
                        in GITHUB_TODO_COMPLETED -> PlanTask(todoText, true, TaskStatus.COMPLETED)
                        in GITHUB_TODO_FAILED -> PlanTask(todoText, false, TaskStatus.FAILED)
                        in GITHUB_TODO_IN_PROGRESS -> PlanTask(todoText, false, TaskStatus.IN_PROGRESS)
                        else -> PlanTask(todoText, false, TaskStatus.TODO)
                    }

                    itemsList.add(task)

                    // Process nested tasks if any
                    taskItemNode.children.forEach { childNode ->
                        if (childNode.type == MarkdownElementTypes.UNORDERED_LIST) {
                            val nestedTasks = mutableListOf<PlanTask>()
                            processTaskItems(childNode, content, nestedTasks)
                            itemsList.addAll(nestedTasks)
                        }
                    }
                } else {
                    val cleanTaskText = taskFirstLine.replace(Regex("^[\\-\\*]\\s+"), "").trim()
                    if (cleanTaskText.isNotEmpty()) {
                        itemsList.add(PlanTask(cleanTaskText, false, TaskStatus.TODO))
                        // Process nested tasks if any
                        taskItemNode.children.forEach { childNode ->
                            if (childNode.type == MarkdownElementTypes.UNORDERED_LIST) {
                                val nestedTasks = mutableListOf<PlanTask>()
                                processTaskItems(childNode, content, nestedTasks)
                                // Add nested tasks to the list
                                itemsList.addAll(nestedTasks)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 用于存储章节标题和状态信息的数据类
     */
    private data class SectionHeaderInfo(
        val title: String,
        val completed: Boolean,
        val status: TaskStatus
    )
}