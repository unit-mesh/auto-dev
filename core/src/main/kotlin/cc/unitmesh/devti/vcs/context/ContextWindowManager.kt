package cc.unitmesh.devti.vcs.context

import com.intellij.openapi.diagnostic.logger

/**
 * Manages context window and token budget allocation for VCS changes.
 */
class ContextWindowManager(
    private val tokenBudget: TokenBudget,
    private val tokenCounter: TokenCounter = TokenCounter.DEFAULT
) {
    private val logger = logger<ContextWindowManager>()

    /**
     * Result of context allocation
     */
    data class AllocationResult(
        val fullDiffChanges: List<PrioritizedChange>,
        val summaryChanges: List<PrioritizedChange>,
        val excludedChanges: List<PrioritizedChange>,
        val totalTokensUsed: Int
    )

    /**
     * Allocate changes to different strategies based on priority and token budget
     */
    fun allocateChanges(
        prioritizedChanges: List<PrioritizedChange>,
        diffContents: Map<PrioritizedChange, String>
    ): AllocationResult {
        val fullDiffChanges = mutableListOf<PrioritizedChange>()
        val summaryChanges = mutableListOf<PrioritizedChange>()
        val excludedChanges = mutableListOf<PrioritizedChange>()

        tokenBudget.reset()

        // Sort changes by priority (already sorted from FilePriorityCalculator)
        for (change in prioritizedChanges) {
            when (change.priority) {
                FilePriority.EXCLUDED -> {
                    excludedChanges.add(change)
                    continue
                }
                else -> {
                    val allocated = tryAllocateChange(
                        change,
                        diffContents[change],
                        fullDiffChanges,
                        summaryChanges
                    )
                    if (!allocated) {
                        excludedChanges.add(change)
                    }
                }
            }
        }

        logger.info(
            "Context allocation: ${fullDiffChanges.size} full, " +
                    "${summaryChanges.size} summary, ${excludedChanges.size} excluded. " +
                    "Tokens used: ${tokenBudget.used}/${tokenBudget.maxTokens}"
        )

        return AllocationResult(
            fullDiffChanges = fullDiffChanges,
            summaryChanges = summaryChanges,
            excludedChanges = excludedChanges,
            totalTokensUsed = tokenBudget.used
        )
    }

    /**
     * Try to allocate a change to either full diff or summary
     */
    private fun tryAllocateChange(
        change: PrioritizedChange,
        diffContent: String?,
        fullDiffChanges: MutableList<PrioritizedChange>,
        summaryChanges: MutableList<PrioritizedChange>
    ): Boolean {
        // Try full diff first for high priority files
        if (change.priority.level >= FilePriority.HIGH.level && diffContent != null) {
            val tokens = tokenCounter.countTokens(diffContent)
            if (tokenBudget.allocate(tokens)) {
                fullDiffChanges.add(change)
                return true
            }
        }

        // Try summary if full diff doesn't fit or priority is lower
        val summaryStrategy = SummaryDiffStrategy()
        val summary = summaryStrategy.generateDiff(change, diffContent)
        val summaryTokens = tokenCounter.countTokens(summary)

        if (tokenBudget.allocate(summaryTokens)) {
            summaryChanges.add(change)
            return true
        }

        // If even summary doesn't fit, try metadata only for critical files
        if (change.priority == FilePriority.CRITICAL) {
            val metadataStrategy = MetadataOnlyStrategy()
            val metadata = metadataStrategy.generateDiff(change, null)
            val metadataTokens = tokenCounter.countTokens(metadata)

            if (tokenBudget.allocate(metadataTokens)) {
                summaryChanges.add(change)
                return true
            }
        }

        return false
    }

    /**
     * Calculate optimal strategy for a change based on available budget
     */
    fun selectStrategy(change: PrioritizedChange, diffContent: String?): DiffStrategy {
        if (diffContent == null) {
            return MetadataOnlyStrategy()
        }

        val tokens = tokenCounter.countTokens(diffContent)

        return when {
            // High priority and fits in budget -> full diff
            change.priority.level >= FilePriority.HIGH.level && tokenBudget.hasCapacity(tokens) -> {
                FullDiffStrategy()
            }
            // Medium priority or doesn't fit -> summary
            change.priority.level >= FilePriority.MEDIUM.level -> {
                SummaryDiffStrategy()
            }
            // Low priority -> metadata only
            else -> {
                MetadataOnlyStrategy()
            }
        }
    }

    companion object {
        /**
         * Create a context window manager with default GPT-4 budget
         */
        fun forGpt4(): ContextWindowManager {
            return ContextWindowManager(TokenBudget.forGpt4())
        }

        /**
         * Create a context window manager with GPT-3.5 budget
         */
        fun forGpt35Turbo(): ContextWindowManager {
            return ContextWindowManager(TokenBudget.forGpt35Turbo())
        }

        /**
         * Create a context window manager with custom budget
         */
        fun custom(maxTokens: Int): ContextWindowManager {
            return ContextWindowManager(TokenBudget.custom(maxTokens))
        }
    }
}

