package cc.unitmesh.devti.language

import com.intellij.lang.Language

object DevInLanguage : Language("DevIn", "text/devin", "text/x-devin", "application/x-devin") {
    val INSTANCE: Language = DevInLanguage
    override fun isCaseSensitive() = true
    override fun getDisplayName() = "DevIn"
}