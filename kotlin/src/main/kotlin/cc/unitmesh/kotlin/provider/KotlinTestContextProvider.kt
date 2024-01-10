package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.idea.provider.JavaTestContextProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode


class KotlinTestContextProvider : JavaTestContextProvider() {
    override fun langFileSuffix(): String = "kt"

    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.action == ChatActionType.GENERATE_TEST && creationContext.sourceFile?.language is KotlinLanguage
    }

    override fun isSpringRelated(element: PsiElement): Boolean {
        val imports = (element.containingFile as KtFile).importList?.imports?.map { it.text } ?: emptyList()
        when (element) {
            is KtNamedFunction -> {
                val annotations: List<KtAnnotationEntry> = element.annotationEntries
                for (annotation in annotations) {
                    val refName = annotation.typeReference?.text ?: continue
                    imports.forEach { import ->
                        if (!import.endsWith(refName)) {
                            return@forEach
                        }

                        if (import.contains("org.springframework.web.bind")) {
                            return true
                        }
                    }
                }
            }

            is KtClassOrObject -> {
                val annotations = element.annotationEntries
                for (annotation in annotations) {
                    val refName = annotation.typeReference?.text ?: continue
                    imports.forEach { import ->
                        if (!import.endsWith(refName)) {
                            return@forEach
                        }

                        if (import.contains("org.springframework.web.bind")) {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }
}
