package cc.unitmesh.devti.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.context.TestFileContext
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
abstract class AutoTestService : LazyExtensionInstance<AutoTestService>(), RunService {
    @Attribute("language")
    var language: String? = null

    @Attribute("implementation")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? = implementationClass

    abstract fun isApplicable(element: PsiElement): Boolean
    /**
     * Finds or creates a test file for the given source file, project, and element.
     *
     * @param sourceFile The source file for which to find or create a test file.
     * @param project The project in which the test file should be created.
     * @param psiElement The element for which the test file should be created.
     * @return The TestFileContext object representing the found or created test file, or null if it could not be found or created.
     *
     * This method is responsible for locating an existing test file associated with the given source file and element,
     * or creating a new test file if one does not already exist. The test file is typically used for unit testing purposes.
     * The source file, project, and element parameters are used to determine the context in which the test file should be created.
     * If a test file is found or created successfully, a TestFileContext object representing the test file is returned.
     * If a test file cannot be found or created, null is returned.
     */
    abstract fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, psiElement: PsiElement): TestFileContext?

    /**
     * Looks up the relevant classes in the project for the given element.
     *
     * @param project the project in which to perform the lookup
     * @param element the element for which to find the relevant classes
     * @return a list of ClassContext objects representing the relevant classes found in the project
     */
    abstract fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext>

    /**
     * This method is used to collect syntax errors from a given project and write them to an output file.
     * It takes the output file, the project to check, and an optional action to be executed with the list of errors.
     *
     * @param outputFile The virtual file where the syntax errors will be written to.
     * @param project The project to be analyzed for syntax errors.
     * @param runAction An optional lambda function that takes a list of strings as its parameter, representing the syntax errors.
     *                  If provided, this action is invoked with an empty list of errors, indicating no syntax errors were found.
     */
    open fun collectSyntaxError(outputFile: VirtualFile, project: Project, runAction: ((errors: List<String>) -> Unit)?) {
        runAction?.invoke(emptyList())
    }

    /**
     * Attempts to fix syntax errors in the given Kotlin file within the project.
     * This method is designed to be overridden by subclasses to provide custom syntax error fixing logic.
     *
     * @param outputFile The virtual file that needs to have its syntax errors fixed.
     * @param project The current project in which the file resides.
     */
    open fun tryFixSyntaxError(outputFile: VirtualFile, project: Project, issues: List<String>) {
        // send to chat panel
    }

    companion object {
        val log = logger<AutoTestService>()
        private val EP_NAME: ExtensionPointName<AutoTestService> =
            ExtensionPointName.create("cc.unitmesh.testContextProvider")

        fun context(psiElement: PsiElement): AutoTestService? {
            val extensionList = EP_NAME.extensionList
            val writeTestService = extensionList.firstOrNull {
                it.isApplicable(psiElement)
            }

            if (writeTestService == null) {
                return null
            }

            return writeTestService
        }
    }
}