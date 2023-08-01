package cc.unitmesh.idea.provider

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.idea.MvcUtil
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project

open class JavaTestContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.action == ChatActionType.WRITE_TEST && creationContext.sourceFile?.language is JavaLanguage
    }

    open fun langFileSuffix() = "java"

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val items = mutableListOf<ChatContextItem>()

        val isController = creationContext.sourceFile?.name?.let {
            MvcUtil.isController(it, langFileSuffix())
        } ?: false

        val isService = creationContext.sourceFile?.name?.let {
            MvcUtil.isService(it, langFileSuffix())
        } ?: false

        val baseTestPrompt = """
            |You MUST use should_xx_xx style for test method name.
            |You MUST use given-when-then style.
            |""".trimMargin()

        items += when {
            isController -> {
                val testControllerPrompt = baseTestPrompt + """
                            |You MUST use MockMvc and test API only.
                            |""".trimMargin()
                ChatContextItem(JavaTestContextProvider::class, testControllerPrompt)
            }

            isService -> {
                val testServicePrompt = baseTestPrompt + """
                            |You MUST use Mock library and test service only.
                            |""".trimMargin()

                ChatContextItem(JavaTestContextProvider::class, testServicePrompt)
            }

            else -> {
                ChatContextItem(JavaTestContextProvider::class, baseTestPrompt)
            }
        }

        return items
    }

}