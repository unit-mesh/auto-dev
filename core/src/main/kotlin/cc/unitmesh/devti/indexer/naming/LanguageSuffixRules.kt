package cc.unitmesh.devti.indexer.naming

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
