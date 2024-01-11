package cc.unitmesh.idea.provider

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.idea.MvcUtil
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

open class JavaTestContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.action == ChatActionType.GENERATE_TEST && creationContext.sourceFile?.language is JavaLanguage
    }

    open fun langFileSuffix() = "java"

    var baseTestPrompt = """
            |- You MUST use should_xx_xx style for test method name.
            |- You MUST use given-when-then style.
            |- Test file should be complete and compilable, without need for further actions.
            |- Ensure that each test focuses on a single use case to maintain clarity and readability.
            |- Instead of using `@BeforeEach` methods for setup, include all necessary code initialization within each individual test method, do not write parameterized tests.
            |""".trimMargin()

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val fileName = creationContext.sourceFile?.name

        val isSpringRelated = checkIsSpringRelated(creationContext)

        val prompt = baseTestPrompt + junitRule(project)

        val finalPrompt = when {
            isController(fileName) && isSpringRelated -> {
                val testControllerPrompt = prompt + """
                            |- Use appropriate Spring test annotations such as `@MockBean`, `@Autowired`, `@WebMvcTest`, `@DataJpaTest`, `@AutoConfigureTestDatabase`, `@AutoConfigureMockMvc`, `@SpringBootTest` etc.
                            |""".trimMargin()

                ChatContextItem(JavaTestContextProvider::class, testControllerPrompt)
            }

            isService(fileName) && isSpringRelated -> {
                val testServicePrompt = prompt + """
                            |- Follow the common Spring code style by using the AssertJ library.
                            |- Assume that the database is empty before each test and create valid entities with consideration for data constraints (jakarta.validation.constraints).
                            |""".trimMargin()

                ChatContextItem(JavaTestContextProvider::class, testServicePrompt)
            }

            else -> {
                ChatContextItem(JavaTestContextProvider::class, prompt)
            }
        }

        return listOf(finalPrompt)
    }

    open fun checkIsSpringRelated(creationContext: ChatCreationContext) =
        runReadAction { creationContext.element?.let { isSpringRelated(it) } ?: false }

    protected fun isService(fileName: @NlsSafe String?) =
        fileName?.let { MvcUtil.isService(it, langFileSuffix()) } ?: false

    protected fun isController(fileName: @NlsSafe String?) =
        fileName?.let { MvcUtil.isController(it, langFileSuffix()) } ?: false

    protected fun junitRule(project: Project): String {
        var rule = ""
        prepareLibraryData(project)?.forEach {
            if (it.groupId?.contains("org.junit.jupiter") == true) {
                rule =
                    "- This project uses JUnit 5, you should import `org.junit.jupiter.api.Test` and use `@Test` annotation."
                return@forEach
            }

            if (it.groupId?.contains("org.junit") == true) {
                rule = "- This project uses JUnit 4, you should import `org.junit.Test` and use `@Test` annotation."
                return@forEach
            }
        }

        return rule
    }

    open fun isSpringRelated(element: PsiElement): Boolean {
        when (element) {
            is PsiMethod -> {
                val annotations = element.annotations
                for (annotation in annotations) {
                    val fqn = annotation.qualifiedName
                    if (fqn != null && fqn.startsWith("org.springframework")) {
                        return true
                    }
                }
            }

            is PsiClass -> {
                val annotations = element.annotations
                for (annotation in annotations) {
                    val fqn = annotation.qualifiedName
                    if (fqn != null && fqn.startsWith("org.springframework")) {
                        return true
                    }
                }
            }
        }

        return false
    }
}