package cc.unitmesh.devti.bridge.knowledge

import cc.unitmesh.devti.bridge.KnowledgeTransfer
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.project.Project

class KnowledgeFunctionProvider : ToolchainFunctionProvider {
    override fun funcNames(): List<String> = listOf(KnowledgeTransfer.Knowledge.name)

    override fun isApplicable(project: Project, funcName: String): Boolean =
        funcName == KnowledgeTransfer.Knowledge.name

    override fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>
    ): Any {
        return ""
    }
}