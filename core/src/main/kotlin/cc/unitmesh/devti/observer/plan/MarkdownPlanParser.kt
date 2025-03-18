package cc.unitmesh.devti.observer.plan

import com.intellij.openapi.diagnostic.logger
import kotlinx.serialization.Serializable
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.accept
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.ast.visitors.RecursiveVisitor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

/**
 * The `MarkdownPlanParser` is a Kotlin object designed to parse markdown-formatted task plans into structured data.
 * It processes markdown content, identifies tasks and sections, and organizes them into a hierarchical structure
 * that can be used for further processing or display. The parser supports both simple and detailed task structures,
 * allowing for flexible markdown formatting.
 *
 * ### Key Features:
 * - **Task Status Parsing**: Recognizes task status markers such as `[✓]` (completed), `[x]` (completed), `[!]` (failed),
 *   and `[*]` (in progress). Tasks without a marker are considered `TODO`.
 * - **Section Hierarchy**: Parses ordered lists (e.g., `1.`, `2.`) as sections and unordered lists (e.g., `-`, `*`) as tasks
 *   within those sections.
 * - **Flexible Input**: Handles both simple (flat) and detailed (nested) markdown structures.
 * - **Error Handling**: Logs warnings if parsing fails, ensuring graceful degradation.
 * - **Markdown Output**: Provides a method to format structured task entries back into markdown format.
 *
 * ### Usage Example:
 * Given the following markdown content:
 * ```markdown
 * 1. 领域模型重构：
 *   - [✓] 将BlogPost实体合并到Blog聚合根，建立完整的领域对象
 *   - [*] 添加领域行为方法（发布、审核、评论等）
 *   - [!] 修复数据模型冲突
 *   - [ ] 实现新增的领域服务
 * 2. 分层结构调整：
 *   - [ ] 清理entity层冗余对象
 * ```
 * The parser will produce a structured list of `AgentTaskEntry` objects, each containing:
 * - A title (e.g., "领域模型重构").
 * - A list of `AgentPlanStep` objects representing tasks (e.g., "将BlogPost实体合并到Blog聚合根").
 * - Status information for each task (e.g., `COMPLETED`, `IN_PROGRESS`, `FAILED`, or `TODO`).
 *
 * ### Methods:
 * - **`interpretPlan(content: String): List<AgentTaskEntry>`**: Parses the provided markdown content and returns a list of
 *   structured task entries.
 * - **`parse(content: String): List<AgentTaskEntry>`**: Alias for `interpretPlan`, providing the same functionality.
 * - **`formatPlanToMarkdown(entries: MutableList<AgentTaskEntry>): String`**: Converts a list of structured task entries
 *   back into markdown format.
 *
 * ### Internal Components:
 * - **`TaskMarkers`**: Handles task status markers and determines the status of tasks based on the marker.
 * - **`PatternMatcher`**: Provides regex patterns for identifying tasks and sections in markdown content.
 * - **`PlanSectionVisitor`**: Extracts section and task information from markdown nodes.
 * - **`DetailedPlanStructureInterpreter`**: Processes nested markdown structures and builds the task hierarchy.
 * - **`TaskExtractor`**: Extracts tasks from unordered lists and assigns their status and description.
 *
 * ### Notes:
 * - The parser uses the `MarkdownParser` from the `org.intellij.markdown` library to build an abstract syntax tree (AST)
 *   of the markdown content.
 * - It supports GitHub Flavored Markdown (GFM) syntax.
 * - The parser is designed to be robust and handle edge cases, such as missing markers or malformed markdown.
 *
 * ### Example Output:
 * For the example markdown above, the output might look like:
 * ```kotlin
 * listOf(
 *     AgentTaskEntry(
 *         title = "领域模型重构",
 *         steps = listOf(
 *             AgentPlanStep(step = "将BlogPost实体合并到Blog聚合根", completed = true, status = TaskStatus.COMPLETED),
 *             AgentPlanStep(step = "添加领域行为方法（发布、审核、评论等）", completed = false, status = TaskStatus.IN_PROGRESS),
 *             AgentPlanStep(step = "修复数据模型冲突", completed = false, status = TaskStatus.FAILED),
 *             AgentPlanStep(step = "实现新增的领域服务", completed = false, status = TaskStatus.TODO)
 *         ),
 *         completed = false,
 *         status = TaskStatus.IN_PROGRESS
 *     ),
 *     AgentTaskEntry(
 *         title = "分层结构调整",
 *         steps = listOf(
 *             AgentPlanStep(step = "清理entity层冗余对象", completed = false, status = TaskStatus.TODO)
 *         ),
 *         completed = false,
 *         status = TaskStatus.TODO
 *     )
 * )
 * ```
 */
