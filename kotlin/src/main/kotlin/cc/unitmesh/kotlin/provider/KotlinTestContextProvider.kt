package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.idea.provider.JavaTestContextProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction


class KotlinTestContextProvider : JavaTestContextProvider() {
    override fun langFileSuffix(): String = "kt"

    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.action == ChatActionType.GENERATE_TEST && creationContext.sourceFile?.language is KotlinLanguage
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val fileName = creationContext.sourceFile?.name

        val isSpringRelated = checkIsSpringRelated(creationContext)

        baseTestPrompt += junitRule(project)

        val finalPrompt = when {
            isController(fileName) && isSpringRelated -> {
                val testControllerPrompt = baseTestPrompt + "\n" + """
                            |- Use appropriate Spring test annotations such as `@MockBean`, `@Autowired`, `@WebMvcTest`, `@DataJpaTest`, `@AutoConfigureTestDatabase`, `@AutoConfigureMockMvc`, `@SpringBootTest` etc.
                            |""".trimMargin()
                ChatContextItem(JavaTestContextProvider::class, testControllerPrompt)
            }

            isService(fileName) && isSpringRelated -> {
                val testServicePrompt = baseTestPrompt + "\n" + """
                            |- Follow the common Spring code style by using the AssertJ library.
                            |- Assume that the database is empty before each test and create valid entities with consideration for data constraints (jakarta.validation.constraints).
                            |""".trimMargin()

                ChatContextItem(JavaTestContextProvider::class, testServicePrompt)
            }

            else -> {
                ChatContextItem(JavaTestContextProvider::class, baseTestPrompt)
            }
        }

        return listOf(finalPrompt)
    }


    override fun isSpringRelated(element: PsiElement): Boolean {
        val imports = (element.containingFile as KtFile).importList?.imports?.map { it.text } ?: emptyList()
        when (element) {
            is KtNamedFunction -> {
                val annotations: List<KtAnnotationEntry> = element.annotationEntries
                for (annotation in annotations) {
                    val refName = annotation.typeReference?.text ?: continue
                    imports.forEach { import ->
                        // in some case is import *, so we need to check the end of the import
                        if (refName.endsWith("Mapping") && import.contains("import org.springframework.web.bind.annotation.*")) {
                            return true
                        }

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
                        if (refName.endsWith("RestController") && import.contains("import org.springframework.web.bind.annotation.*")) {
                            return true
                        }

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
