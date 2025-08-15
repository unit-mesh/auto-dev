package cc.unitmesh.diagram.parser

/**
 * Parser for HTML-like labels in Graphviz
 * Handles HTML tags like <b>, <br/>, <i>, etc.
 */
object HtmlLabelParser {
    
    /**
     * Parse HTML label and convert to plain text or structured format
     */
    fun parseHtmlLabel(htmlLabel: String): String {
        if (htmlLabel.isBlank()) return htmlLabel
        
        // Remove angle brackets if present (HTML labels are wrapped in < >)
        val cleanLabel = htmlLabel.trim().let { label ->
            if (label.startsWith("<") && label.endsWith(">")) {
                label.substring(1, label.length - 1)
            } else {
                label
            }
        }
        
        return convertHtmlToText(cleanLabel)
    }
    
    /**
     * Convert HTML tags to plain text representation
     */
    private fun convertHtmlToText(html: String): String {
        var result = html

        // Replace common HTML tags with text equivalents
        result = result.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        result = result.replace(Regex("<b>(.*?)</b>", RegexOption.IGNORE_CASE)) { matchResult ->
            "**${matchResult.groupValues[1]}**"
        }
        result = result.replace(Regex("<i>(.*?)</i>", RegexOption.IGNORE_CASE)) { matchResult ->
            "*${matchResult.groupValues[1]}*"
        }
        result = result.replace(Regex("<u>(.*?)</u>", RegexOption.IGNORE_CASE)) { matchResult ->
            "_${matchResult.groupValues[1]}_"
        }

        // Remove other HTML tags but keep content
        result = result.replace(Regex("<[^>]+>"), "")

        // Clean up extra whitespace but preserve newlines
        result = result.replace(Regex("[ \\t]+"), " ") // Replace multiple spaces/tabs with single space
        result = result.replace(Regex("\\n[ \\t]+"), "\n") // Remove spaces after newlines
        result = result.replace(Regex("[ \\t]+\\n"), "\n") // Remove spaces before newlines
        result = result.trim()

        return result
    }
    
    /**
     * Check if a label contains HTML tags
     */
    fun isHtmlLabel(label: String): Boolean {
        val trimmed = label.trim()
        return when {
            // Check if wrapped in angle brackets (Graphviz HTML label format)
            trimmed.startsWith("<") && trimmed.endsWith(">") -> true
            // Check if contains HTML tags anywhere in the string
            trimmed.contains(Regex("<[^>]+>")) -> true
            // Check for common HTML patterns even without angle brackets
            trimmed.contains(Regex("<(b|i|u|br|font|table|tr|td)\\b", RegexOption.IGNORE_CASE)) -> true
            else -> false
        }
    }
    
    /**
     * Extract structured information from HTML label
     */
    fun parseStructuredHtml(htmlLabel: String): HtmlLabelInfo {
        val cleanLabel = htmlLabel.trim().let { label ->
            if (label.startsWith("<") && label.endsWith(">")) {
                label.substring(1, label.length - 1)
            } else {
                label
            }
        }
        
        // Extract title (first bold text)
        val titleMatch = Regex("<b>(.*?)</b>", RegexOption.IGNORE_CASE).find(cleanLabel)
        val title = titleMatch?.groupValues?.get(1)?.trim()
        
        // Extract description (text after first <br/>)
        val parts = cleanLabel.split(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), limit = 2)
        val description = if (parts.size > 1) {
            convertHtmlToText(parts[1])
        } else null
        
        val plainText = convertHtmlToText(cleanLabel)
        
        return HtmlLabelInfo(
            title = title,
            description = description,
            plainText = plainText,
            originalHtml = htmlLabel
        )
    }
}

/**
 * Structured information extracted from HTML label
 */
data class HtmlLabelInfo(
    val title: String?,
    val description: String?,
    val plainText: String,
    val originalHtml: String
)
