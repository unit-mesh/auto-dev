package cc.unitmesh.agent.util

/**
 * Utility for extracting walkthrough content from analysis output.
 * 
 * Walkthrough sections are marked with HTML comments:
 * - Start: `<!-- walkthrough_start -->`
 * - End: `<!-- walkthrough_end -->`
 * 
 * This extractor handles:
 * - Single walkthrough section
 * - Multiple walkthrough sections (concatenated)
 * - Incomplete sections (missing end marker)
 * - Missing sections (returns empty string)
 */
object WalkthroughExtractor {
    private val walkthroughStartRegex = Regex("<!--\\s*walkthrough_start\\s*-->")
    private val walkthroughEndRegex = Regex("<!--\\s*walkthrough_end\\s*-->")

    /**
     * Extract walkthrough content from analysis output.
     * 
     * @param analysisOutput The full analysis output containing walkthrough markers
     * @return Extracted walkthrough content (trimmed), or empty string if no walkthrough found
     * 
     * Examples:
     * ```
     * <!-- walkthrough_start -->
     * This is the walkthrough content
     * <!-- walkthrough_end -->
     * ```
     * Returns: "This is the walkthrough content"
     * 
     * Multiple sections:
     * ```
     * <!-- walkthrough_start -->
     * Section 1
     * <!-- walkthrough_end -->
     * Some other content
     * <!-- walkthrough_start -->
     * Section 2
     * <!-- walkthrough_end -->
     * ```
     * Returns: "Section 1\n\nSection 2"
     */
    fun extract(analysisOutput: String): String {
        if (analysisOutput.isBlank()) {
            return ""
        }

        val walkthroughSections = mutableListOf<String>()
        var currentIndex = 0

        // Find all walkthrough sections
        while (currentIndex < analysisOutput.length) {
            val startMatch = walkthroughStartRegex.find(analysisOutput, currentIndex)
            if (startMatch == null) {
                break
            }

            val endMatch = walkthroughEndRegex.find(analysisOutput, startMatch.range.last + 1)
            
            val walkthroughContent = if (endMatch != null) {
                // Complete section: extract content between markers
                analysisOutput.substring(startMatch.range.last + 1, endMatch.range.first).trim()
            } else {
                // Incomplete section: extract everything after start marker
                analysisOutput.substring(startMatch.range.last + 1).trim()
            }

            if (walkthroughContent.isNotBlank()) {
                walkthroughSections.add(walkthroughContent)
            }

            // Move to next section
            currentIndex = if (endMatch != null) {
                endMatch.range.last + 1
            } else {
                analysisOutput.length
            }
        }

        // Concatenate all sections with double newline separator
        return walkthroughSections.joinToString("\n\n")
    }

    /**
     * Check if the analysis output contains any walkthrough markers.
     * 
     * @param analysisOutput The analysis output to check
     * @return true if at least one walkthrough_start marker is found
     */
    fun hasWalkthrough(analysisOutput: String): Boolean {
        return walkthroughStartRegex.find(analysisOutput) != null
    }

    /**
     * Extract and validate walkthrough content.
     * Returns null if walkthrough is incomplete or invalid.
     * 
     * @param analysisOutput The analysis output containing walkthrough markers
     * @return Extracted walkthrough content if complete, null otherwise
     */
    fun extractComplete(analysisOutput: String): String? {
        if (analysisOutput.isBlank()) {
            return null
        }

        val startMatch = walkthroughStartRegex.find(analysisOutput) ?: return null
        val endMatch = walkthroughEndRegex.find(analysisOutput, startMatch.range.last + 1) ?: return null

        val content = analysisOutput.substring(startMatch.range.last + 1, endMatch.range.first).trim()
        return if (content.isNotBlank()) content else null
    }
}
