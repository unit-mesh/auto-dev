package cc.unitmesh.devti.bridge.provider

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

abstract class KnowledgeWebApiProvider : LazyExtensionInstance<KnowledgeWebApiProvider>() {
    @Attribute("implementationClass")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? = implementationClass

    abstract fun isApplicable(project: Project): Boolean

    abstract fun lookupApiCallTree(project: Project, httpMethod: String, httpUrl: String): List<PsiElement>

    companion object {
        val EP_NAME: ExtensionPointName<KnowledgeWebApiProvider> =
            ExtensionPointName.create("cc.unitmesh.knowledgeWebApiProvide")

        fun available(project: Project): List<KnowledgeWebApiProvider> {
            return EP_NAME.extensionList.filter { it.isApplicable(project) }
        }
    }
}