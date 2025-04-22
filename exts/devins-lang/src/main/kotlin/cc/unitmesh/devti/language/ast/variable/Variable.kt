package cc.unitmesh.devti.language.ast.variable

import cc.unitmesh.devti.language.ast.variable.toolchain.BuildToolchainVariable
import cc.unitmesh.devti.language.ast.variable.toolchain.DatabaseToolchainVariable
import cc.unitmesh.devti.language.ast.variable.toolchain.SonarqubeVariable
import cc.unitmesh.devti.language.ast.variable.toolchain.TerminalToolchainVariable
import cc.unitmesh.devti.language.ast.variable.toolchain.VcsToolchainVariable

interface Variable {
    val variableName: String
    val description: String
    var value: Any?
}

data class DebugValue(
    override val variableName: String,
    override var value: Any?,
    override val description: String,
) : Variable {
    companion object {
        fun description(key: String): String {
            return PsiContextVariable.from(key)?.description
                ?: ContextVariable.from(key)?.description
                ?: SystemInfoVariable.from(key)?.description
//                ?: ConditionPsiVariable.from(key)?.description
                /// ...
                ?: DatabaseToolchainVariable.from(key)?.description
                ?: TerminalToolchainVariable.from(key)?.description
                ?: VcsToolchainVariable.from(key)?.description
                ?: BuildToolchainVariable.from(key)?.description
                ?: SonarqubeVariable.from(key)?.description
                ?: "Unknown"
        }

        fun all(): List<Variable> {
            val allVariables = mutableListOf<Variable>()
            allVariables.addAll(ContextVariable.values())
            allVariables.addAll(PsiContextVariable.all())
            allVariables.addAll(SystemInfoVariable.values())
//            allVariables.addAll(ConditionPsiVariable.values())
            /// ...
            allVariables.addAll(DatabaseToolchainVariable.values())
            allVariables.addAll(TerminalToolchainVariable.values())
            allVariables.addAll(VcsToolchainVariable.values())
            allVariables.addAll(BuildToolchainVariable.values())
            allVariables.addAll(SonarqubeVariable.values())
            return allVariables
        }
    }
}