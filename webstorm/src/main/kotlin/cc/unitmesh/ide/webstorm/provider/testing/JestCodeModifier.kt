package cc.unitmesh.ide.webstorm.provider.testing

import cc.unitmesh.ide.webstorm.LanguageApplicableUtil
import com.intellij.lang.Language
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.PlatformUtils

class JestCodeModifier : JavaScriptTestCodeModifier("jest") {
    override fun isApplicable(language: Language): Boolean {
        if (PlatformUtils.isWebStorm()) return true

        return LanguageApplicableUtil.isJavaScriptApplicable(language)
    }

    override fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        if (code.startsWith("import") && code.contains("test ")) {
            return insertClass(sourceFile, project, code)
        }

        insertMethod(sourceFile, project, code)
        return true
    }

    override fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        val file = PsiManager.getInstance(project).findFile(sourceFile) as JSFile
        ApplicationManager.getApplication().invokeLater {
            val rootElement = runReadAction {

            }
        }

        return true
    }

    override fun insertClass(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        return true
    }

}