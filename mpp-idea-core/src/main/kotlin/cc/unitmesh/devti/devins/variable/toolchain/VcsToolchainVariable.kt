package cc.unitmesh.devti.devins.variable.toolchain

import cc.unitmesh.devti.devins.variable.ToolchainVariable

/**
 * Enum representing variables used in the generation of code structures.
 */
enum class VcsToolchainVariable(
    override val variableName: String,
    override var value: Any? = null,
    override val description: String = "",
) : ToolchainVariable {
    CurrentChanges("currentChanges", description = "The code changes in the current working directory"),

    CurrentBranch("currentBranch", description = "The name of the current branch"),

    HistoryCommitMessages("historyCommitMessages", description = "The commit messages in the history"),

    Diff("diff", description = "The diff of the current changes")
    ;

    companion object {
        /**
         * Returns the PsiVariable with the given variable name.
         *
         * @param variableName the variable name to search for
         * @return the PsiVariable with the given variable name
         */
        fun from(variableName: String): VcsToolchainVariable? {
            return values().firstOrNull { it.variableName == variableName }
        }
    }
}

