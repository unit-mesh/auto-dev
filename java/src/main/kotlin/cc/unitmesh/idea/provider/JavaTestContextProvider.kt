package cc.unitmesh.idea.provider

import cc.unitmesh.devti.custom.test.TestTemplateFinder
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.idea.MvcUtil
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod


open class JavaTestContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.action == ChatActionType.GENERATE_TEST && creationContext.sourceFile?.language is JavaLanguage
    }

    open fun langFileSuffix() = "java"

    var baseTestPrompt = """
            |- You MUST use should_xx_xx style for test method name, You MUST use given-when-then style.
            |- Test file should be complete and compilable, without need for further actions.
            |- Ensure that each test focuses on a single use case to maintain clarity and readability.
            |- Instead of using `@BeforeEach` methods for setup, include all necessary code initialization within each individual test method, do not write parameterized tests.
            |""".trimMargin()

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val fileName = creationContext.sourceFile?.name

        val isSpringRelated = checkIsSpringRelated(creationContext)

        var prompt = baseTestPrompt + junitRule(project)

        val language = creationContext.sourceFile?.language?.displayName ?: "Java"

        val testPrompt = project.service<TestTemplateFinder>()
        val finalPrompt = when {
            isController(fileName) && isSpringRelated -> {
                var testControllerPrompt = prompt + """
                            |- Use appropriate Spring test annotations such as `@MockBean`, `@Autowired`, `@WebMvcTest`, `@DataJpaTest`, `@AutoConfigureTestDatabase`, `@AutoConfigureMockMvc`, `@SpringBootTest` etc.
                            |""".trimMargin()

                val lookup = testPrompt.lookup("ControllerTest.java")
                if (lookup != null) {
                    testControllerPrompt += "\nHere is the Test code template as example\n```$language\n$lookup\n```\n"
                }

                ChatContextItem(JavaTestContextProvider::class, testControllerPrompt)
            }

            isService(fileName) && isSpringRelated -> {
                var testServicePrompt = prompt + """
                            |- Follow the common Spring code style by using the AssertJ library.
                            |- Assume that the database is empty before each test and create valid entities with consideration for data constraints (jakarta.validation.constraints).
                            |""".trimMargin()

                val lookup = testPrompt.lookup("ServiceTest.java")
                if (lookup != null) {
                    testServicePrompt += "\nHere is the Test code template as example\n```$language\n$lookup\n```\n"
                }

                ChatContextItem(JavaTestContextProvider::class, testServicePrompt)
            }

            else -> {
                val lookup = testPrompt.lookup("Test.java")
                if (lookup != null) {
                    prompt += "\nHere is the Test code template as example\n```$language\n$lookup\n```\n"
                }
                ChatContextItem(JavaTestContextProvider::class, prompt)
            }
        }

        return listOf(finalPrompt)
    }

    open fun checkIsSpringRelated(creationContext: ChatCreationContext) =
        runReadAction { creationContext.element?.let { isSpringRelated(it) } ?: false }

    protected fun isService(fileName: String?) =
        fileName?.let { MvcUtil.isService(it, langFileSuffix()) } ?: false

    protected fun isController(fileName: String?) =
        fileName?.let { MvcUtil.isController(it, langFileSuffix()) } ?: false


    private val projectJunitCache = mutableMapOf<Project, String>()

    /**
     * Returns a string representing the JUnit rule for the given project.
     *
     * @param project The project for which to retrieve the JUnit rule.
     * @return A string representing the JUnit rule for the project.
     *
     * This method checks if the project already has a JUnit rule in the projectJunitCache.
     * If it does, it returns the cached rule. Otherwise, it prepares the library data for the project
     * and checks if the project uses JUnit 5 or JUnit 4. Based on the result, it sets the rule string
     * accordingly. Finally, it adds the rule to the projectJunitCache and returns the rule string.
     */
    protected fun junitRule(project: Project): String {
        if (projectJunitCache.containsKey(project)) {
            return projectJunitCache[project]!!
        }

        var rule = ""
        val libraryData = prepareLibraryData(project)
        libraryData?.forEach {
            when (it.groupId) {
                "org.assertj" -> {
                    rule += "- This project uses AssertJ, you should import `org.assertj.core.api.Assertions` and use `assertThat` method."
                }
                "org.junit.jupiter" -> {
                    rule +=
                        "- This project uses JUnit 5, you should import `org.junit.jupiter.api.Test` and use `@Test` annotation."
                }
                "junit" -> {
                    rule += "- This project uses JUnit 4, you should import `org.junit.Test` and use `@Test` annotation."
                }
            }
        }

        projectJunitCache[project] = rule
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