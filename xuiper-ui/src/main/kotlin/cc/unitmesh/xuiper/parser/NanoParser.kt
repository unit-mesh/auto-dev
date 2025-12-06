package cc.unitmesh.xuiper.parser

import cc.unitmesh.xuiper.ast.NanoNode
import cc.unitmesh.xuiper.spec.NanoSpec

/**
 * NanoParser - Parser interface for NanoDSL
 * 
 * Abstraction layer for parsing NanoDSL source code.
 * Different parser implementations can be plugged in:
 * - IndentParser: Python-style indentation (current)
 * - BraceParser: C-style braces (future)
 * - JsonParser: Direct JSON input (future)
 * 
 * When LLM capabilities change:
 * 1. Create new parser implementation
 * 2. Configure which parser to use via spec
 * 3. Old parsers remain for backward compatibility
 */
interface NanoParser {
    /** The spec this parser follows */
    val spec: NanoSpec
    
    /**
     * Parse source code into AST
     */
    fun parse(source: String): ParseResult
    
    /**
     * Validate source without full parsing
     */
    fun validate(source: String): ValidationResult
}

/**
 * Parse result - either success with AST or failure with errors
 */
sealed class ParseResult {
    data class Success(val ast: NanoNode.Component) : ParseResult()
    data class Failure(val errors: List<ParseError>) : ParseResult()
    
    fun getOrNull(): NanoNode.Component? = (this as? Success)?.ast
    fun getOrThrow(): NanoNode.Component = when (this) {
        is Success -> ast
        is Failure -> throw ParseException(errors.first().message)
    }
}

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ParseError> = emptyList(),
    val warnings: List<ParseWarning> = emptyList()
)

/**
 * Parse error with location information
 */
data class ParseError(
    val message: String,
    val line: Int,
    val column: Int = 0,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

/**
 * Parse warning
 */
data class ParseWarning(
    val message: String,
    val line: Int,
    val suggestion: String? = null
)

/**
 * Error severity levels
 */
enum class ErrorSeverity {
    ERROR,      // Fatal, cannot continue
    WARNING,    // Non-fatal, but should be fixed
    INFO        // Informational
}

/**
 * Parse exception
 */
class ParseException(message: String) : Exception(message)

/**
 * Parser factory - creates parser based on spec version
 */
object NanoParserFactory {
    private val parsers = mutableMapOf<String, () -> NanoParser>()
    
    fun register(version: String, factory: () -> NanoParser) {
        parsers[version] = factory
    }
    
    fun create(spec: NanoSpec): NanoParser {
        val factory = parsers[spec.version]
            ?: throw IllegalArgumentException("No parser registered for spec version: ${spec.version}")
        return factory()
    }
    
    fun createDefault(): NanoParser {
        // Import here to avoid circular dependency
        return cc.unitmesh.xuiper.parser.IndentParser()
    }
}

