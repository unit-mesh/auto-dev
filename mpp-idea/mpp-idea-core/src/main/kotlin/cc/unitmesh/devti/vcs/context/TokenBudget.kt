package cc.unitmesh.devti.vcs.context

/**
 * Manages token budget for context window.
 */
data class TokenBudget(
    val maxTokens: Int,
    private var usedTokens: Int = 0
) {
    /**
     * Get remaining tokens
     */
    val remaining: Int
        get() = maxTokens - usedTokens

    /**
     * Get used tokens
     */
    val used: Int
        get() = usedTokens

    /**
     * Check if there are enough tokens available
     */
    fun hasCapacity(tokens: Int): Boolean {
        return remaining >= tokens
    }

    /**
     * Allocate tokens from the budget
     * @return true if allocation succeeded, false if not enough tokens
     */
    fun allocate(tokens: Int): Boolean {
        return if (hasCapacity(tokens)) {
            usedTokens += tokens
            true
        } else {
            false
        }
    }

    /**
     * Release tokens back to the budget
     */
    fun release(tokens: Int) {
        usedTokens = maxOf(0, usedTokens - tokens)
    }

    /**
     * Reset the budget
     */
    fun reset() {
        usedTokens = 0
    }

    /**
     * Get usage percentage
     */
    fun usagePercentage(): Double {
        return if (maxTokens > 0) {
            (usedTokens.toDouble() / maxTokens) * 100
        } else {
            0.0
        }
    }

    companion object {
        /**
         * Default budget for GPT-4 (8K context)
         */
        fun forGpt4(): TokenBudget = TokenBudget(8000)

        /**
         * Budget for GPT-4 32K
         */
        fun forGpt4_32k(): TokenBudget = TokenBudget(32000)

        /**
         * Budget for GPT-3.5-turbo
         */
        fun forGpt35Turbo(): TokenBudget = TokenBudget(4000)

        /**
         * Budget for GPT-3.5-turbo 16K
         */
        fun forGpt35Turbo16k(): TokenBudget = TokenBudget(16000)

        /**
         * Custom budget
         */
        fun custom(maxTokens: Int): TokenBudget = TokenBudget(maxTokens)
    }
}

