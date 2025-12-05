package cc.unitmesh.ide.javascript.util

import com.intellij.javascript.testing.JSTestRunnerManager
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiFile

object JsUtil {
    fun guessTestFrameworkName(file: PsiFile): String? {
        val findPackageDependentProducers =
            runReadAction { JSTestRunnerManager.getInstance().findPackageDependentProducers(file) }

        val testRunConfigurationProducer = findPackageDependentProducers.firstOrNull()
        return testRunConfigurationProducer?.configurationType?.displayName
    }
}