object MarkdownPlanParser {
    private val logger = logger<MarkdownPlanParser>()

    private val markdownRootType = IElementType("ROOT")
    private val markdownFlavor = GFMFlavourDescriptor()

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

    private object PatternMatcher {
        val TASK_PATTERN = Regex("^\\s*-\\s*\\[\\s*([xX!*✓]?)\\s*\\]\\s*(.*)")
        val SECTION_PATTERN = Regex("^(\\d+)\\.\\s*(?:\\[([xX!*✓]?)\\]\\s*)?(.+?)(?:\\s*\\[([xX!*✓]?)\\])?$")
        val UNORDERED_ITEM_CLEANER = Regex("^[\\-\\*]\\s+")
        val CODE_FILE_LINK = Regex("\\[(.*?)\\]\\((.*?)\\)")
    }

    fun interpretPlan(content: String): List<AgentTaskEntry> {
        try {
            val documentTree = parseMarkdownDocument(content)
            return extractPlanStructure(documentTree, content)
        } catch (e: Exception) {
            logger.warn("无法解析markdown计划内容", e)
            return emptyList()
        }
    }


    private fun parseMarkdownDocument(content: String): ASTNode {
        return MarkdownParser(markdownFlavor).parse(markdownRootType, content)
    }

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

    private fun findTopLevelSections(documentNode: ASTNode): List<ASTNode> {
        return documentNode.children.filter { it.type == MarkdownElementTypes.ORDERED_LIST }
    }

    private fun isSimplePlanStructure(sectionNode: ASTNode, content: String): Boolean {
        return sectionNode.children.none { listItem ->
            listItem.type == MarkdownElementTypes.LIST_ITEM &&
                    listItem.children.any { it.type == MarkdownElementTypes.UNORDERED_LIST }
        }
    }

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

    private fun interpretDetailedPlanStructure(
        documentNode: ASTNode,
        content: String,
        planSections: MutableList<AgentTaskEntry>
    ) {
        val planStructureInterpreter = DetailedPlanStructureInterpreter(content)
        planStructureInterpreter.interpretDocument(documentNode, planSections)
    }

    private class PlanSectionVisitor(private val documentContent: String) {
        fun extractBasicSectionInfo(sectionNode: ASTNode): BasicSectionInfo? {
            val sectionText = sectionNode.getTextInNode(documentContent).toString().trim()
            val titleMatch = "^(\\d+)\\.\\s*(.+?)(?:\\s*${TaskMarkers.CHECKMARK})?$".toRegex().find(sectionText)

            return titleMatch?.let {
                val title = it.groupValues[2].trim()
                val completed = sectionText.contains(TaskMarkers.CHECKMARK)
                BasicSectionInfo(title, completed)
            }
        }

        fun extractDetailedSectionInfo(listItemNode: ASTNode): DetailedSectionInfo? {
            val fullText = listItemNode.getTextInNode(documentContent).toString().trim()
            val firstLineEnd = fullText.indexOf('\n')
            val headerLine = if (firstLineEnd > 0) fullText.substring(0, firstLineEnd).trim() else fullText

            val headerMatch = PatternMatcher.SECTION_PATTERN.find(headerLine) ?: return null

            val title = headerMatch.groupValues[3].trim()

            val startMarker = headerMatch.groupValues[2]
            val endMarker = headerMatch.groupValues[4]
            val statusMarker = if (startMarker.isNotEmpty()) startMarker else endMarker

            val completed = TaskMarkers.isCompleted(statusMarker)
            val status = TaskMarkers.determineStatus(statusMarker)

            return DetailedSectionInfo(title, completed, status)
        }
    }

    private class DetailedPlanStructureInterpreter(private val documentContent: String) {
        private val sectionVisitor = PlanSectionVisitor(documentContent)
        private val taskExtractor = TaskExtractor(documentContent)
        private var currentSection: MutableSectionContext? = null

        fun interpretDocument(documentNode: ASTNode, planSections: MutableList<AgentTaskEntry>) {
            documentNode.accept(object : RecursiveVisitor() {
                override fun visitNode(node: ASTNode) {
                    when (node.type) {
                        MarkdownElementTypes.ORDERED_LIST -> {
                            processOrderedList(node, planSections)
                        }

                        MarkdownElementTypes.UNORDERED_LIST -> {
                            processUnorderedList(node)
                        }

                        else -> super.visitNode(node)
                    }
                }
            })

            finalizeCurrentSection(planSections)
        }

