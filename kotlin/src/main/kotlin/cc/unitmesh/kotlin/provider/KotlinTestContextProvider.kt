package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.idea.provider.JavaTestContextProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction

class KotlinTestContextProvider : JavaTestContextProvider() {
    override fun langFileSuffix(): String = "kt"

    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.action == ChatActionType.GENERATE_TEST && creationContext.sourceFile?.language is KotlinLanguage
    }

    override fun isSpringRelated(element: PsiElement): Boolean {
        when (element) {
            is KtNamedFunction -> {
                val annotations = element.annotationEntries
                for (annotation in annotations) {
                    val fqn = annotation.name
                    if (fqn != null && fqn.startsWith("org.springframework")) {
                        return true
                    }
                }
            }

            is KtClassOrObject -> {
                val annotations = element.annotationEntries
                for (annotation in annotations) {
                    val fqn = annotation.name
                    if (fqn != null && fqn.startsWith("org.springframework")) {
                        return true
                    }
                }
            }
        }

        return false
    }
}
