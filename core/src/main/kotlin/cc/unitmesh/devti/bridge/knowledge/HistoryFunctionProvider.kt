package cc.unitmesh.devti.bridge.knowledge

import cc.unitmesh.devti.bridge.KnowledgeTransfer
import cc.unitmesh.devti.provider.RevisionProvider
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager

class HistoryFunctionProvider : ToolchainFunctionProvider {
    override fun isApplicable(project: Project, funcName: String): Boolean = funcName == KnowledgeTransfer.History.name

    override fun funcNames(): List<String> = listOf(KnowledgeTransfer.History.name)

    override fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>
    ): Any {
        val path = project.lookupFile(prop) ?: return "File not found"
        return RevisionProvider.provide()?.let {
            val changes = it.history(project, path)
            return changes ?: "No changes found for history provider"
        } ?: "No history provider found"
    }
}

fun Project.lookupFile(path: String): VirtualFile? {
    val projectPath = this.guessProjectDir()?.toNioPath()
    val realpath = projectPath?.resolve(path)
    return VirtualFileManager.getInstance().findFileByUrl("file://${realpath?.toAbsolutePath()}")
}
