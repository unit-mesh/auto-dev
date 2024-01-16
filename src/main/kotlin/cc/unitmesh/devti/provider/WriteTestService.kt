package cc.unitmesh.devti.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.context.TestFileContext
import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistryImpl
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunProfile
import com.intellij.ide.actions.runAnything.RunAnythingPopupUI
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

/**
 * The `WriteTestService` class is an abstract class that provides a base implementation for writing tests in different programming languages.
 * It extends the `LazyExtensionInstance` class, which allows lazy initialization of the `WriteTestService` instances.
 *
 * @property language The programming language for which the test service is applicable.
 * @property implementationClass The fully qualified name of the implementation class.
 *
 * @constructor Creates a new instance of the `WriteTestService` class.
 */
abstract class WriteTestService : LazyExtensionInstance<WriteTestService>() {
    @Attribute("language")
    var language: String? = null

    @Attribute("implementation")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? = implementationClass
    abstract fun runConfigurationClass(project: Project): Class<out RunProfile>
    abstract fun isApplicable(element: PsiElement): Boolean
    abstract fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext?
    abstract fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext>

    fun runTest(project: Project, virtualFile: VirtualFile) {
        val runManager = RunManager.getInstance(project)
        val testConfig = runManager.allConfigurationsList.firstOrNull {
            val runConfigureClass = runConfigurationClass(project)
            it.name == virtualFile.nameWithoutExtension && (it.javaClass == runConfigureClass)
        }

        if (testConfig == null) {
            log.warn("Failed to find test configuration for: ${virtualFile.nameWithoutExtension}")
            return
        }

        val configurationSettings =
            runManager.findConfigurationByTypeAndName(testConfig.type, testConfig.name)

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
            val extensionList = EP_NAME.extensionList
            val writeTestService = extensionList.firstOrNull {
                it.isApplicable(psiElement)
            }

            if (writeTestService == null) {
                log.warn("Could not find WriteTestService for: ${psiElement.language}")
                return null
            }

            return writeTestService
        }
    }
}