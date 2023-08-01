package cc.unitmesh.devti.provider

import cc.unitmesh.devti.context.ClassContext
import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistryImpl
import com.intellij.execution.RunManager
import com.intellij.ide.actions.runAnything.RunAnythingPopupUI
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

data class TestFileContext(
    val isNewFile: Boolean,
    val file: VirtualFile,
    val relatedClasses: List<ClassContext> = emptyList(),
    val testClassName: String?,
    val language: Language,
)

abstract class WriteTestService : LazyExtensionInstance<WriteTestService>() {
    @Attribute("language")
    var language: String? = null

    @Attribute("implementation")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? {
        return implementationClass
    }

    abstract fun isApplicable(element: PsiElement): Boolean

    abstract fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext?
    abstract fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext>

    fun runTest(project: Project, virtualFile: VirtualFile) {
        val runManager = RunManager.getInstance(project)
        val allConfigurationsList = runManager.allConfigurationsList
        log.warn(virtualFile.nameWithoutExtension)
        val testConfig = allConfigurationsList.firstOrNull {
            it.name == virtualFile.nameWithoutExtension && it is GradleRunConfiguration
        }

        if (testConfig == null) {
            log.warn("Failed to find test configuration for: ${virtualFile.nameWithoutExtension}")
            return
        }

        val configurationSettings =
            runManager.findConfigurationByTypeAndName(testConfig.getType(), testConfig.name)

        if (configurationSettings == null) {
            log.warn("Failed to find test configuration for: ${virtualFile.nameWithoutExtension}")
            return
        }

        log.info("configurationSettings: $configurationSettings")
        runManager.selectedConfiguration = configurationSettings

        val executor: Executor = RunAnythingPopupUI.getExecutor()
        ExecutorRegistryImpl.RunnerHelper.run(
            project,
            testConfig,
            configurationSettings,
            DataContext.EMPTY_CONTEXT,
            executor
        )
    }

    companion object {
        val log = logger<WriteTestService>()
        private val EP_NAME: ExtensionPointName<WriteTestService> =
            ExtensionPointName.create("cc.unitmesh.testContextProvider")

        fun context(psiElement: PsiElement): WriteTestService? {
            val lang = psiElement.language.displayName
            val extensionList = EP_NAME.extensionList
            val providers = extensionList.filter {
                it.language?.lowercase() == lang.lowercase() && it.isApplicable(psiElement)
            }

            return providers.firstOrNull()
        }
    }
}