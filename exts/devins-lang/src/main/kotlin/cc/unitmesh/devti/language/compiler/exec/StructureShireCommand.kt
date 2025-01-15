package cc.unitmesh.devti.language.compiler.exec

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.context.FileContextProvider
import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand

class StructureInCommand(val myProject: Project, val prop: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.STRUCTURE

    private val logger = logger<StructureInCommand>()
    override suspend fun execute(): String? {
        val virtualFile = file(myProject, prop)
        if (virtualFile == null) {
            logger.warn("File not found: $prop")
            return null
        }

        val psiFile: PsiFile = withContext(Dispatchers.IO) {
            ApplicationManager.getApplication().executeOnPooledThread<PsiFile?> {
                runReadAction {
                    PsiManager.getInstance(myProject).findFile(virtualFile)
                }
            }.get()
        } ?: return null

        return FileContextProvider().from(psiFile)?.let {
            return it?.format()
        }
    }

    fun file(project: Project, path: String): VirtualFile? {
        val filename = path.split("#")[0]
        val virtualFile = project.lookupFile(filename)
        return virtualFile
    }
}