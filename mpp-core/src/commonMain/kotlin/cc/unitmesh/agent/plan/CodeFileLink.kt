package cc.unitmesh.agent.plan

import kotlinx.serialization.Serializable

/**
 * Represents a link to a code file in a plan step.
 * Format in markdown: [DisplayText](filepath)
 * 
 * Example: [Main.java](src/main/java/com/example/Main.java)
 */
@Serializable
data class CodeFileLink(
    /**
     * The display text shown in the link
     */
    val displayText: String,
    
    /**
     * The file path (relative or absolute)
     */
    val filePath: String
) {
    /**
     * Convert to markdown link format
     */
    fun toMarkdown(): String = "[$displayText]($filePath)"
    
    companion object {
        private val LINK_PATTERN = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")
        
        /**
         * Extract all code file links from text
         */
        fun extractFromText(text: String): List<CodeFileLink> {
            return LINK_PATTERN.findAll(text).map { match ->
                CodeFileLink(
                    displayText = match.groupValues[1],
                    filePath = match.groupValues[2]
                )
            }.toList()
        }
    }
}

