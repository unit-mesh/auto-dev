package cc.unitmesh.indexer.model

import kotlinx.serialization.Serializable

/**
 * Represents a semantic name extracted from code (filename, classname, method name, etc.)
 * Optimized for LLM context window usage with weight and grouping support.
 */
@Serializable
data class SemanticName(
    val name: String,                  // Normalized name (suffix removed)
    val type: ElementType,             // FILE, CLASS, METHOD, FIELD
    val tokens: Int,                   // Estimated token cost
    val source: String = "",           // Original source file/class
    val original: String = "",         // Original name before normalization
    val weight: Float = 0.5f,          // Importance weight [0.0, 1.0]
    val packageName: String = "",      // Package name for grouping
    val parentClassName: String = "",  // Parent class name for methods
    val weightCategory: String = ""    // Category: Critical, High, Medium, Low, Minimal
)

/**
 * Types of code elements that can be represented
 */
@Serializable
enum class ElementType {
    FILE, CLASS, METHOD, FIELD, INTERFACE, ENUM, PROPERTY, FUNCTION, UNKNOWN
}

/**
 * Represents a class with its methods aggregated
 * Used for structured LLM output
 */
@Serializable
data class ClassMethodGroup(
    val className: String,
    val methods: List<String>,
    val weight: Float = 0.5f,
    val packageName: String = ""
)
