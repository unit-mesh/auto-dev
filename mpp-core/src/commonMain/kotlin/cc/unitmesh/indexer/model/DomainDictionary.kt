package cc.unitmesh.indexer.model

import kotlinx.serialization.Serializable

/**
 * Collection of semantic names organized by level with support for grouping and weights.
 * Used for generating LLM context with progressive detail and importance ranking.
 */
@Serializable
data class DomainDictionary(
    val level1: List<SemanticName>,  // Filenames only
    val level2: List<SemanticName>,  // Classes and public methods
    val metadata: Map<String, String> = emptyMap()
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
     * Get all names sorted by weight (highest first)
     */
    fun getAllNamesSortedByWeight(): List<String> {
        return (level1 + level2)
            .distinctBy { it.name }
            .sortedByDescending { it.weight }
            .map { it.name }
    }

    /**
     * Get total token count
     */
    fun getTotalTokens(): Int {
        return (level1 + level2).sumOf { it.tokens }
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
            "highCount" to allItems.count { it.weight >= 0.6f && it.weight < 0.8f },
            "mediumCount" to allItems.count { it.weight >= 0.4f && it.weight < 0.6f },
            "lowCount" to allItems.count { it.weight < 0.4f }
        )
    }

    /**
     * Group methods by class for structured output
     */
    fun groupMethodsByClass(): List<ClassMethodGroup> {
        val methodsByClass = level2
            .filter { it.type == ElementType.METHOD || it.type == ElementType.FUNCTION }
            .groupBy { it.parentClassName }

        return methodsByClass.map { (className, methods) ->
            val avgWeight = methods.map { it.weight }.average().toFloat()
            val packageName = methods.firstOrNull()?.packageName ?: ""
            ClassMethodGroup(
                className = className,
                methods = methods.map { it.name },
                weight = avgWeight,
                packageName = packageName
            )
        }.sortedByDescending { it.weight }
    }
}
