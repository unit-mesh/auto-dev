package cc.unitmesh.idea.provider

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.idea.MvcUtil
import com.intellij.openapi.project.Project

class TestContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.action == ChatActionType.WRITE_TEST
    }

    override fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val items = mutableListOf<ChatContextItem>()

        val isController = creationContext.sourceFile?.name?.let {
            MvcUtil.isController(it)
        } ?: false

        val isService = creationContext.sourceFile?.name?.let {
            MvcUtil.isService(it)
        } ?: false

        if (isController) {
            items.add(
                ChatContextItem(
                    TestContextProvider::class,
                    """
                        |You MUST use should_xx style for test method name.
                        |You MUST use MockMvc and test API only.
                        |You MUST use given-when-then style.
                        |You MUST use should_xx style for test method name.""".trimMargin()
                )
            )
        }

        if (isService) {
            items.add(
                ChatContextItem(
                    TestContextProvider::class,
                    """
                        |You MUST use should_xx style for test method name.
                        |You MUST use Mockito and test service only.
                        |You MUST use given-when-then style.
                        |You MUST use should_xx style for test method name. """.trimMargin()
                )
            )
        }

        return items
    }

}