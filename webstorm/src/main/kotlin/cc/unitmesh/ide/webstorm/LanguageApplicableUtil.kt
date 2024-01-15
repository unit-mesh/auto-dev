package cc.unitmesh.ide.webstorm

import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.json.JsonLanguage
import com.intellij.lang.Language
import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.html.HtmlCompatibleFile
import com.intellij.lang.javascript.JavaScriptSupportLoader
import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.lang.javascript.dialects.TypeScriptJSXLanguageDialect
import com.intellij.psi.PsiFile
import com.intellij.util.PlatformUtils
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.annotations.ApiStatus

object LanguageApplicableUtil {
    private val supportedLanguages = setOf(JavascriptLanguage.INSTANCE.id, JavaScriptSupportLoader.TYPESCRIPT.id)
    fun isJavaScriptApplicable(language: Language) =
        supportedLanguages.contains(language.id) || language is HTMLLanguage || language is JsonLanguage || language is TypeScriptJSXLanguageDialect

    @RequiresReadLock
    @ApiStatus.Internal
    fun isWebChatCreationContextSupported(creationContext: ChatCreationContext): Boolean {
        if (PlatformUtils.isWebStorm()) {
            return true
        }
        return isWebLLMContext(creationContext.sourceFile)
    }

    @ApiStatus.Internal
    private fun isWebLLMContext(psiFile: PsiFile?): Boolean {
        if (psiFile == null) return false
        if ((psiFile is HtmlCompatibleFile) || PackageJsonUtil.isPackageJsonFile(psiFile)) {
            return true
        }

        return psiFile.language.isKindOf(JavascriptLanguage.INSTANCE) || psiFile.language.isKindOf(HTMLLanguage.INSTANCE)
    }
}