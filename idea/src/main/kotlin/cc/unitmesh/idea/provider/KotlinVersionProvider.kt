package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinLanguage

class KotlinVersionProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        val sourceFile: PsiFile? = creationContext.sourceFile
        return sourceFile == null || sourceFile.language == KotlinLanguage.INSTANCE
    }

    override fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        return emptyList()
    }

    private fun isKotlinFile(psiFile: PsiFile?) =
        psiFile?.containingFile?.virtualFile?.extension?.equals("kt", true) ?: false
}