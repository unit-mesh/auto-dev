package cc.unitmesh.devti.indexer.model

/**
 * Represents a semantic name extracted from code (filename, classname, method name, etc.)
 * Optimized for LLM context window usage.
 */
data class SemanticName(
    val name: String,          // Normalized name (suffix removed)
    val type: ElementType,     // FILE, CLASS, METHOD, FIELD
    val tokens: Int,           // Estimated token cost
    val source: String = "",   // Original source file/class
    val original: String = ""  // Original name before normalization
)

enum class ElementType {
    FILE, CLASS, METHOD, FIELD
}

/**
 * Collection of semantic names organized by level.
 * Used for generating LLM context with progressive detail.
 */
data class DomainDictionary(
    val level1: List<SemanticName>,  // Filenames only
    val level2: List<SemanticName>,  // Classes and public methods
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Get all names as a simple list for LLM prompt (Level 1 + Level 2)
     */
    fun getAllNames(): List<String> {
        return (level1 + level2)
            .map { it.name }
            .distinct()  // Remove duplicates
    }
    
    /**
     * Get all names with their types for debugging
     */
    fun getAllNamesWithType(): List<String> {
        return (level1 + level2)
            .distinctBy { it.name }
            .map { "${it.name} (${it.type})" }
    }
    
    /**
     * Get total token cost estimation
     */
    fun getTotalTokens(): Int {
        return (level1 + level2).sumOf { it.tokens }
    }
    
    /**
     * Format as CSV string for LLM consumption
     * Format: name, type, source, original_name
     */
    fun toCsvFormat(): String {
        val header = "名称,类型,来源,原始名称,Token数\n"
        val rows = (level1 + level2)
            .distinctBy { it.name }
            .map { name ->
                "${name.name},${name.type},${name.source},${name.original},${name.tokens}"
            }
        return header + rows.joinToString("\n")
    }
    
    /**
     * Format as simple comma-separated list (backward compatible)
     */
    fun toSimpleList(): String {
        return getAllNames().joinToString(", ")
    }
}
