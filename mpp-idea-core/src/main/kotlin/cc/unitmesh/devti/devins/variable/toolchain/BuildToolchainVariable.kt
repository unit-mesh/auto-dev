package cc.unitmesh.devti.devins.variable.toolchain

import cc.unitmesh.devti.devins.variable.ToolchainVariable

/**
 * Enum representing variables used in the generation of code structures.
 */
enum class BuildToolchainVariable(
    override val variableName: String,
    override var value: Any? = null,
    override val description: String = "",
) : ToolchainVariable {
    ProjectDependencies("projectDependencies", description = "The dependencies of the project"),
    ;

    companion object {
        /**
         * Returns the PsiVariable with the given variable name.
         *
         * @param variableName the variable name to search for
         * @return the PsiVariable with the given variable name
         */
        fun from(variableName: String): BuildToolchainVariable? {
            return values().firstOrNull { it.variableName == variableName }
        }
    }
}

