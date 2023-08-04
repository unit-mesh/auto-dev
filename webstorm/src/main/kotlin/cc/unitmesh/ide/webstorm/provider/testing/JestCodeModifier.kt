package cc.unitmesh.ide.webstorm.provider.testing

import cc.unitmesh.ide.webstorm.LanguageApplicableUtil
import com.intellij.lang.Language
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

class JestCodeModifier : JavaScriptTestCodeModifier("jest") {
    override fun isApplicable(language: Language): Boolean {
        return LanguageApplicableUtil.isJavaScriptApplicable(language)
    }

    override fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        if (code.startsWith("import") && code.contains("test")) {
            return insertClass(sourceFile, project, code)
        }

        insertClass(sourceFile, project, code)
        return true
    }

    override fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): Boolean {
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