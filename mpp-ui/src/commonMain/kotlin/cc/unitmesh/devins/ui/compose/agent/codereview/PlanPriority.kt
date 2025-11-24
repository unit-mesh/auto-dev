package cc.unitmesh.devins.ui.compose.agent.codereview

/**
 * Priority-related utilities for plan items
 * Handles priority adjustment logic
 * 
 * This is a pure function utility that can be easily tested without Compose dependencies
 */
object PlanPriority {
    /**
     * Keywords that indicate code style or formatting issues
     * These should always be treated as MEDIUM priority
     */
    private val CODE_STYLE_KEYWORDS = setOf(
        "代码风格", "Code Style",
        "字符串格式化", "String Formatting",
        "格式化", "Formatting",
        "风格", "Style"
    )
    
    /**
     * Check if a title indicates a code style issue
     */
    fun isCodeStyleIssue(title: String): Boolean {
        return CODE_STYLE_KEYWORDS.any { keyword ->
            title.contains(keyword, ignoreCase = true)
        }
    }
    
    /**
     * Adjust priority based on issue category
     * Code style and formatting issues are always downgraded to MEDIUM
     */
    fun adjustPriority(title: String, priority: String): String {
        return if (isCodeStyleIssue(title) && isHighPriority(priority)) {
            "MEDIUM"
        } else {
            priority
        }
    }
    
    /**
     * Check if priority is high (CRITICAL or HIGH)
     */
    fun isHighPriority(priority: String): Boolean {
        return priority.contains("关键", ignoreCase = true) ||
               priority.contains("CRITICAL", ignoreCase = true) ||
               priority.contains("高", ignoreCase = true) ||
               priority.contains("HIGH", ignoreCase = true)
    }
    
    /**
     * Get priority info (adjusted priority) for a plan item
     * Color resolution is done in Compose context
     */
    fun getAdjustedPriority(item: PlanItem): String {
        return adjustPriority(item.title, item.priority)
    }
}

