package cc.unitmesh.devti.language.ast.variable.toolchain

import cc.unitmesh.devti.language.ast.variable.ToolchainVariable

/**
 * Enum representing variables used in the generation of code structures.
 */
enum class TerminalToolchainVariable(
    override val variableName: String,
    override var value: Any? = null,
    override val description: String = "",
) : ToolchainVariable {
    SHELL_PATH("shellPath", "/bin/bash", "The path to the shell executable"),

    PWD("pwd", null, "The current working directory"),
    ;

    companion object {
        /**
         * Returns the PsiVariable with the given variable name.
         *
         * @param variableName the variable name to search for
         * @return the PsiVariable with the given variable name
         */
        fun from(variableName: String): TerminalToolchainVariable? {
            return values().firstOrNull { it.variableName == variableName }
        }
    }
}