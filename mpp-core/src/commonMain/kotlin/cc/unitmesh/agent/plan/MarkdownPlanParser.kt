package cc.unitmesh.agent.plan

/**
 * Parser for markdown-formatted plans.
 * 
 * Supports the following format:
 * ```
 * 1. Task Title
 *    - [✓] Completed step
 *    - [*] In-progress step
 *    - [ ] Todo step
 *    - [!] Failed step
 * 2. Another Task
 *    - [ ] Step with [file link](path/to/file.kt)
 * ```
 * 
 * This is a multiplatform implementation that doesn't depend on
 * IntelliJ's markdown parser.
 */
object MarkdownPlanParser {
    
    // Pattern for task headers: "1. Task Title" or "1. [x] Task Title"
    private val TASK_HEADER_PATTERN = Regex("^(\\d+)\\.\\s*(?:\\[([xX!*✓]?)\\]\\s*)?(.+?)(?:\\s*\\[([xX!*✓]?)\\])?$")
    
    // Pattern for step items: "- [x] Step description"
    private val STEP_PATTERN = Regex("^\\s*[-*]\\s*\\[\\s*([xX!*✓]?)\\s*]\\s*(.*)")
    
    // Pattern for unordered list items without checkbox: "- Step description"
    private val UNORDERED_ITEM_PATTERN = Regex("^\\s*[-*]\\s+(.+)")
    
    /**
     * Parse markdown content into a list of PlanTasks.
     */
    fun parse(content: String): List<PlanTask> {
        val lines = content.lines()
        val tasks = mutableListOf<PlanTask>()
        var currentTask: PlanTask? = null
        var stepIdCounter = 0
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            
            // Try to match task header
            val taskMatch = TASK_HEADER_PATTERN.find(trimmedLine)
            if (taskMatch != null) {
                // Save previous task if exists
                currentTask?.let { 
                    it.updateStatusFromSteps()
                    tasks.add(it) 
                }
                
                val title = taskMatch.groupValues[3].trim()
                val startMarker = taskMatch.groupValues[2]
                val endMarker = taskMatch.groupValues[4]
                val marker = startMarker.ifEmpty { endMarker }
                
                currentTask = PlanTask(
                    id = PlanTask.generateId(),
                    title = title,
                    status = TaskStatus.fromMarker(marker)
                )
                continue
            }
            
            // Try to match step with checkbox
            val stepMatch = STEP_PATTERN.find(line)
            if (stepMatch != null && currentTask != null) {
                val marker = stepMatch.groupValues[1]
                val description = stepMatch.groupValues[2].trim()
                val codeFileLinks = CodeFileLink.extractFromText(description)
                
                val step = PlanStep(
                    id = "step_${++stepIdCounter}",
                    description = description,
                    status = TaskStatus.fromMarker(marker),
                    codeFileLinks = codeFileLinks
                )
                currentTask.addStep(step)
                continue
            }
            
            // Try to match unordered list item without checkbox
            val unorderedMatch = UNORDERED_ITEM_PATTERN.find(line)
            if (unorderedMatch != null && currentTask != null) {
                val description = unorderedMatch.groupValues[1].trim()
                // Skip if it looks like a checkbox item that didn't match
                if (description.startsWith("[")) continue
                
                val codeFileLinks = CodeFileLink.extractFromText(description)
                val step = PlanStep(
                    id = "step_${++stepIdCounter}",
                    description = description,
                    status = TaskStatus.TODO,
                    codeFileLinks = codeFileLinks
                )
                currentTask.addStep(step)
            }
        }
        
        // Don't forget the last task
        currentTask?.let { 
            it.updateStatusFromSteps()
            tasks.add(it) 
        }
        
        return tasks
    }
    
    /**
     * Format a list of tasks back to markdown.
     */
    fun formatToMarkdown(tasks: List<PlanTask>): String {
        val sb = StringBuilder()
        tasks.forEachIndexed { index, task ->
            sb.append(task.toMarkdown(index + 1))
        }
        return sb.toString()
    }
    
    /**
     * Parse markdown content into an AgentPlan.
     */
    fun parseToPlan(content: String): AgentPlan {
        val tasks = parse(content)
        return AgentPlan.create(tasks)
    }
}

