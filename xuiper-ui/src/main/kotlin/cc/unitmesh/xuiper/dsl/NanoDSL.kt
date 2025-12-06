package cc.unitmesh.xuiper.dsl

import cc.unitmesh.xuiper.ast.NanoNode
import cc.unitmesh.xuiper.ir.NanoIR
import cc.unitmesh.xuiper.ir.NanoIRConverter
import cc.unitmesh.xuiper.parser.IndentParser
import cc.unitmesh.xuiper.parser.NanoParser
import cc.unitmesh.xuiper.parser.ParseResult
import cc.unitmesh.xuiper.parser.ValidationResult
import cc.unitmesh.xuiper.spec.NanoSpec
import cc.unitmesh.xuiper.spec.v1.NanoSpecV1
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * NanoDSL - Main entry point for parsing and converting NanoDSL
 *
 * This is the facade for all NanoDSL operations.
 * Configure via spec and parser for different behaviors.
 *
 * Usage:
 * ```kotlin
 * val source = """
 *     component GreetingCard:
 *         Card:
 *             Text("Hello!", style="h2")
 * """
 *
 * // Parse to AST
 * val ast = NanoDSL.parse(source)
 *
 * // Convert to JSON IR
 * val ir = NanoDSL.toIR(source)
 *
 * // Get JSON string
 * val json = NanoDSL.toJson(source)
 * ```
 */
object NanoDSL {
    /** Current specification */
    var spec: NanoSpec = NanoSpecV1
        private set

    /** Current parser */
    var parser: NanoParser = IndentParser(spec)
        private set

    private val json = Json {
        prettyPrint = true
        encodeDefaults = false
    }

    /**
     * Configure NanoDSL with a different spec and parser
     */
    fun configure(spec: NanoSpec, parser: NanoParser? = null) {
        this.spec = spec
        this.parser = parser ?: IndentParser(spec)
    }

    /**
     * Parse NanoDSL source to AST
     */
    fun parse(source: String): NanoNode.Component {
        return when (val result = parser.parse(source)) {
            is ParseResult.Success -> result.ast
            is ParseResult.Failure -> throw cc.unitmesh.xuiper.parser.ParseException(
                result.errors.firstOrNull()?.message ?: "Parse failed"
            )
        }
    }

    /**
     * Parse with full result (success or failure with errors)
     */
    fun parseResult(source: String): ParseResult {
        return parser.parse(source)
    }

    /**
     * Validate source without full parsing
     */
    fun validate(source: String): ValidationResult {
        return parser.validate(source)
    }

    /**
     * Parse NanoDSL source and convert to IR
     */
    fun toIR(source: String): NanoIR {
        val ast = parse(source)
        return NanoIRConverter.convert(ast)
    }

    /**
     * Convert NanoNode AST to IR
     */
    fun toIR(node: NanoNode): NanoIR {
        return NanoIRConverter.convert(node)
    }

    /**
     * Parse NanoDSL source and convert to JSON string
     */
    fun toJson(source: String, prettyPrint: Boolean = true): String {
        val ir = toIR(source)
        return if (prettyPrint) {
            json.encodeToString(ir)
        } else {
            Json.encodeToString(ir)
        }
    }

    /**
     * Convert NanoIR to JSON string
     */
    fun toJson(ir: NanoIR, prettyPrint: Boolean = true): String {
        return if (prettyPrint) {
            json.encodeToString(ir)
        } else {
            Json.encodeToString(ir)
        }
    }

    /**
     * Parse JSON string back to NanoIR
     */
    fun fromJson(jsonString: String): NanoIR {
        return json.decodeFromString(jsonString)
    }
}

