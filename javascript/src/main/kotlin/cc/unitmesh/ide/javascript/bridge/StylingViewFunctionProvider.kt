package cc.unitmesh.ide.javascript.bridge

import cc.unitmesh.devti.bridge.ArchViewCommand
import cc.unitmesh.devti.bridge.utils.StructureCommandUtil
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.css.CssFileType
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope

class StylingViewFunctionProvider : ToolchainFunctionProvider {
    override suspend fun isApplicable(project: Project, funcName: String) = funcName == ArchViewCommand.StylingView.name

    override suspend fun funcNames(): List<String> = listOf(ArchViewCommand.StylingView.name)

    override suspend fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>,
        commandName: @NlsSafe String
    ): Any {
        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        val scssType = FileTypeManagerEx.getInstanceEx().getFileTypeByExtension("scss")
        var files = runReadAction { FileTypeIndex.getFiles(scssType, searchScope) }
        if (files.isEmpty()) {
            files = FileTypeIndex.getFiles(CssFileType.INSTANCE, searchScope)
        }

        val result = files
            .filter { !isVendorOrIgnoreStyle(it) }
            .mapNotNull { virtualFile ->
                val psiFile =
                    runReadAction { PsiManager.getInstance(project).findFile(virtualFile) } ?: return@mapNotNull null
                StructureCommandUtil.getFileStructure(project, virtualFile, psiFile)
            }

        return result.joinToString("\n")
    }

    /// skip all .module.css files and min,css files and under vendors
    private fun isVendorOrIgnoreStyle(file: VirtualFile): Boolean =
        file.name.contains(".module.css")
                || file.name.contains(".min.css")
                || file.path.contains("vendors")
                || file.path.contains("dist")
                || file.path.contains("assets")
}