package cc.unitmesh.codegraph.model

import kotlinx.serialization.Serializable

/**
 * Represents a code element node in the AST.
 * This is the common model shared across JVM and JS platforms.
 */
@Serializable
data class CodeNode(
    /**
     * Unique identifier for the node
     */
    val id: String,
    
    /**
     * Type of the code element (e.g., "class", "method", "interface")
     */
    val type: CodeElementType,
    
    /**
     * Name of the code element
     */
    val name: String,
    
    /**
     * Package or namespace this element belongs to
     */
    val packageName: String,
    
    /**
     * File path where this element is defined
     */
    val filePath: String,
    
    /**
     * Start line number (1-based)
     */
    val startLine: Int,
    
    /**
     * End line number (1-based)
     */
    val endLine: Int,
    
    /**
     * Start column (0-based)
     */
    val startColumn: Int,
    
    /**
     * End column (0-based)
     */
    val endColumn: Int,
    
    /**
     * Full qualified name (e.g., "com.example.MyClass.myMethod")
     */
    val qualifiedName: String,
    
    /**
     * Content of the code element
     */
    val content: String,
    
    /**
     * Additional metadata
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Types of code elements that can be represented in the AST
 */
@Serializable
enum class CodeElementType {
    PACKAGE,
    CLASS,
    INTERFACE,
    ENUM,
    CONSTRUCTOR,
    METHOD,
    FUNCTION,
    FIELD,
    PROPERTY,
    PARAMETER,
    VARIABLE,
    ANNOTATION,
    IMPORT,
    UNKNOWN
}

