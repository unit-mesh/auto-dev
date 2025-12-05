package cc.unitmesh.devti.indexer.model

/**
 * Represents a semantic name extracted from code (filename, classname, method name, etc.)
 * Optimized for LLM context window usage with weight and grouping support.
 */
data class SemanticName(
    val name: String,                  // Normalized name (suffix removed)
    val type: ElementType,             // FILE, CLASS, METHOD, FIELD
    val tokens: Int,                   // Estimated token cost
    val source: String = "",           // Original source file/class
    val original: String = "",         // Original name before normalization
    val weight: Float = 0.5f,          // Importance weight [0.0, 1.0]
    val packageName: String = "",      // Java package name for grouping
    val parentClassName: String = "",  // Parent class name for methods
    val weightCategory: String = ""    // Category: Critical, High, Medium, Low, Minimal
)

enum class ElementType {
    FILE, CLASS, METHOD, FIELD
}

/**
 * Represents a class with its methods aggregated
 * Used for structured LLM output
 */
data class ClassMethodGroup(
    val className: String,
    val methods: List<String>,
    val weight: Float = 0.5f,
    val packageName: String = ""
)

/**
 * Collection of semantic names organized by level with support for grouping and weights.
 * Used for generating LLM context with progressive detail and importance ranking.
 */
data class DomainDictionary(
    val level1: List<SemanticName>,  // Filenames only
    val level2: List<SemanticName>,  // Classes and public methods
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Get all names as a simple list for LLM prompt (Level 1 + Level 2)
     */
    fun getAllNames(): List<String> {
        return (level1 + level2)
            .map { it.name }
            .distinct()  // Remove duplicates
    }

    /**
     * Get all names sorted by weight (descending)
     * Higher weight items appear first for LLM attention
     */
    fun getAllNamesSortedByWeight(): List<String> {
        return (level1 + level2)
            .distinctBy { it.name }
            .sortedByDescending { it.weight }
            .map { it.name }
    }

    /**
     * Get all names with their types for debugging
     */
    fun getAllNamesWithType(): List<String> {
        return (level1 + level2)
            .distinctBy { it.name }
            .map { "${it.name} (${it.type})" }
    }

    /**
     * Get total token cost estimation
     */
    fun getTotalTokens(): Int {
        return (level1 + level2).sumOf { it.tokens }
    }

    /**
     * Group semantic names by package and aggregate methods under classes
     * Output format:
     * ```
     * com.example.user
     *   UserController (weight: High)
     *     - getUser, createUser, updateUser
     *   UserService (weight: High)
     *     - getUserById, saveUser
     * com.example.blog
     *   BlogController (weight: Medium)
     *     - listBlogs, createBlog
     * ```
     */
    fun toGroupedFormat(): String {
        val result = StringBuilder()
        
        // Group by package
        val byPackage = (level1 + level2)
            .distinctBy { it.name }
            .sortedByDescending { it.weight }
            .groupBy { it.packageName }

        for ((pkg, items) in byPackage) {
            if (pkg.isNotEmpty()) {
                result.append("$pkg\n")
            }
            
            // Group by parent class for methods
            val classes = items.filter { it.type == ElementType.CLASS }
            val methodsByClass = items
                .filter { it.type == ElementType.METHOD }
                .groupBy { it.parentClassName }

            for (classItem in classes) {
                val methodsForClass = methodsByClass[classItem.name] ?: emptyList()
                val weightCategory = classItem.weightCategory.takeIf { it.isNotEmpty() } ?: "Medium"
                
                result.append("  ${classItem.name} (weight: $weightCategory)\n")
                
                if (methodsForClass.isNotEmpty()) {
                    for (method in methodsForClass) {
                        result.append("    - ${method.name}\n")
                    }
                } else {
                    result.append("    - (no public methods)\n")
                }
            }
            
            // Add files and other items
            val otherItems = items.filter { it.type == ElementType.FILE }
            if (otherItems.isNotEmpty()) {
                result.append("  Files:\n")
                for (item in otherItems) {
                    result.append("    - ${item.name}\n")
                }
            }
            
            result.append("\n")
        }

        return result.toString()
    }

    /**
     * Format as CSV string for LLM consumption
     * Format: name, type, source, original_name, weight, category, package
     */
    fun toCsvFormat(): String {
        val header = "名称,类型,来源,原始名称,Token数,权重,权重等级,所属包\n"
        val rows = (level1 + level2)
            .distinctBy { it.name }
            .sortedByDescending { it.weight }
            .map { name ->
                val weightPercent = (name.weight * 100).toInt()
                "${name.name},${name.type},${name.source},${name.original},${name.tokens},$weightPercent%,${name.weightCategory},${name.packageName}"
            }
        return header + rows.joinToString("\n")
    }

    /**
     * Format as simple comma-separated list (backward compatible)
     */
    fun toSimpleList(): String {
        return getAllNames().joinToString(", ")
    }

    /**
     * Format as weighted simple list (best for LLM)
     * Higher weight items appear first
     */
    fun toWeightedList(): String {
        return getAllNamesSortedByWeight().joinToString(", ")
    }

    /**
     * Get weight statistics
     */
    fun getWeightStatistics(): Map<String, Any> {
        val allItems = level1 + level2
        val avgWeight = if (allItems.isEmpty()) 0f else allItems.map { it.weight }.average().toFloat()
        val maxWeight = allItems.maxOfOrNull { it.weight } ?: 0f
        val minWeight = allItems.minOfOrNull { it.weight } ?: 0f

        return mapOf(
            "averageWeight" to avgWeight,
            "maxWeight" to maxWeight,
            "minWeight" to minWeight,
            "criticalCount" to allItems.count { it.weight >= 0.8f },
            "highCount" to allItems.count { it.weight in 0.6f..0.8f },
            "mediumCount" to allItems.count { it.weight in 0.4f..0.6f },
            "lowCount" to allItems.count { it.weight < 0.4f }
        )
    }
}
