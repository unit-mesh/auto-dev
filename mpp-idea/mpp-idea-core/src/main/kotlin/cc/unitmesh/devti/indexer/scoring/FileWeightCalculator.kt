package cc.unitmesh.devti.indexer.scoring

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.vcsUtil.VcsUtil
// import git4idea.history.GitFileHistory // Removed in platform 252

/**
 * Calculates file importance weight based on:
 * - Git commit frequency (60%)
 * - File size/lines of code (40%)
 *
 * Weight is normalized to [0.0, 1.0] for LLM context prioritization.
 */
object FileWeightCalculator {
    private val logger = logger<FileWeightCalculator>()

    /**
     * Calculate weight for a virtual file
     * Returns value in [0.0, 1.0] where:
     *  - 1.0 = most important (frequently changed, large)
     *  - 0.0 = least important (rarely changed, small)
     */
    fun calculateWeight(
        project: Project,
        virtualFile: VirtualFile,
        maxCommitCount: Int = 100,
        maxLineCount: Int = 2000
    ): Float {
        try {
            // Get commit frequency (60% weight)
            val commitCount = getCommitCount(project, virtualFile)
            val commitWeight = (commitCount.coerceAtMost(maxCommitCount).toFloat() / maxCommitCount)

            // Get file size in lines (40% weight)
            val lineCount = getLineCount(virtualFile)
            val sizeWeight = (lineCount.coerceAtMost(maxLineCount).toFloat() / maxLineCount)

            // Combined weight: 60% commits + 40% size
            return (0.6f * commitWeight + 0.4f * sizeWeight).coerceIn(0.0f, 1.0f)
        } catch (e: Exception) {
            logger.debug("Failed to calculate weight for ${virtualFile.path}", e)
            return 0.5f  // Default neutral weight on error
        }
    }

    /**
     * Get class importance weight based on file weight and class size
     */
    fun calculateClassWeight(
        project: Project,
        virtualFile: VirtualFile,
        psiClass: PsiClass
    ): Float {
        val fileWeight = calculateWeight(project, virtualFile)
        
        // Class size: count methods and fields
        val methodCount = runReadAction { psiClass.methods.size.toFloat() }
        val fieldCount = runReadAction { psiClass.fields.size.toFloat() }
        val totalMembers = (methodCount + fieldCount).coerceAtLeast(1f)
        
        // Large classes are more important
        val classSize = totalMembers.coerceAtMost(10f) / 10f
        
        // Combined: 70% file importance + 30% class size
        return (0.7f * fileWeight + 0.3f * classSize).coerceIn(0.0f, 1.0f)
    }

    /**
     * Get commit count for a file from Git history
     * Note: GitFileHistory API changed in platform 252, returning default value
     */
    private fun getCommitCount(project: Project, virtualFile: VirtualFile): Int {
        // GitFileHistory.collectHistory removed in platform 252
        // Return moderate value as default
        logger.debug("Git commit history not available for ${virtualFile.name}")
        return 5 // Default moderate commit count
    }

    /**
     * Get line count of a file
     */
    private fun getLineCount(virtualFile: VirtualFile): Int {
        return try {
            val content = String(virtualFile.contentsToByteArray())
            content.count { it == '\n' }.coerceAtLeast(1)
        } catch (e: Exception) {
            logger.debug("Cannot get line count for ${virtualFile.name}: ${e.message}")
            0
        }
    }

    /**
     * Get weight category for a semantic element
     * Useful for sorting and prioritization
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
}
