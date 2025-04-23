package cc.unitmesh.devti.language.provider

import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import kotlin.reflect.KClass

class ToolchainContextItem(
    val clazz: KClass<*>,
    var text: String,
)

data class ToolchainPrepareContext(
    val sourceFile: PsiFile?,
    val element: PsiElement?,
    val extraItems: List<ToolchainContextItem> = emptyList(),
)

interface LanguageToolchainProvider {
    fun isApplicable(project: Project, context: ToolchainPrepareContext): Boolean

    @RequiresBackgroundThread
    suspend fun collect(project: Project, context: ToolchainPrepareContext): List<ToolchainContextItem>

    companion object {
        private val EP_NAME: LanguageExtension<LanguageToolchainProvider> =
            LanguageExtension("cc.unitmesh.shireLanguageToolchainProvider")

        suspend fun collectToolchainContext(
            project: Project,
            toolchainPrepareContext: ToolchainPrepareContext,
        ): List<ToolchainContextItem> {
            val elements = mutableListOf<ToolchainContextItem>()

            toolchainPrepareContext.sourceFile?.language?.let {
                val provider = EP_NAME.forLanguage(it)
                if (provider.isApplicable(project, toolchainPrepareContext)) {
                    elements.addAll(provider.collect(project, toolchainPrepareContext))
                }
            }

            elements.addAll(toolchainPrepareContext.extraItems)
            return elements.distinctBy { it.text }
        }
    }
}