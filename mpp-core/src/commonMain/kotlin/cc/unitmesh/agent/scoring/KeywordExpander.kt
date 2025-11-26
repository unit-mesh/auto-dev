package cc.unitmesh.agent.scoring

import cc.unitmesh.indexer.naming.CamelCaseSplitter

/**
 * Configuration for keyword expansion behavior.
 */
data class KeywordExpanderConfig(
    /** Minimum results threshold - expand keywords if results below this */
    val minResultsThreshold: Int = 3,
    /** Maximum results threshold - filter with secondary keywords if above this */
    val maxResultsThreshold: Int = 50,
    /** Ideal results range - no expansion needed */
    val idealMinResults: Int = 5,
    val idealMaxResults: Int = 30,
    /** Whether to include stemmed variants (encode -> encoded, encoder, etc.) */
    val enableStemming: Boolean = true,
    /** Maximum number of Level 3 (tertiary) keywords to generate */
    val maxTertiaryKeywords: Int = 6
)

/**
 * Result of keyword expansion containing all levels.
 */
data class ExpandedKeywords(
    /** Original query */
    val original: String,
    /** Level 1: Primary keywords (most specific, includes original + phrase variations) */
    val primary: List<String>,
    /** Level 2: Secondary keywords (component words from splitting) */
    val secondary: List<String>,
    /** Level 3: Tertiary keywords (stems and variants, broadest) */
    val tertiary: List<String>
) {
    /**
     * Get keywords at the specified level.
     */
    fun atLevel(level: Int): List<String> = when (level) {
        1 -> primary
        2 -> secondary
        3 -> tertiary
        else -> emptyList()
    }
    
    /**
     * Get all keywords up to and including the specified level.
     */
    fun upToLevel(level: Int): List<String> = when {
        level >= 3 -> (primary + secondary + tertiary).distinct()
        level >= 2 -> (primary + secondary).distinct()
        level >= 1 -> primary
        else -> emptyList()
    }
}

/**
 * Multi-level keyword expansion for adaptive search.
 * 
 * Generates hierarchical keywords from specific to broad:
 * 
 * ## Keyword Levels
 * 
 * **Level 1 (Primary)**: Most specific keywords
 * - Original query phrase
 * - Phrase variations (e.g., "base64 encoding" → "base64 encoder", "base64 encode")
 * 
 * **Level 2 (Secondary)**: Component words
 * - Individual words from the phrase (e.g., "base64", "encoding")
 * - CamelCase split components (e.g., "AuthService" → "Auth", "Service")
 * 
 * **Level 3 (Tertiary)**: Stem variants and related terms
 * - Verb forms (encode → encoded, encoding, encoder)
 * - Common abbreviation expansions
 * 
 * ## Usage Strategy
 * 
 * ```kotlin
 * val expander = KeywordExpander()
 * val expanded = expander.expand("base64 encoding")
 * 
 * // Level 1: ["base64 encoding", "base64 encoder", "base64 encode"]
 * // Level 2: ["base64", "encoding"]
 * // Level 3: ["encode", "encoded", "encoder", "encoders"]
 * 
 * // Adaptive search:
 * // 1. Search with Level 1 keywords
 * // 2. If too few results (< minThreshold), expand to Level 2
 * // 3. If too many results (> maxThreshold), filter using Level 2
 * ```
 * 
 * @see ExpandedKeywords
 * @see KeywordExpanderConfig
 */
