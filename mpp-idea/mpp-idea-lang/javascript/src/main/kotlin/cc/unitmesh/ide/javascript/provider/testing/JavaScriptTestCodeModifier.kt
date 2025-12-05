package cc.unitmesh.ide.javascript.provider.testing

import cc.unitmesh.devti.context.builder.CodeModifier
import cc.unitmesh.ide.javascript.util.LanguageApplicableUtil
import com.intellij.lang.Language
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.impl.JSPsiElementFactory
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager

open class JavaScriptTestCodeModifier : CodeModifier {
    override fun isApplicable(language: Language): Boolean {
        return LanguageApplicableUtil.isJavaScriptApplicable(language)
    }

    override fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        val isExit = sourceFile as? JSFile
        if (isExit == null) {
            insertClass(sourceFile, project, code)
            return true
        }

        insertMethod(sourceFile, project, code)
        return true
    }

    override fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        // todo: spike for insert different method type, like named function, arrow function, etc.
        val jsFile = PsiManager.getInstance(project).findFile(sourceFile) as JSFile
        val psiElement = jsFile.lastChild

        val element = PsiFileFactory.getInstance(project).createFileFromText(jsFile.language, "")
        val codeElement = JSPsiElementFactory.createJSStatement(code, element)

        runReadAction {
            psiElement?.parent?.addAfter(codeElement, psiElement)
        }

        return true
    }

    override fun insertClass(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        WriteCommandAction.runWriteCommandAction(project) {
            val psiFile = PsiManager.getInstance(project).findFile(sourceFile) as JSFile
            val document = psiFile.viewProvider.document!!
            document.insertString(document.textLength, code)
        }

        return true
    }

}
