package cc.unitmesh.devti.bridge.assessment

import cc.unitmesh.devti.bridge.Assessment
import cc.unitmesh.devti.bridge.command.SccWrapper
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir

class SccFunctionProvider : ToolchainFunctionProvider {
    override fun isApplicable(project: Project, funcName: String): Boolean = funcName == Assessment.SCC.name

    override fun execute(
        project: Project,
        funcName: String,
        args: List<Any>,
        allVariables: Map<String, Any?>
    ): Any {
        val path = if (args.isEmpty()) project.guessProjectDir()!!.path else args[0].toString()
        return SccWrapper().runSccSync(path)
    }
}
