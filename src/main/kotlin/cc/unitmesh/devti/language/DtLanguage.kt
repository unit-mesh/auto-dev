package cc.unitmesh.devti.language

import com.intellij.lang.Language

object DtLanguage : Language("DevTi", "text/devti", "text/x-devti", "application/x-devti") {
    override fun isCaseSensitive() = true
    override fun getDisplayName() = "DevTi"
}
