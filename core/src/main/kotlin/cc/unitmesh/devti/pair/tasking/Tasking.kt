package cc.unitmesh.devti.pair.tasking

enum class TaskingStatus {
    TODO,
    DOING,
    DONE,
}

data class Tasking(
    val id: Int,
    val name: String,
    val status: TaskingStatus,
) {
    companion object {
        /**
         * Parse GitHub Markdown to Tasking
         * Example input:
         * ```markdown
         * - [ ] Task 1
         * - [ ] Task 2
         * ```
         */
        fun fromMarkdown(markdown: String): List<Tasking> {
            val taskRegex = Regex("^[\\s*-]*\\[([\\s*x])\\] (.+)", RegexOption.MULTILINE)
            val matches = taskRegex.findAll(markdown)

            val tasks = matches.map { matchResult ->
                val (statusChar, taskName) = matchResult.destructured
                val status = when (statusChar.trim()) {
                    "x" -> TaskingStatus.DONE
                    else -> TaskingStatus.TODO
                }

                Tasking(id = 0, name = taskName.trim(), status = status)
            }.toList()

            return tasks
        }
    }
}
