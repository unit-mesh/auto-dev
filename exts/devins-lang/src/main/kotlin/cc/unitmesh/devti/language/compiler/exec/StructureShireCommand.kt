package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.utils.lookupFile
import com.intellij.ide.structureView.StructureView
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.lang.LanguageStructureViewBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.html.HtmlTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.start


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

        return "```\n" + getFileStructure(myProject, virtualFile, psiFile) + "\n```"
    }

    fun getFileStructure(project: Project, file: VirtualFile, psiFile: PsiFile): String {
        var openFiles: Array<FileEditor> = arrayOf()
        ApplicationManager.getApplication().invokeAndWait {
            openFiles = FileEditorManager.getInstance(myProject).openFile(file, true)
        }

        val fileEditor = openFiles.firstOrNull() ?: return "No FileEditor found."

        val viewFactory = LanguageStructureViewBuilder.INSTANCE.forLanguage(psiFile.language)

        if (viewFactory != null) {
            val view: StructureView = viewFactory.getStructureViewBuilder(psiFile)
                ?.createStructureView(fileEditor, project)
                ?: return "No StructureView found."

            val root: StructureViewTreeElement = view.treeModel.root
            return traverseStructure(root, 0, StringBuilder()).toString()
        }

        return "No StructureViewModel found."
    }

    // (1000-9999)
    private val maxLineWith = 11

    /**
     * Display format
     * ```
     * elementName (location) - line number or navigate to element ?
     * ```
     */
    private fun traverseStructure(element: StructureViewTreeElement, depth: Int, sb: StringBuilder): StringBuilder {
        val indent = if (element.value is PsiElement) {
            val psiElement = element.value as PsiElement
            val line = "(" + psiElement.textRange.startOffset + "," + psiElement.textRange.endOffset + ")"
             if (line.length < maxLineWith) {
                line + " ".repeat(maxLineWith - line.length) + "  ".repeat(depth)
             } else {
                 line + "  ".repeat(depth)
             }
        } else {
            "  ".repeat(depth)
        }

        /// todo: add element line
        var str = element.presentation.presentableText
        if (!str.isNullOrBlank() && !element.presentation.locationString.isNullOrBlank()) {
            str += " (${element.presentation.locationString})"
        }

        sb.append(indent).append(str).append("\n")

        for (child in element.children) {
            if (child is StructureViewTreeElement) {
                traverseStructure(child, depth + 1, sb)
            }
        }

        return sb
    }

    fun file(project: Project, path: String): VirtualFile? {
        val filename = path.split("#")[0]
        val virtualFile = project.lookupFile(filename)
        return virtualFile
    }
}