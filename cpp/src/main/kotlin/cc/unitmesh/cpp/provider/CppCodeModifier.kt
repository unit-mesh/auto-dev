package cc.unitmesh.cpp.provider

import cc.unitmesh.devti.context.builder.CodeModifier
import com.intellij.lang.Language
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.cidr.execution.debugger.OCDebuggerTypesHelper
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.parser.OCElementType
import com.jetbrains.cidr.lang.psi.OCFile
import com.jetbrains.cidr.lang.util.OCElementFactory

class CppCodeModifier : CodeModifier {
    override fun isApplicable(language: Language): Boolean = language is OCLanguage

    override fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        val isExit = sourceFile as? OCFile
        if (isExit == null) {
            insertClass(sourceFile, project, code)
            return true
        }

        insertMethod(sourceFile, project, code)
        return true
    }

    override fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        val file = sourceFile as OCFile
        val psiElement = file.lastChild

        val codeElement = OCElementFactory.expressionOrStatementsCodeFragment(code, project, file, true, false)

        com.intellij.openapi.application.runReadAction {
            psiElement?.parent?.addAfter(codeElement, psiElement)
        }

        return true
    }

    override fun insertClass(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        WriteCommandAction.runWriteCommandAction(project) {
            val psiFile = PsiManager.getInstance(project).findFile(sourceFile) as OCFile
            val document = psiFile.viewProvider.document!!
            document.insertString(document.textLength, code)
        }

        return true
    }
}
