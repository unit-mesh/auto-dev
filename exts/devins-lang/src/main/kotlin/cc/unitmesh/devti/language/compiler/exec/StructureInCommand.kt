package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.bridge.utils.StructureCommandUtil
import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.util.relativePath
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.MalformedURLException
import java.net.URL


class StructureInCommand(val myProject: Project, val prop: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.STRUCTURE

    private val logger = logger<StructureInCommand>()
    override suspend fun execute(): String? {
        val virtualFile = if (isUrl(prop)) {
            fetchHtmlFromUrl(myProject, prop)
        } else {
            file(myProject, prop)
        }
        
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

        val structure = StructureCommandUtil.getFileStructure(myProject, virtualFile, psiFile)
        val filepath = if (isUrl(prop)) {
            prop
        } else {
            virtualFile.relativePath(myProject)
        }

        return "# structure: $filepath\n```\n$structure\n```"
    }

    fun file(project: Project, path: String): VirtualFile? {
        val filename = path.split("#")[0]
        val virtualFile = project.lookupFile(filename)
        return virtualFile
    }
    
    private fun isUrl(str: String): Boolean {
        return try {
            val url = URL(str)
            url.protocol == "http" || url.protocol == "https"
        } catch (e: MalformedURLException) {
            false
        }
    }
    
    private suspend fun fetchHtmlFromUrl(project: Project, url: String): VirtualFile? {
        return try {
            val htmlContent = withContext(Dispatchers.IO) {
                Jsoup.connect(url).get().outerHtml()
            }

            ScratchRootType.getInstance()
                .createScratchFile(project, "autodev-structure.html", HTMLLanguage.INSTANCE, htmlContent)
        } catch (e: Exception) {
            logger.warn("Failed to fetch HTML from URL: $url", e)
            null
        }
    }
}
