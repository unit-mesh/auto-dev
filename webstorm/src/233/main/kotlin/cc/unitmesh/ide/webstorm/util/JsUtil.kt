package cc.unitmesh.ide.webstorm.util

import com.intellij.javascript.testing.JSTestRunnerManager
import com.intellij.psi.PsiFile

object JsUtil {
    fun guessTestFrameworkName(file: PsiFile): String? {
        val findPackageDependentProducers =
            JSTestRunnerManager.getInstance().findPackageDependentProducers(file)

        val testRunConfigurationProducer = findPackageDependentProducers.firstOrNull()
        return testRunConfigurationProducer?.configurationType?.displayName
    }
}