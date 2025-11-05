package cc.unitmesh.indexer.naming

/**
 * Defines suffix removal rules for a programming language.
 * Minimizes redundant technical terms in LLM context, e.g., removing "Controller", "Service", "DTO"
 */
interface LanguageSuffixRules {
    
    /**
     * Get the list of suffixes to remove/replace, ordered by priority (longest first).
     * Returns a map of: suffix -> replacement (null means remove)
     * Example for Java: "Controller" -> null, "DTO" -> null, "Manager" -> null
     */
    val suffixMap: Map<String, String?>
    
    /**
     * Normalize a name by removing suffixes.
     * "UserController" -> "User"
     * "BlogService" -> "Blog"
     */
    fun normalize(name: String): String {
        var result = name
        // Check suffixes in order (longest first for correctness)
        for ((suffix, replacement) in suffixMap) {
            if (result.endsWith(suffix)) {
                result = if (replacement != null) {
                    result.dropLast(suffix.length) + replacement
                } else {
                    result.dropLast(suffix.length)
                }
                return result
            }
        }
        return result
    }
}

/**
 * Common suffix rules that work across multiple languages
 */
class CommonSuffixRules : LanguageSuffixRules {
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
        
        // Data transfer objects
        "DTO" to null,
        "Dto" to null,
        "VO" to null,
        "PO" to null,
        "Entity" to null,
        "Model" to null,
        "Request" to null,
        "Response" to null,
        "Config" to null,
        "Configuration" to null,
        
        // Common technical suffixes
        "Utils" to null,
        "Util" to null,
        "Test" to null,
        "Tests" to null,
        "Spec" to null,
        "Specs" to null,
        "Mock" to null,
        "Stub" to null,
        "Impl" to null,
        "Implementation" to null
    )
}
