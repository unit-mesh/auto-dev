package cc.unitmesh.devti.command

/**
 * Core edit logic for handling AI-generated code with existing code markers
 * This class is separated for better testability and reusability
 */
class EditApply {

    /**
     * Apply edit to original content using AI-generated code with markers
     */
    fun applyEdit(originalContent: String, codeEdit: String): String {
        val originalLines = originalContent.lines()
        val editLines = codeEdit.lines()

        // First, identify what lines are already defined in the edit (excluding markers)
        val editDefinedLines = editLines
            .filter { !isExistingCodeMarker(it.trim()) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        val result = mutableListOf<String>()
        var originalIndex = 0

        for ((editLineIndex, editLine) in editLines.withIndex()) {
            val trimmedEditLine = editLine.trim()

            // Check if this is an "existing code" marker
            if (isExistingCodeMarker(trimmedEditLine)) {
                // Find content to preserve from original that's not redefined in edit
                val preservedContent = findContentToPreserve(
                    originalLines,
                    originalIndex,
                    editDefinedLines,
                    editLines,
                    editLineIndex
                )
                result.addAll(preservedContent.lines)
                originalIndex = preservedContent.newOriginalIndex
            } else {
                // This is an actual edit line - add it
                result.add(editLine)

                // Skip the corresponding original line if it matches exactly
                if (originalIndex < originalLines.size &&
                    originalLines[originalIndex].trim() == trimmedEditLine
                ) {
                    originalIndex++
                }
            }
        }

        return result.joinToString("\n")
    }

    private data class PreservedContent(val lines: List<String>, val newOriginalIndex: Int)

    /**
     * Find content to preserve when encountering an existing code marker
     */
    private fun findContentToPreserve(
        originalLines: List<String>,
        currentOriginalIndex: Int,
        editDefinedLines: Set<String>,
        editLines: List<String>,
        currentEditIndex: Int
    ): PreservedContent {
        val preservedLines = mutableListOf<String>()
        var originalIndex = currentOriginalIndex

        // Find the next non-marker line in the edit to know the boundary
        val nextEditLineIndex = findNextNonMarkerLine(editLines, currentEditIndex + 1)
        val nextEditLine = if (nextEditLineIndex >= 0) editLines[nextEditLineIndex].trim() else null

        while (originalIndex < originalLines.size) {
            val originalLine = originalLines[originalIndex]
            val trimmedOriginalLine = originalLine.trim()

            // If we found the next edit line in the original, stop copying
            if (nextEditLine != null && trimmedOriginalLine == nextEditLine) {
                break
            }

            // Skip lines that are already redefined in the edit
            if (trimmedOriginalLine.isNotEmpty() && !editDefinedLines.contains(trimmedOriginalLine)) {
                preservedLines.add(originalLine)
            }

            originalIndex++
        }

        return PreservedContent(preservedLines, originalIndex)
    }

    /**
     * Check if a line is an existing code marker
     */
    fun isExistingCodeMarker(line: String): Boolean {
        if (!line.startsWith("//")) return false
        
        val lowerLine = line.lowercase()
        
        // Check for various patterns of existing code markers
        return lowerLine.contains("existing code") ||
                lowerLine.contains("... existing code ...") ||
                lowerLine.contains("existing getters and setters") ||
                lowerLine.contains("... existing getters and setters ...") ||
                lowerLine.contains("existing methods") ||
                lowerLine.contains("... existing methods ...") ||
                lowerLine.contains("existing fields") ||
                lowerLine.contains("... existing fields ...") ||
                lowerLine.contains("existing properties") ||
                lowerLine.contains("... existing properties ...") ||
                lowerLine.contains("existing constructors") ||
                lowerLine.contains("... existing constructors ...") ||
                lowerLine.contains("existing imports") ||
                lowerLine.contains("... existing imports ...") ||
                // Generic pattern for "... existing [something] ..."
                lowerLine.matches(Regex(""".*\.\.\.\s*existing\s+\w+.*\.\.\.""")) ||
                // Pattern for just "... existing ..."
                lowerLine.matches(Regex(""".*\.\.\.\s*existing\s*\.\.\."""))
    }

    private fun findNextNonMarkerLine(lines: List<String>, startIndex: Int): Int {
        for (i in startIndex until lines.size) {
            if (!isExistingCodeMarker(lines[i].trim())) {
                return i
            }
        }
        return -1
    }

    /**
     * Intelligently skip to the next relevant line based on the marker type
     */
    private fun skipToNextRelevantLine(
        originalLines: List<String>, 
        currentIndex: Int, 
        marker: String, 
        nextEditLine: String
    ): Int {
        val lowerMarker = marker.lowercase()
        var index = currentIndex
        
        // Try to find the next edit line by scanning ahead
        while (index < originalLines.size) {
            val originalLine = originalLines[index].trim()
            
            // If we find the next edit line, stop here
            if (originalLine == nextEditLine) {
                break
            }
            
            // For specific markers, try to skip intelligently
            when {
                lowerMarker.contains("getters and setters") -> {
                    // Skip getter/setter methods
                    if (isGetterOrSetter(originalLine)) {
                        index++
                        continue
                    }
                }
                lowerMarker.contains("methods") -> {
                    // Skip method definitions
                    if (isMethodDefinition(originalLine)) {
                        index = skipMethodBody(originalLines, index)
                        continue
                    }
                }
                lowerMarker.contains("fields") || lowerMarker.contains("properties") -> {
                    // Skip field/property declarations
                    if (isFieldOrProperty(originalLine)) {
                        index++
                        continue
                    }
                }
                lowerMarker.contains("imports") -> {
                    // Skip import statements
                    if (originalLine.startsWith("import ")) {
                        index++
                        continue
                    }
                }
            }
            
            // If we can't categorize this line, move to next
            index++
        }
        
        return index
    }

    private fun isGetterOrSetter(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.matches(Regex("""(public|private|protected)?\s*(get|set)\s*\w+\s*\(.*\).*""")) ||
                trimmed.matches(Regex("""(public|private|protected)?\s*\w+\s+(get|set)\s*\(.*\).*"""))
    }

    private fun isMethodDefinition(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.matches(Regex("""(public|private|protected|internal)?\s*(fun|override\s+fun)\s+\w+\s*\(.*\).*""")) ||
                trimmed.matches(Regex("""(public|private|protected)?\s*\w+\s+\w+\s*\(.*\).*"""))
    }

    private fun isFieldOrProperty(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.matches(Regex("""(public|private|protected|internal)?\s*(val|var)\s+\w+.*""")) ||
                trimmed.matches(Regex("""(public|private|protected)?\s*(static\s+)?\w+\s+\w+.*"""))
    }

    private fun skipMethodBody(originalLines: List<String>, startIndex: Int): Int {
        var index = startIndex + 1
        var braceCount = 0
        var foundOpenBrace = false
        
        while (index < originalLines.size) {
            val line = originalLines[index].trim()
            
            for (char in line) {
                when (char) {
                    '{' -> {
                        braceCount++
                        foundOpenBrace = true
                    }
                    '}' -> {
                        braceCount--
                        if (foundOpenBrace && braceCount == 0) {
                            return index + 1
                        }
                    }
                }
            }
            
            // If it's a single-line method (no braces), stop at the end of the line
            if (!foundOpenBrace && (line.endsWith(";") || line.endsWith("}"))) {
                return index + 1
            }
            
            index++
        }
        
        return index
    }
}
