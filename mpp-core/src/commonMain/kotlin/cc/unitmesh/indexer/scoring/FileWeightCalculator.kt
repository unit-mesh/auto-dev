package cc.unitmesh.indexer.scoring

/**
 * Calculates file importance weight based on various factors.
 * Weight is normalized to [0.0, 1.0] for LLM context prioritization.
 * 
 * Since this is a multiplatform module, we provide a simplified version
 * that doesn't rely on Git history (which requires platform-specific implementations).
 */
object FileWeightCalculator {
    
    /**
     * Calculate weight based on file characteristics
     * This is a simplified version for multiplatform compatibility
     */
    fun calculateWeight(
        filePath: String,
        fileSize: Long = 0,
        isInMainSource: Boolean = true,
        isTestFile: Boolean = false
    ): Float {
        var weight = 0.5f // Base weight
        
        // File location weight (40%)
        weight += when {
            isTestFile -> -0.2f // Test files are less important
            isInMainSource -> 0.2f // Main source files are more important
            filePath.contains("example") || filePath.contains("demo") -> -0.1f
            else -> 0.0f
        }
        
        // File size weight (30%) - larger files might be more important
        weight += when {
            fileSize > 10000 -> 0.15f // Large files
            fileSize > 5000 -> 0.1f   // Medium files
            fileSize > 1000 -> 0.05f  // Small files
            else -> 0.0f              // Very small files
        }
        
        // File name patterns (30%)
        val fileName = filePath.substringAfterLast('/')
        weight += when {
            fileName.contains("Main", ignoreCase = true) -> 0.15f
            fileName.contains("Core", ignoreCase = true) -> 0.1f
            fileName.contains("Base", ignoreCase = true) -> 0.1f
            fileName.contains("Abstract", ignoreCase = true) -> 0.05f
            fileName.contains("Interface", ignoreCase = true) -> 0.05f
            fileName.contains("Util", ignoreCase = true) -> -0.1f
            fileName.contains("Helper", ignoreCase = true) -> -0.1f
            fileName.contains("Test", ignoreCase = true) -> -0.15f
            else -> 0.0f
        }
        
        // Ensure weight is within bounds
        return weight.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Get weight category for display purposes
     */
    fun getWeightCategory(weight: Float): String {
        return when {
            weight >= 0.8f -> "Critical"
            weight >= 0.6f -> "High"
            weight >= 0.4f -> "Medium"
            weight >= 0.2f -> "Low"
            else -> "Minimal"
        }
    }
    
    /**
     * Calculate class-specific weight based on class characteristics
     */
    fun calculateClassWeight(
        className: String,
        isPublic: Boolean = true,
        isAbstract: Boolean = false,
        isInterface: Boolean = false,
        methodCount: Int = 0,
        baseWeight: Float = 0.5f
    ): Float {
        var weight = baseWeight
        
        // Visibility weight
        weight += if (isPublic) 0.1f else -0.1f
        
        // Type weight
        weight += when {
            isInterface -> 0.15f // Interfaces are important for understanding contracts
            isAbstract -> 0.1f   // Abstract classes define important patterns
            else -> 0.0f
        }
        
        // Method count weight (more methods = more important)
        weight += when {
            methodCount > 20 -> 0.15f
            methodCount > 10 -> 0.1f
            methodCount > 5 -> 0.05f
            else -> 0.0f
        }
        
        // Class name patterns
        weight += when {
            className.endsWith("Manager") -> -0.05f
            className.endsWith("Helper") -> -0.1f
            className.endsWith("Util") -> -0.1f
            className.endsWith("Test") -> -0.2f
            className.contains("Main") -> 0.15f
            className.contains("Core") -> 0.1f
            else -> 0.0f
        }
        
        return weight.coerceIn(0.0f, 1.0f)
    }
}
