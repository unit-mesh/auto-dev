package cc.unitmesh.devti.language.debugger

import cc.unitmesh.devti.devins.variable.ContextVariable
import cc.unitmesh.devti.devins.variable.PsiContextVariable
import cc.unitmesh.devti.devins.variable.SystemInfoVariable
import cc.unitmesh.devti.devins.variable.Variable
import cc.unitmesh.devti.devins.variable.toolchain.BuildToolchainVariable
import cc.unitmesh.devti.devins.variable.toolchain.DatabaseToolchainVariable
import cc.unitmesh.devti.devins.variable.toolchain.SonarqubeVariable
import cc.unitmesh.devti.devins.variable.toolchain.TerminalToolchainVariable
import cc.unitmesh.devti.devins.variable.toolchain.VcsToolchainVariable
import kotlin.collections.addAll

data class DebugValueVariable(
    override val variableName: String,
    override var value: Any?,
    override val description: String,
) : Variable {
    companion object {
        fun description(key: String): String {
            return PsiContextVariable.Companion.from(key)?.description
                ?: ContextVariable.Companion.from(key)?.description
                ?: SystemInfoVariable.Companion.from(key)?.description
//                ?: ConditionPsiVariable.from(key)?.description
                /// ...
                ?: DatabaseToolchainVariable.Companion.from(key)?.description
                ?: TerminalToolchainVariable.Companion.from(key)?.description
                ?: VcsToolchainVariable.Companion.from(key)?.description
                ?: BuildToolchainVariable.Companion.from(key)?.description
                ?: SonarqubeVariable.Companion.from(key)?.description
                ?: "Unknown"
        }

        fun all(): List<Variable> {
            val allVariables = mutableListOf<Variable>()
            allVariables.addAll(ContextVariable.values())
            allVariables.addAll(PsiContextVariable.Companion.all())
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