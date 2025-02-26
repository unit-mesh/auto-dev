package cc.unitmesh.devti.bridge.utils

import com.intellij.ide.structureView.StructureView
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.lang.LanguageStructureViewBuilder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

object StructureCommandUtil {
    /**
     * ```
     * (1000-9999)
     * ```
     */
    private val maxLineWith = 11
    private val maxDepth = 5
    private val maxLinesForShowLinNO = 60

    fun getFileStructure(project: Project, file: VirtualFile, psiFile: PsiFile): String {
        val viewFactory = LanguageStructureViewBuilder.INSTANCE.forLanguage(psiFile.language)
        val fileEditor: FileEditor = FileEditorManager.getInstance(project).getEditors(file).firstOrNull()
            ?: FileEditorManager.getInstance(project).openFile(file, false, false).firstOrNull()
            ?: return "No FileEditor found."

        if (viewFactory != null) {
            val view: StructureView = viewFactory.getStructureViewBuilder(psiFile)
                ?.createStructureView(fileEditor, project)
                ?: return "No StructureView found."

            /// close the editor
            FileEditorManager.getInstance(project).closeFile(file)

            val root: StructureViewTreeElement = view.treeModel.root
            return traverseStructure(root, 0, StringBuilder()).toString()
        }

        return "No StructureViewModel found."
    }

    /**
     * Display format
     * ```
     * elementName (location) - line number or navigate to element ?
     * ```
     */
    private fun traverseStructure(element: StructureViewTreeElement, depth: Int, sb: StringBuilder): StringBuilder {
        val indent = formatBeforeCode(element, depth)
        var str = element.presentation.presentableText
//        if (!str.isNullOrBlank() && !element.presentation.locationString.isNullOrBlank()) {
//            str += " (${element.presentation.locationString})"
//        }
        if (!str.isNullOrBlank()) {
            sb.append(indent).append(str).append("\n")
        }

        for (child in element.children) {
            if (child is StructureViewTreeElement) {
                traverseStructure(child, depth + 1, sb)
            }
        }

        return sb
    }

    private fun formatBeforeCode(element: StructureViewTreeElement, depth: Int): String {
        return if (element.value is PsiElement) {
            val psiElement = element.value as PsiElement
            val line = formatLine(psiElement)
            if (line.length < maxLineWith) {
                line + " ".repeat(maxLineWith - line.length) + "  ".repeat(depth)
            } else {
                line + "  ".repeat(depth)
            }
        } else {
            " ".repeat(maxLineWith) + "  ".repeat(depth)
        }
    }

    private fun formatLine(psiElement: PsiElement): String {
        val psiFile: PsiFile = psiElement.containingFile
        val document: Document = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile) ?: return ""
        val start = document.getLineNumber(psiElement.textRange.startOffset)
        val end = document.getLineNumber(psiElement.textRange.endOffset)

        if (end - start > maxLinesForShowLinNO) {
            return "(${start + 1}-${end + 1})"
        }

        return " ".repeat(maxDepth)
    }
}