package cc.unitmesh.devti.provider

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

abstract class TestContextProvider : LazyExtensionInstance<TestContextProvider>() {
    @Attribute("language")
    var language: String? = null

    @Attribute("implementation")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? {
        return implementationClass
    }

    abstract fun prepareTestFile(sourceFile: PsiFile, project: Project): VirtualFile?

    abstract fun insertTestMethod(methodName: String, code: String): String

    companion object {
        private val EP_NAME: ExtensionPointName<TestContextProvider> =
            ExtensionPointName.create("cc.unitmesh.testContextProvider")

        fun context(lang: String): TestContextProvider? {
            val extensionList = EP_NAME.extensionList
            val providers = extensionList.filter {
                it.language?.lowercase() == lang.lowercase()
            }

            return if (providers.isEmpty()) {
                extensionList.first()
            } else {
                providers.first()
            }
        }
    }
}