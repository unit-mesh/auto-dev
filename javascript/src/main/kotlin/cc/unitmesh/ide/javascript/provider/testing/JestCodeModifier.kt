package cc.unitmesh.ide.javascript.provider.testing

import cc.unitmesh.ide.javascript.util.LanguageApplicableUtil
import com.intellij.lang.Language

class JestCodeModifier : JavaScriptTestCodeModifier() {
    override fun isApplicable(language: Language): Boolean {
        return LanguageApplicableUtil.isJavaScriptApplicable(language)
    }
}