package cc.unitmesh.devti.prompting.optimizer

import cc.unitmesh.devti.util.parser.CodeFence

object PromptOptimizer {
    fun trimCodeSpace(prompt: String): String {
        val fences = CodeFence.parseAll(prompt)
        return fences.joinToString("\n") {
            when (it.originLanguage) {
                null -> {
                    trim(it.text)
                }
                "python" -> {
                    "```${it.originLanguage}\n${it.text}\n```"
                }
                "txt", "md", "markdown" -> {
                    trim(it.text)
                }
                else -> {
                    "```${it.originLanguage}\n${trim(it.text)}\n```"
                }
            }
        }
    }

    /**
     * Related to [#317](https://github.com/unit-mesh/auto-dev/issues/317)
     *
     * Similar to the following shell command:
     * ```bash
     * grep -Ev '^[ \t]*$ input.rs | sed 's/^[ \t]*\/\/' | sed 's/[ \t]$//'
     * ```
     */
    private fun trim(prompt: String): String {
        return prompt.lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") { it.trim() }
    }
}