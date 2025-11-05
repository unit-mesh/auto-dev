package cc.unitmesh.indexer.naming

/**
 * Utility for splitting camelCase and PascalCase names into individual words.
 * Useful for extracting meaningful terms from class names and method names.
 */
object CamelCaseSplitter {
    
    /**
     * Split a camelCase or PascalCase string into individual words.
     * Examples:
     * - "UserController" -> ["User", "Controller"]
     * - "getUserById" -> ["get", "User", "By", "Id"]
     * - "XMLHttpRequest" -> ["XML", "Http", "Request"]
     * - "iPhone" -> ["i", "Phone"]
     */
    fun split(input: String): List<String> {
        if (input.isEmpty()) return emptyList()
        
        val words = mutableListOf<String>()
        var currentWord = StringBuilder()
        var lastWasUpper = false
        var lastWasDigit = false
        
        for (i in input.indices) {
            val char = input[i]
            val isUpper = char.isUpperCase()
            val isDigit = char.isDigit()
            val isLetter = char.isLetter()
            
            when {
                // Start of a new word if:
                // 1. Current char is uppercase and last was lowercase
                // 2. Current char is lowercase and last was uppercase (for acronyms like "XMLHttp")
                // 3. Current char is digit and last was letter
                // 4. Current char is letter and last was digit
                (isUpper && !lastWasUpper && currentWord.isNotEmpty()) ||
                (!isUpper && lastWasUpper && currentWord.length > 1) ||
                (isDigit && !lastWasDigit && currentWord.isNotEmpty()) ||
                (isLetter && lastWasDigit && currentWord.isNotEmpty()) -> {
                    
                    // For acronyms, keep the last uppercase letter with the new word
                    if (!isUpper && lastWasUpper && currentWord.length > 1) {
                        val lastChar = currentWord.last()
                        currentWord.setLength(currentWord.length - 1)
                        if (currentWord.isNotEmpty()) {
                            words.add(currentWord.toString())
                        }
                        currentWord = StringBuilder().append(lastChar).append(char)
                    } else {
                        if (currentWord.isNotEmpty()) {
                            words.add(currentWord.toString())
                        }
                        currentWord = StringBuilder().append(char)
                    }
                }
                
                // Continue current word
                else -> {
                    currentWord.append(char)
                }
            }
            
            lastWasUpper = isUpper
            lastWasDigit = isDigit
        }
        
        // Add the last word
        if (currentWord.isNotEmpty()) {
            words.add(currentWord.toString())
        }
        
        return words.filter { it.isNotEmpty() }
    }
    
    /**
     * Split and filter out common technical words
     */
    fun splitAndFilter(input: String, suffixRules: LanguageSuffixRules? = null): List<String> {
        val words = split(input)
        val filtered = words.filter { word ->
            // Filter out single characters and common technical words
            word.length > 1 && !isCommonTechnicalWord(word)
        }
        
        // Apply suffix rules if provided
        return if (suffixRules != null) {
            filtered.map { suffixRules.normalize(it) }.filter { it.isNotEmpty() }
        } else {
            filtered
        }
    }
    
    private fun isCommonTechnicalWord(word: String): Boolean {
        val lowerWord = word.lowercase()
        return lowerWord in setOf(
            "get", "set", "is", "has", "can", "should", "will", "do", "make", "create",
            "update", "delete", "find", "search", "list", "add", "remove", "check",
            "validate", "convert", "parse", "format", "encode", "decode", "serialize",
            "deserialize", "load", "save", "read", "write", "open", "close", "start",
            "stop", "run", "execute", "process", "handle", "manage", "build", "init",
            "destroy", "clear", "reset", "refresh", "reload", "sync", "async", "await",
            "by", "id", "to", "from", "with", "without", "for", "of", "in", "on", "at"
        )
    }
}
