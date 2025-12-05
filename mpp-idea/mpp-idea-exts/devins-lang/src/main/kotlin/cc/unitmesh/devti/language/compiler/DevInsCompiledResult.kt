package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.agent.custom.model.CustomAgentConfig
import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.ast.variable.VariableTable
import cc.unitmesh.devti.language.psi.DevInFile

data class DevInsCompiledResult(
    /**
     * The origin DevIns content
     */
    var input: String = "",
    /**
     * Output String of a compile result
     */
    var output: String = "",
    var isLocalCommand: Boolean = false,
    var hasError: Boolean = false,
    var executeAgent: CustomAgentConfig? = null,
    /**
     * Next job to be executed
     */
    var nextJob: DevInFile? = null,

    /**
     * The frontmatter of the file, which contains the configuration of Shire
     */
    var config: HobbitHole? = null,

    /**
     * Symbol table for all variables
     */
    var variableTable: VariableTable = VariableTable(),
) {
}
