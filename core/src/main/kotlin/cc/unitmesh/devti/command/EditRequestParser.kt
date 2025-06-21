package cc.unitmesh.devti.command

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/**
 * Parser for EditRequest objects from various text formats
 * Supports YAML, advanced format, and legacy format parsing
 */
class  EditRequestParser {
    
    /**
     * Parse content into EditRequest, trying multiple formats
     * @param content The content to parse
     * @return EditRequest if parsing succeeds, null if all formats fail
     * @throws ParseException if parsing fails with specific error information
     */
    fun parse(content: String): EditRequest? {
        if (content.isBlank()) {
            return null
        }
        
        val errors = mutableListOf<ParseException>()
        
        // Try YAML format first
        try {
            parseAsYaml(content)?.let { return it }
        } catch (e: ParseException) {
            errors.add(e)
        }
        
        // Try advanced format
        try {
            parseAsAdvancedFormat(content)?.let { return it }
        } catch (e: ParseException) {
            errors.add(e)
        }
        
        // Try legacy format
        try {
            parseAsLegacyFormat(content)?.let { return it }
        } catch (e: ParseException) {
            errors.add(e)
        }
        
        // If we have specific errors, we can log them for debugging
        // For now, return null to maintain backward compatibility
        return null
    }
    
    /**
     * Parse content as YAML format
     */
    fun parseAsYaml(content: String): EditRequest? {
        return try {
            val yaml = Yaml(SafeConstructor(LoaderOptions()))
            val data = yaml.load<Map<String, Any>>(content) 
                ?: throw ParseException.YamlParseException("YAML content is null or empty")

            val targetFile = data["target_file"] as? String 
                ?: throw ParseException.MissingFieldException("target_file")
            
            val instructions = data["instructions"] as? String ?: ""
            
            val codeEdit = data["code_edit"] as? String
                ?: throw ParseException.MissingFieldException("code_edit")

            // Process escape sequences for YAML quoted strings
            val processedCodeEdit = processEscapeSequences(codeEdit)

            validateEditRequest(targetFile, processedCodeEdit)

            EditRequest(
                targetFile = targetFile,
                instructions = instructions,
                codeEdit = processedCodeEdit
            )
        } catch (e: ParseException) {
            throw e
        } catch (e: Exception) {
            throw ParseException.YamlParseException("Failed to parse YAML: ${e.message}", e)
        }
    }
    
    /**
     * Parse content as advanced format (YAML-like with regex)
     */
    fun parseAsAdvancedFormat(content: String): EditRequest? {
        return try {
            val targetFileRegex = """target_file\s*:\s*["']?([^"'\n]+)["']?""".toRegex()
            val instructionsRegex = """instructions\s*:\s*["']?([^"'\n]*?)["']?""".toRegex()
            val blockScalarPattern = """code_edit\s*:\s*\|\s*\n(.*?)(?=\n\S|\n*$)""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val quotedStringPattern = """code_edit\s*:\s*["'](.*?)["']""".toRegex(RegexOption.DOT_MATCHES_ALL)

            val targetFileMatch = targetFileRegex.find(content)
                ?: throw ParseException.MissingFieldException("target_file")
            
            val instructionsMatch = instructionsRegex.find(content)
            val codeEditMatch = blockScalarPattern.find(content) ?: quotedStringPattern.find(content)
                ?: throw ParseException.MissingFieldException("code_edit")

            val codeEditContent = if (blockScalarPattern.matches(codeEditMatch.value)) {
                codeEditMatch.groupValues[1].trimEnd()
            } else {
                // Handle quoted string - process escape sequences
                processEscapeSequences(codeEditMatch.groupValues[1])
            }

            val targetFile = targetFileMatch.groupValues[1].trim()
            val instructions = instructionsMatch?.groupValues?.get(1)?.trim() ?: ""
            
            validateEditRequest(targetFile, codeEditContent)

            EditRequest(
                targetFile = targetFile,
                instructions = instructions,
                codeEdit = codeEditContent
            )
        } catch (e: ParseException) {
            throw e
        } catch (e: Exception) {
            throw ParseException.RegexParseException("Failed to parse advanced format: ${e.message}", e)
        }
    }
    
    /**
     * Parse content as legacy format
     */
    fun parseAsLegacyFormat(content: String): EditRequest? {
        return try {
            val targetFileRegex = """target_file["\s]*[:=]["\s]*["']([^"']+)["']""".toRegex()
            val instructionsRegex = """instructions["\s]*[:=]["\s]*["']([^"']*?)["']""".toRegex(RegexOption.DOT_MATCHES_ALL)

            val targetFileMatch = targetFileRegex.find(content)
                ?: throw ParseException.MissingFieldException("target_file")
            
            val instructionsMatch = instructionsRegex.find(content)
            val codeEditContent = extractCodeEditContent(content)
                ?: throw ParseException.MissingFieldException("code_edit")

            val targetFile = targetFileMatch.groupValues[1]
            val instructions = instructionsMatch?.groupValues?.get(1) ?: ""
            
            validateEditRequest(targetFile, codeEditContent)

            EditRequest(
                targetFile = targetFile,
                instructions = instructions,
                codeEdit = codeEditContent
            )
        } catch (e: ParseException) {
            throw e
        } catch (e: Exception) {
            throw ParseException.RegexParseException("Failed to parse legacy format: ${e.message}", e)
        }
    }
    
    /**
     * Extract code_edit content from legacy format, handling nested quotes
     */
    private fun extractCodeEditContent(content: String): String? {
        return try {
            val codeEditStart = """code_edit["\s]*[:=]["\s]*["']""".toRegex().find(content) ?: return null
            val startIndex = codeEditStart.range.last + 1

            if (startIndex >= content.length) return null

            val openingQuote = content[startIndex - 1]
            var index = startIndex
            var escapeNext = false

            while (index < content.length) {
                val char = content[index]

                if (escapeNext) {
                    escapeNext = false
                } else if (char == '\\') {
                    escapeNext = true
                } else if (char == openingQuote) {
                    val extractedContent = content.substring(startIndex, index)
                    return processEscapeSequences(extractedContent)
                }

                index++
            }

            throw ParseException.QuoteParseException("Unclosed quote in code_edit field")
        } catch (e: ParseException) {
            throw e
        } catch (e: Exception) {
            throw ParseException.QuoteParseException("Failed to extract code_edit content: ${e.message}", e)
        }
    }
    
    /**
     * Process escape sequences in strings
     */
    private fun processEscapeSequences(content: String): String {
        return content
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\\\", "\\")
    }
    
    /**
     * Validate the parsed EditRequest fields
     */
    private fun validateEditRequest(targetFile: String, codeEdit: String) {
        if (targetFile.isBlank()) {
            throw ParseException.InvalidFieldException("target_file", targetFile, "cannot be blank")
        }
        
        if (codeEdit.isBlank()) {
            throw ParseException.InvalidFieldException("code_edit", codeEdit, "cannot be blank")
        }
        
        // Additional validation can be added here
        if (targetFile.contains("..")) {
            throw ParseException.InvalidFieldException("target_file", targetFile, "path traversal not allowed")
        }
    }
}
