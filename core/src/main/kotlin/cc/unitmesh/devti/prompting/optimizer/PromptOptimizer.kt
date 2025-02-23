package cc.unitmesh.devti.prompting.optimizer

object PromptOptimizer {
    /**
     * Similar to the following shell command:
     * ```bash
     * grep -Ev '^[ \t]*$ input.rs | sed 's/^[ \t]*\/\/' | sed 's/[ \t]$//'
     * ```
     */
    fun trimCodeSpace(prompt: String): String {
        /// check language of CodeFence skip for Python
        return prompt.lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") { it.trim() }
    }
}