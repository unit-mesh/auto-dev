package cc.unitmesh.devins.ui.compose.agent.codereview

/**
 * Parses Plan format markdown into structured plan items
 * Supports nested structure: ordered list for items, unordered list for steps
 */
object PlanParser {
    private val CODE_BLOCK_PATTERN = Regex("```plan\\s*\\n([\\s\\S]*?)\\n```", RegexOption.IGNORE_CASE)
    private val ORDERED_ITEM_PATTERN = Regex("^(\\d+)\\.\\s*(.+?)(?:\\s*-\\s*(.+))?$")
    private val UNORDERED_ITEM_PATTERN = Regex("^\\s*-\\s*\\[([\\s✓!*]?)\\]\\s*(.+)$")
    private val FILE_LINK_PATTERN = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")

    /**
     * Parse plan markdown output into structured plan items
     */
    fun parse(planOutput: String): List<PlanItem> {
        val items = mutableListOf<PlanItem>()

        // Extract plan code block if present
        val planContent = CODE_BLOCK_PATTERN.find(planOutput)?.groupValues?.get(1)
            ?: planOutput

        val lines = planContent.lines()
        var currentItem: PlanItem? = null
        var currentSteps = mutableListOf<PlanStep>()
        var currentNumber = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Check for ordered list item (main plan item)
            val orderedMatch = ORDERED_ITEM_PATTERN.find(trimmed)
            if (orderedMatch != null) {
                // Save previous item
                currentItem?.let {
                    items.add(it.copy(steps = currentSteps.toList()))
                }

                // Start new item
                currentNumber = orderedMatch.groupValues[1].toIntOrNull() ?: 0
                val title = orderedMatch.groupValues[2].trim()

                // Extract priority from regex group 3 (format: "Title - PRIORITY")
                val priority = orderedMatch.groupValues.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() } ?: "MEDIUM"

                currentItem = PlanItem(
                    number = currentNumber,
                    title = title,
                    priority = priority
                )
                currentSteps = mutableListOf()
                continue
            }

            // Check for unordered list item (plan step)
            val unorderedMatch = UNORDERED_ITEM_PATTERN.find(trimmed)
            if (unorderedMatch != null) {
                val statusMarker = unorderedMatch.groupValues[1].trim()
                val stepText = unorderedMatch.groupValues[2].trim()

                val status = parseStepStatus(statusMarker)

                // Extract file links from step text
                val fileLinks = extractFileLinks(stepText)

                currentSteps.add(
                    PlanStep(
                        text = stepText,
                        status = status,
                        fileLinks = fileLinks
                    )
                )
                continue
            }

            // If we're inside an item but not a step, append to last step
            if (currentItem != null && currentSteps.isNotEmpty() && trimmed.startsWith("-")) {
                val lastStep = currentSteps.last()
                currentSteps[currentSteps.size - 1] = lastStep.copy(
                    text = "${lastStep.text} ${trimmed.removePrefix("-").trim()}"
                )
            }
        }

        // Save last item
        currentItem?.let {
            items.add(it.copy(steps = currentSteps.toList()))
        }

        return items
    }

    /**
     * Parse step status from markdown marker
     */
    private fun parseStepStatus(marker: String): StepStatus {
        return when (marker) {
            "✓", "x", "X" -> StepStatus.COMPLETED
            "!" -> StepStatus.FAILED
            "*" -> StepStatus.IN_PROGRESS
            else -> StepStatus.TODO
        }
    }

    /**
     * Extract file links from text using markdown link pattern
     */
    private fun extractFileLinks(text: String): List<FileLink> {
        val links = mutableListOf<FileLink>()
        val matches = FILE_LINK_PATTERN.findAll(text)

        matches.forEach { match ->
            val displayText = match.groupValues[1]
            val filePath = match.groupValues[2]
            links.add(FileLink(displayText, filePath))
        }

        return links
    }
}