class KeywordExpander(
    private val config: KeywordExpanderConfig = KeywordExpanderConfig()
) {
    
    /**
     * Expand a query into multi-level keywords.
     */
    fun expand(query: String): ExpandedKeywords {
        val normalized = query.trim()
        if (normalized.isEmpty()) {
            return ExpandedKeywords(query, emptyList(), emptyList(), emptyList())
        }
        
        val primary = generatePrimaryKeywords(normalized)
        val secondary = generateSecondaryKeywords(normalized)
        val tertiary = if (config.enableStemming) {
            generateTertiaryKeywords(normalized, secondary)
        } else {
            emptyList()
        }
        
        return ExpandedKeywords(
            original = normalized,
            primary = primary.distinct(),
            secondary = secondary.distinct().filter { it !in primary },
            tertiary = tertiary.distinct().filter { it !in primary && it !in secondary }
        )
    }
    
    /**
     * Expand query with an optional user-provided secondary keyword.
     */
    fun expandWithHint(query: String, secondaryHint: String?): ExpandedKeywords {
        val base = expand(query)
        
        return if (secondaryHint.isNullOrBlank()) {
            base
        } else {
            // Insert the user hint at the start of secondary keywords
            val hintExpanded = expand(secondaryHint)
            base.copy(
                secondary = (listOf(secondaryHint.trim()) + hintExpanded.primary + base.secondary).distinct(),
                tertiary = (hintExpanded.secondary + base.tertiary).distinct()
            )
        }
    }
    
    /**
     * Determine the recommended search strategy based on result count.
     */
    fun recommendStrategy(resultCount: Int, currentLevel: Int): SearchStrategy {
        return when {
            resultCount == 0 && currentLevel < 3 -> SearchStrategy.EXPAND
            resultCount < config.minResultsThreshold && currentLevel < 3 -> SearchStrategy.EXPAND
            resultCount > config.maxResultsThreshold -> SearchStrategy.FILTER
            resultCount in config.idealMinResults..config.idealMaxResults -> SearchStrategy.KEEP
            else -> SearchStrategy.KEEP
        }
    }
    
    // ==================== Primary Keywords (Level 1) ====================
    
    private fun generatePrimaryKeywords(query: String): List<String> {
        val keywords = mutableListOf(query)
        
        // Split by spaces for phrase processing
        val words = query.split(Regex("\\s+")).filter { it.isNotEmpty() }
        
        if (words.size >= 2) {
            // Generate phrase variations
            val lastWord = words.last()
            val prefix = words.dropLast(1).joinToString(" ")
            
            // Common verb/noun form variations
            val variations = generateWordVariations(lastWord)
            variations.forEach { variant ->
                if (variant != lastWord) {
                    keywords.add("$prefix $variant")
                }
            }
        } else if (words.size == 1) {
            // Single word - add variations directly
            val variations = generateWordVariations(words.first())
            keywords.addAll(variations)
            
            // Also split camelCase and create phrase
            val camelParts = CamelCaseSplitter.split(words.first())
            if (camelParts.size > 1) {
                keywords.add(camelParts.joinToString(" ") { it.lowercase() })
            }
        }
        
        return keywords.filter { it.isNotBlank() }
    }
    
    // ==================== Secondary Keywords (Level 2) ====================
    
    private fun generateSecondaryKeywords(query: String): List<String> {
        val keywords = mutableListOf<String>()
        
        // Split by spaces
        val words = query.split(Regex("\\s+")).filter { it.isNotEmpty() }
        
        // Add individual words
        keywords.addAll(words)
        
        // Split camelCase/PascalCase words
        words.forEach { word ->
            val parts = CamelCaseSplitter.split(word)
            if (parts.size > 1) {
                keywords.addAll(parts.map { it.lowercase() })
            }
        }
        
        // Handle common separators (base_64, base-64)
        words.forEach { word ->
            if (word.contains('_') || word.contains('-')) {
                keywords.addAll(
                    word.split(Regex("[_\\-]"))
                        .filter { it.isNotEmpty() }
                        .map { it.lowercase() }
                )
            }
        }
        
        // Normalize case
        return keywords.map { it.lowercase() }.filter { it.length > 1 }
    }
    
    // ==================== Tertiary Keywords (Level 3) ====================
    
    private fun generateTertiaryKeywords(query: String, secondaryWords: List<String>): List<String> {
        val keywords = mutableListOf<String>()
        
        // Generate stem variants for each secondary word
        val wordsToExpand = secondaryWords.take(4) // Limit to avoid explosion
        
        wordsToExpand.forEach { word ->
            keywords.addAll(generateStemVariants(word))
        }
        
        return keywords.take(config.maxTertiaryKeywords)
    }
    
    // ==================== Word Variation Helpers ====================
    
    private fun generateWordVariations(word: String): List<String> {
        val variations = mutableListOf(word)
        val lower = word.lowercase()
        
        // Common verb endings and their variations
        val verbEndings = mapOf(
            "ing" to listOf("", "e", "er", "ed"),
            "er" to listOf("e", "ing", "ed", "ers"),
            "ed" to listOf("", "e", "ing", "er"),
            "tion" to listOf("te", "ting", "tor", "ted"),
            "ion" to listOf("e", "ing", "or", "ed")
        )
        
        for ((ending, replacements) in verbEndings) {
            if (lower.endsWith(ending) && lower.length > ending.length + 2) {
                val stem = lower.dropLast(ending.length)
                replacements.forEach { replacement ->
                    variations.add(stem + replacement)
                }
            }
        }
        
        // If word is a base form, add common suffixes
        if (!verbEndings.keys.any { lower.endsWith(it) }) {
            variations.addAll(listOf(
                "${lower}ing",
                "${lower}ed",
                "${lower}er",
                "${lower}s"
            ))
            // For words ending in 'e', also try dropping the 'e'
            if (lower.endsWith("e") && lower.length > 2) {
                val stem = lower.dropLast(1)
                variations.addAll(listOf(
                    "${stem}ing",
                    "${stem}ed",
                    "${stem}er"
                ))
            }
        }
        
        return variations.filter { it.length > 1 }.distinct()
    }
    
    private fun generateStemVariants(word: String): List<String> {
        val lower = word.lowercase()
        val variants = mutableListOf<String>()
        
        // Extract stem by removing common suffixes
        val stem = extractStem(lower)
        
        if (stem != lower && stem.length >= 2) {
            variants.add(stem)
            // Add common forms
            variants.addAll(listOf(
                "${stem}e",
                "${stem}ed",
                "${stem}er",
                "${stem}ers",
                "${stem}ing",
                "${stem}s"
            ))
        }
        
        // Add plural/singular variants
        if (lower.endsWith("s") && lower.length > 3) {
            variants.add(lower.dropLast(1))
        } else if (!lower.endsWith("s")) {
            variants.add("${lower}s")
        }
        
        return variants.filter { it.length > 1 && it != lower }
    }
    
    private fun extractStem(word: String): String {
        val suffixes = listOf("ation", "tion", "sion", "ing", "ment", "ness", "ity", "er", "or", "ed", "es", "s")
        
        for (suffix in suffixes) {
            if (word.endsWith(suffix) && word.length > suffix.length + 2) {
                return word.dropLast(suffix.length)
            }
        }
        
        return word
    }
    
    companion object {
        /**
         * Default instance with standard configuration.
         */
        val default = KeywordExpander()
        
        /**
         * Create an expander optimized for code search.
         */
        fun forCodeSearch() = KeywordExpander(
            KeywordExpanderConfig(
                minResultsThreshold = 2,
                maxResultsThreshold = 30,
                enableStemming = true
            )
        )
        
        /**
         * Create an expander optimized for documentation search.
         */
        fun forDocSearch() = KeywordExpander(
            KeywordExpanderConfig(
                minResultsThreshold = 3,
                maxResultsThreshold = 50,
                enableStemming = true
            )
        )
    }
}

/**
 * Recommended search strategy based on result count.
 */
enum class SearchStrategy {
    /** Keep current results - they are in the ideal range */
    KEEP,
    /** Expand search to next keyword level (results too few) */
    EXPAND,
    /** Filter results with more specific keywords (results too many) */
    FILTER
}

