package cc.unitmesh.kotlin.indexer.naming

import cc.unitmesh.devti.indexer.naming.LanguageSuffixRules

/**
 * Kotlin-specific suffix rules for generating optimal LLM context.
 * Removes Spring/common framework suffixes and Kotlin-specific technical patterns.
 * 
 * Key differences from Java:
 * - Includes Kotlin-specific suffixes like "Kt" (for file-level functions)
 * - Handles data classes, sealed classes, and object declarations
 * - Considers extension function patterns
 */
class KotlinNamingRules : LanguageSuffixRules {
    
    override val suffixMap: Map<String, String?> = linkedMapOf(
        // Framework and technical suffixes (ordered by length, longest first)
        "Controller" to null,
        "RestController" to null,
        "Service" to null,
        "Repository" to null,
        "Manager" to null,
        "Handler" to null,
        "Helper" to null,
        "Interceptor" to null,
        "Filter" to null,
        "Listener" to null,
        "Provider" to null,
        "Factory" to null,
        "Builder" to null,
        "Converter" to null,
        "Processor" to null,
        "Validator" to null,
        "Exception" to null,
        "Error" to null,
        
        // JPA/ORM
        "Entity" to null,
        
        // DTO/VO variants (data transfer objects)
        "DTO" to null,
        "Dto" to null,
        "VO" to null,
        "PO" to null,
        "DO" to null,
        "Request" to null,
        "Response" to null,
        
        // Configuration
        "Configuration" to "Config",
        "Config" to null,
        
        // Kotlin-specific suffixes
        "Kt" to null,  // Common suffix for Kotlin utility files
        "Extensions" to null,  // Extension function files
        "Ext" to null,
        
        // Common abbreviations
        "Impl" to null,
        "Utils" to null,
        "Util" to null,
        "Constants" to null,
        "Constant" to null
    )
}

