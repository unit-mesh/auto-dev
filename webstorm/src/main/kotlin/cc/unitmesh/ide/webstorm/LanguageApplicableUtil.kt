package cc.unitmesh.ide.webstorm

import com.intellij.json.JsonLanguage
import com.intellij.lang.Language
import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.javascript.JavaScriptSupportLoader
import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.lang.javascript.dialects.TypeScriptJSXLanguageDialect

object LanguageApplicableUtil {
    private val supportedLanguages = setOf(JavascriptLanguage.INSTANCE.id, JavaScriptSupportLoader.TYPESCRIPT.id)
    fun isJavaScriptApplicable(language: Language) =
        supportedLanguages.contains(language.id) || language is HTMLLanguage || language is JsonLanguage || language is TypeScriptJSXLanguageDialect
}