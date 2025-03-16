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
 * Markdown计划解析器，负责将markdown格式的计划文本解析为结构化的执行计划
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
    private val logger = logger<MarkdownPlanParser>()

    // 文档解析相关
    private val markdownRootType = IElementType("ROOT")
    private val markdownFlavor = GFMFlavourDescriptor()

    // 任务状态标记
    private object TaskMarkers {
        val CHECKMARK = "✓"
        val COMPLETED = listOf("x", "X", "✓")
        val FAILED = listOf("!")
        val IN_PROGRESS = listOf("*")

        fun determineStatus(marker: String): TaskStatus = when (marker) {
            in COMPLETED -> TaskStatus.COMPLETED
            in FAILED -> TaskStatus.FAILED
            in IN_PROGRESS -> TaskStatus.IN_PROGRESS
            else -> TaskStatus.TODO
        }

        fun isCompleted(marker: String): Boolean = marker in COMPLETED
    }

    // 领域特定的模式匹配器
    private object PatternMatcher {
        /** 匹配任务项，例如 "- [x] 任务描述" */
        val TASK_PATTERN = Regex("^\\s*-\\s*\\[\\s*([xX!*✓]?)\\s*\\]\\s*(.*)")

        /** 匹配计划章节，例如 "1. [x] 章节标题" 或 "1. 章节标题 [x]" */
        val SECTION_PATTERN = Regex("^(\\d+)\\.\\s*(?:\\[([xX!*✓]?)\\]\\s*)?(.+?)(?:\\s*\\[([xX!*✓]?)\\])?$")

        /** 提取无序列表项的内容，去掉前缀 */
        val UNORDERED_ITEM_CLEANER = Regex("^[\\-\\*]\\s+")
    }

    /**
     * 解析markdown文本为结构化执行计划
     * @param content markdown格式的计划文本
     * @return 解析得到的执行计划列表
     */
    fun interpretPlan(content: String): List<AgentTaskEntry> {
        try {
            val documentTree = parseMarkdownDocument(content)
            return extractPlanStructure(documentTree, content)
        } catch (e: Exception) {
            logger.warn("无法解析markdown计划内容", e)
            return emptyList()
        }
    }

    /**
     * 解析Markdown文档结构
     */
    private fun parseMarkdownDocument(content: String): ASTNode {
        return MarkdownParser(markdownFlavor).parse(markdownRootType, content)
    }

    /**
     * 从文档结构中提取计划
     */
    private fun extractPlanStructure(documentNode: ASTNode, content: String): List<AgentTaskEntry> {
        val planSections = mutableListOf<AgentTaskEntry>()
        val topLevelSections = findTopLevelSections(documentNode)

        if (topLevelSections.isNotEmpty()) {
            if (isSimplePlanStructure(topLevelSections.first(), content)) {
                interpretSimplePlanStructure(topLevelSections.first(), content, planSections)
            } else {
                interpretDetailedPlanStructure(documentNode, content, planSections)
            }
        }

        return planSections
    }

    /**
     * 查找文档中的顶级章节列表
     */
    private fun findTopLevelSections(documentNode: ASTNode): List<ASTNode> {
        return documentNode.children.filter { it.type == MarkdownElementTypes.ORDERED_LIST }
    }

    /**
     * 判断是否为简单计划结构（无子任务的简单列表）
     */
    private fun isSimplePlanStructure(sectionNode: ASTNode, content: String): Boolean {
        return sectionNode.children.none { listItem ->
            listItem.type == MarkdownElementTypes.LIST_ITEM &&
                    listItem.children.any { it.type == MarkdownElementTypes.UNORDERED_LIST }
        }
    }

    /**
     * 解析简单计划结构（仅包含章节标题的列表）
     */
    private fun interpretSimplePlanStructure(
        sectionListNode: ASTNode,
        content: String,
        planSections: MutableList<AgentTaskEntry>
    ) {
        val sectionVisitor = PlanSectionVisitor(content)
        sectionListNode.children.forEach { item ->
            if (item.type == MarkdownElementTypes.LIST_ITEM) {
                val sectionInfo = sectionVisitor.extractBasicSectionInfo(item)
                if (sectionInfo != null) {
                    planSections.add(AgentTaskEntry(sectionInfo.title, emptyList(), sectionInfo.completed))
                }
            }
        }
    }

    /**
     * 解析详细计划结构（包含子任务的复杂结构）
     */
    private fun interpretDetailedPlanStructure(
        documentNode: ASTNode,
        content: String,
        planSections: MutableList<AgentTaskEntry>
    ) {
        val planStructureInterpreter = DetailedPlanStructureInterpreter(content)
        planStructureInterpreter.interpretDocument(documentNode, planSections)
    }

    /**
     * 访问者类，用于解析计划章节信息
     */
    private class PlanSectionVisitor(private val documentContent: String) {
        /**
         * 提取基础章节信息（无任务）
         */
        fun extractBasicSectionInfo(sectionNode: ASTNode): BasicSectionInfo? {
            val sectionText = sectionNode.getTextInNode(documentContent).toString().trim()
            val titleMatch = "^(\\d+)\\.\\s*(.+?)(?:\\s*${TaskMarkers.CHECKMARK})?$".toRegex().find(sectionText)

            return titleMatch?.let {
                val title = it.groupValues[2].trim()
                val completed = sectionText.contains(TaskMarkers.CHECKMARK)
                BasicSectionInfo(title, completed)
            }
        }

        /**
         * 从列表项节点中提取完整章节信息
         */
        fun extractDetailedSectionInfo(listItemNode: ASTNode): DetailedSectionInfo? {
            val fullText = listItemNode.getTextInNode(documentContent).toString().trim()
            val firstLineEnd = fullText.indexOf('\n')
            val headerLine = if (firstLineEnd > 0) fullText.substring(0, firstLineEnd).trim() else fullText

            val headerMatch = PatternMatcher.SECTION_PATTERN.find(headerLine) ?: return null

            // 提取章节标题和状态
            val title = headerMatch.groupValues[3].trim()

            // 获取状态标记（可能在开头或结尾）
            val startMarker = headerMatch.groupValues[2]
            val endMarker = headerMatch.groupValues[4]
            val statusMarker = if (startMarker.isNotEmpty()) startMarker else endMarker

            val completed = TaskMarkers.isCompleted(statusMarker)
            val status = TaskMarkers.determineStatus(statusMarker)

            return DetailedSectionInfo(title, completed, status)
        }
    }

    /**
     * 详细计划结构解析器
     */
    private class DetailedPlanStructureInterpreter(private val documentContent: String) {
        private val sectionVisitor = PlanSectionVisitor(documentContent)
        private val taskExtractor = TaskExtractor(documentContent)

        // 当前正在处理的章节信息
        private var currentSection: MutableSectionContext? = null

        /**
         * 解析整个文档结构，提取计划章节
         */
        fun interpretDocument(documentNode: ASTNode, planSections: MutableList<AgentTaskEntry>) {
            documentNode.accept(object : RecursiveVisitor() {
                override fun visitNode(node: ASTNode) {
                    when (node.type) {
                        MarkdownElementTypes.ORDERED_LIST -> {
                            processOrderedList(node, planSections)
                            // 跳过递归以避免重复处理
                    }

                    MarkdownElementTypes.UNORDERED_LIST -> {
                            processUnorderedList(node)
                            // 跳过递归以避免重复处理
                        }

                        else -> super.visitNode(node)
                    }
                }
            })

            // 处理最后一个章节（如果存在）
            finalizeCurrentSection(planSections)
        }

        /**
         * 处理有序列表（章节列表）
         */
        private fun processOrderedList(listNode: ASTNode, planSections: MutableList<AgentTaskEntry>) {
            listNode.children.forEach { item ->
                if (item.type == MarkdownElementTypes.LIST_ITEM) {
                    val sectionInfo = sectionVisitor.extractDetailedSectionInfo(item)

                    if (sectionInfo != null) {
                        // 完成前一章节的处理
                        finalizeCurrentSection(planSections)

                        // 创建新的章节上下文
                        currentSection = MutableSectionContext(
                            title = sectionInfo.title,
                            completed = sectionInfo.completed,
                            status = sectionInfo.status
                        )

                        // 处理子任务列表
                        item.children.forEach { childNode ->
                    if (childNode.type == MarkdownElementTypes.UNORDERED_LIST) {
                                processUnorderedList(childNode)
                            }
                        }
                    }
                }
            }
        }

        /**
         * 处理无序列表（任务列表）
         */
        private fun processUnorderedList(listNode: ASTNode) {
            currentSection?.let { section ->
                taskExtractor.extractTasks(listNode, section.tasks)
            }
        }

        /**
         * 完成当前章节处理并添加到计划列表
         */
        private fun finalizeCurrentSection(planSections: MutableList<AgentTaskEntry>) {
            currentSection?.let { section ->
                val plan = AgentTaskEntry(
                    section.title,
                    section.tasks,
                    section.completed,
                    section.status
                )
                plan.updateCompletionStatus()
                planSections.add(plan)
                currentSection = null
            }
        }
    }

    /**
     * 任务提取器
     */
    private class TaskExtractor(private val documentContent: String) {
        /**
         * 从无序列表中提取任务项
         */
        fun extractTasks(listNode: ASTNode, taskList: MutableList<AgentPlanStep>) {
            listNode.children.forEach { item ->
                if (item.type != MarkdownElementTypes.LIST_ITEM) return@forEach

                val taskText = item.getTextInNode(documentContent).toString().trim()
                val firstLineEnd = taskText.indexOf('\n')
                val taskLine = if (firstLineEnd > 0) taskText.substring(0, firstLineEnd).trim() else taskText

                // 尝试匹配GitHub风格的任务项
                val taskMatch = PatternMatcher.TASK_PATTERN.find(taskLine)

                if (taskMatch != null) {
                    // 提取任务描述和状态
                    val statusMarker = taskMatch.groupValues[1]
                    val description = taskMatch.groupValues[2].trim()

                    // 创建任务对象
                    val task = AgentPlanStep(
                        description,
                        TaskMarkers.isCompleted(statusMarker),
                        TaskMarkers.determineStatus(statusMarker)
                    )

                    taskList.add(task)
                } else {
                    // 处理普通列表项作为任务
                    val cleanDescription = taskLine.replace(PatternMatcher.UNORDERED_ITEM_CLEANER, "").trim()
                    if (cleanDescription.isNotEmpty()) {
                        taskList.add(AgentPlanStep(cleanDescription, false, TaskStatus.TODO))
                    }
                }

                // 处理嵌套任务
                item.children.forEach { childNode ->
                            if (childNode.type == MarkdownElementTypes.UNORDERED_LIST) {
                        extractTasks(childNode, taskList)
                    }
                }
            }
        }
    }

    // 领域模型 - 表示章节上下文的可变数据结构
    private data class MutableSectionContext(
        val title: String,
        var completed: Boolean,
        var status: TaskStatus,
        val tasks: MutableList<AgentPlanStep> = mutableListOf()
    )

    // 领域模型 - 基本章节信息
    private data class BasicSectionInfo(
        val title: String,
        val completed: Boolean
    )

    // 领域模型 - 详细章节信息
    private data class DetailedSectionInfo(
        val title: String,
        val completed: Boolean,
        val status: TaskStatus
    )

    // 为保持向后兼容性，提供parse方法的别名
    fun parse(content: String): List<AgentTaskEntry> = interpretPlan(content)
}