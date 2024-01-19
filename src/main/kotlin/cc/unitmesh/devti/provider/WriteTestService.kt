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
    /**
     * Retrieves the run configuration class for the given project.
     *
     * @param project The project for which to retrieve the run configuration class.
     * @return The run configuration class for the project.
     */
    abstract fun runConfigurationClass(project: Project): Class<out RunProfile>?
    abstract fun isApplicable(element: PsiElement): Boolean
    /**
     * Finds or creates a test file for the given source file, project, and element.
     *
     * @param sourceFile The source file for which to find or create a test file.
     * @param project The project in which the test file should be created.
     * @param element The element for which the test file should be created.
     * @return The TestFileContext object representing the found or created test file, or null if it could not be found or created.
     *
     * This method is responsible for locating an existing test file associated with the given source file and element,
     * or creating a new test file if one does not already exist. The test file is typically used for unit testing purposes.
     * The source file, project, and element parameters are used to determine the context in which the test file should be created.
     * If a test file is found or created successfully, a TestFileContext object representing the test file is returned.
     * If a test file cannot be found or created, null is returned.
     */
    abstract fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext?
    /**
     * Looks up the relevant classes in the project for the given element.
     *
     * @param project the project in which to perform the lookup
     * @param element the element for which to find the relevant classes
     * @return a list of ClassContext objects representing the relevant classes found in the project
     */
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
                log.warn("Could not find language support for: ${psiElement.language}, make you have the plugin installed.")
                return null
            }

            return writeTestService
        }
    }
}