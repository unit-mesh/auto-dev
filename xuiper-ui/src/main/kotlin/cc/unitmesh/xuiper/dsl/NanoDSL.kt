package cc.unitmesh.xuiper.dsl

import cc.unitmesh.xuiper.ir.NanoIR
import cc.unitmesh.xuiper.ir.NanoIRConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * NanoDSL - Main entry point for parsing and converting NanoDSL
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
    private val parser = NanoDSLParser()
    
    private val json = Json {
        prettyPrint = true
        encodeDefaults = false
    }

    /**
     * Parse NanoDSL source to AST
     */
    fun parse(source: String): NanoNode.Component {
        return parser.parse(source)
    }

    /**
     * Parse NanoDSL source and convert to IR
     */
    fun toIR(source: String): NanoIR {
        val ast = parser.parse(source)
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

