package cc.unitmesh.ide.webstorm.provider.testing

import cc.unitmesh.ide.webstorm.LanguageApplicableUtil
import com.intellij.lang.Language
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.PlatformUtils
import kotlin.jvm.internal.Ref

class JestCodeModifier : JavaScriptTestCodeModifier("jest") {
    override fun isApplicable(language: Language): Boolean {
        if (PlatformUtils.isWebStorm()) return true

        return LanguageApplicableUtil.isJavaScriptApplicable(language)
    }

    override fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        if (code.startsWith("import") && code.contains("test")) {
            return insertClass(sourceFile, project, code)
        }

        insertMethod(sourceFile, project, code)
        return true
    }

    override fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        val jsFile = ReadAction.compute<PsiFile, Throwable> {
            PsiManager.getInstance(project).findFile(sourceFile) as JSFile
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