package cc.unitmesh.devti.language.ast.variable.toolchain

import cc.unitmesh.devti.language.ast.variable.ToolchainVariable
/**
 * Enum representing variables used in the generation of code structures.
 */
enum class SonarqubeVariable(
    override val variableName: String,
    override var value: Any? = null,
    override val description: String = "",
) : ToolchainVariable {
    Issue("sonarIssue", null, "the issue of current file"),
    Results("sonarResults", null, "the results of current file")
    ;

    companion object {
        /**
         * Returns the PsiVariable with the given variable name.
         *
         * @param variableName the variable name to search for
         * @return the PsiVariable with the given variable name
         */
        fun from(variableName: String): SonarqubeVariable? {
            return values().firstOrNull { it.variableName == variableName }
        }
    }
}