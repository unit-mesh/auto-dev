package cc.unitmesh.ide.javascript.provider.testing

import cc.unitmesh.ide.javascript.util.LanguageApplicableUtil
import com.intellij.lang.Language
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

class JestCodeModifier : JavaScriptTestCodeModifier() {
    override fun isApplicable(language: Language): Boolean {
        return LanguageApplicableUtil.isJavaScriptApplicable(language)
    }
}