        private fun processOrderedList(listNode: ASTNode, planSections: MutableList<AgentTaskEntry>) {
            listNode.children.forEach { item ->
                if (item.type == MarkdownElementTypes.LIST_ITEM) {
                    val sectionInfo = sectionVisitor.extractDetailedSectionInfo(item)

                    if (sectionInfo != null) {
                        finalizeCurrentSection(planSections)
                        currentSection = MutableSectionContext(
                            title = sectionInfo.title,
                            completed = sectionInfo.completed,
                            status = sectionInfo.status
                        )
                        item.children.forEach { childNode ->
                            if (childNode.type == MarkdownElementTypes.UNORDERED_LIST) {
                                processUnorderedList(childNode)
                            }
                        }
                    }
                }
            }
        }

        private fun processUnorderedList(listNode: ASTNode) {
            currentSection?.let { section ->
                taskExtractor.extractTasks(listNode, section.tasks)
            }
        }

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

    private class TaskExtractor(private val documentContent: String) {
        fun extractTasks(listNode: ASTNode, taskList: MutableList<AgentPlanStep>) {
            listNode.children.forEach { item ->
                if (item.type != MarkdownElementTypes.LIST_ITEM) return@forEach

                val taskText = item.getTextInNode(documentContent).toString().trim()
                val firstLineEnd = taskText.indexOf('\n')
                val taskLine = if (firstLineEnd > 0) taskText.substring(0, firstLineEnd).trim() else taskText

                val taskMatch = PatternMatcher.TASK_PATTERN.find(taskLine)
                if (taskMatch != null) {
                    val statusMarker = taskMatch.groupValues[1]
                    val description = taskMatch.groupValues[2].trim()

                    // Extract code file links from the description
                    val codeFileLinks = extractCodeFileLinks(description)
                    val task = AgentPlanStep(
                        description,
                        TaskMarkers.isCompleted(statusMarker),
                        TaskMarkers.determineStatus(statusMarker),
                        codeFileLinks = codeFileLinks
                    )

                    taskList.add(task)
                } else {
                    val cleanDescription = taskLine.replace(PatternMatcher.UNORDERED_ITEM_CLEANER, "").trim()
                    if (cleanDescription.isNotEmpty()) {
                        val codeFileLinks = extractCodeFileLinks(cleanDescription)
                        taskList.add(
                            AgentPlanStep(
                                cleanDescription,
                                false,
                                TaskStatus.TODO,
                                codeFileLinks = codeFileLinks
                            )
                        )
                    }
                }
                item.children.forEach { childNode ->
                    if (childNode.type == MarkdownElementTypes.UNORDERED_LIST) {
                        extractTasks(childNode, taskList)
                    }
                }
            }
        }

        private fun extractCodeFileLinks(text: String): List<CodeFileLink> {
            val links = mutableListOf<CodeFileLink>()
            val matches = PatternMatcher.CODE_FILE_LINK.findAll(text)

            matches.forEach { match ->
                val displayText = match.groupValues[1]
                val filePath = match.groupValues[2]
                links.add(CodeFileLink(displayText, filePath))
            }

            return links
        }
    }

    fun parse(content: String): List<AgentTaskEntry> = interpretPlan(content)
    fun formatPlanToMarkdown(entries: MutableList<AgentTaskEntry>): String {
        val stringBuilder = StringBuilder()
        entries.forEachIndexed { index, entry ->
            stringBuilder.append("${index + 1}. ${entry.title}\n")
            entry.steps.forEach { step ->
                val stepText = if (step.codeFileLinks.isNotEmpty()) {
                    var text = step.step
                    step.codeFileLinks.forEach { link ->
                        text = text.replace(
                            "[${link.displayText}](${link.filePath})",
                            "[${link.displayText}](${link.filePath})"
                        )
                    }
                    text
                } else {
                    step.step
                }
                stringBuilder.append("   - [${if (step.completed) "x" else " "}] $stepText\n")
            }
        }
        return stringBuilder.toString()
    }
}


data class MutableSectionContext(
    val title: String,
    var completed: Boolean,
    var status: TaskStatus,
    val tasks: MutableList<AgentPlanStep> = mutableListOf()
)

data class BasicSectionInfo(
    val title: String,
    val completed: Boolean
)

data class DetailedSectionInfo(
    val title: String,
    val completed: Boolean,
    val status: TaskStatus
)

@Serializable
data class CodeFileLink(
    val displayText: String,
    val filePath: String
)
