package cc.unitmesh.devti.provider

import cc.unitmesh.devti.context.ClassContext
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

data class TestFileContext(
    val isNewFile: Boolean,
    val file: VirtualFile,
    val relatedClass: List<ClassContext> = emptyList()
)

abstract class TestContextProvider : LazyExtensionInstance<TestContextProvider>() {
    val AUTO_DEV_PLACEHOLDER = "<AUTO_DEV_PLACEHOLDER>"

    @Attribute("language")
    var language: String? = null

    @Attribute("implementation")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? {
        return implementationClass
    }

    abstract fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext?
    abstract fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext>

    abstract fun insertTestCode(sourceFile: VirtualFile, project: Project, methodCode: String): Boolean
    abstract fun insertClassCode(sourceFile: VirtualFile, project: Project, code: String): Boolean

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