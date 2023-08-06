package cc.unitmesh.devti.provider

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.builtin.DefaultContextPrompter
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

abstract class ContextPrompter : LazyExtensionInstance<ContextPrompter>() {
    protected var action: ChatActionType? = null
    protected var selectedText: String = ""
    protected var file: PsiFile? = null
    protected var project: Project? = null
    protected var lang: String = ""
    protected var offset: Int = 0

    private val chatContextCache: MutableMap<ChatCreationContext, String> = mutableMapOf()

    suspend fun collectionContext(creationContext: ChatCreationContext): String {
        if (chatContextCache.containsKey(creationContext)) {
            val cachedContent = chatContextCache[creationContext]!!
            if (cachedContent.isNotEmpty()) {
                return cachedContent
            }
        }

        var chatContext = ""

        val contextItems = ChatContextProvider.collectChatContextList(project!!, creationContext)
        contextItems.forEach {
            chatContext += it.text + "\n"
        }

        logger<ContextPrompter>().info("context: $chatContext")

        chatContextCache[creationContext] = chatContext
        return chatContext
    }

    open fun initContext(
        actionType: ChatActionType,
        selectedText: String,
        file: PsiFile?,
        project: Project,
        offset: Int,
        element: PsiElement? = null,
    ) {
        this.action = actionType
        this.selectedText = selectedText
        this.file = file
        this.project = project
        this.lang = file?.language?.displayName ?: ""
        this.offset = offset
    }

    @Attribute("language")
    var language: String? = null

    @Attribute("implementationClass")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? {
        return implementationClass
    }

    open fun appendAdditionContext(context: String) {}
    open fun displayPrompt(): String = ""
    open fun requestPrompt(): String = ""

    companion object {
        private val EP_NAME: ExtensionPointName<ContextPrompter> =
            ExtensionPointName.create("cc.unitmesh.contextPrompter")

        fun prompter(lang: String): ContextPrompter {
            val langLowercase = lang.lowercase()
            val extensionList = EP_NAME.extensionList
            val contextPrompter = filterByLang(extensionList, langLowercase)

            val prompter = if (contextPrompter.isNotEmpty()) {
                contextPrompter.first()
            } else {
                // if lang == "TypeScript JSX", we just use TypeScript
                val firstPartLang = langLowercase.split(" ")[0]
                val partLang = filterByLang(extensionList, firstPartLang)
                if (partLang.isNotEmpty()) {
                    partLang[0]
                } else {
                    logger<ContextPrompter>().warn("No context prompter found for language $lang, will use default")
                    DefaultContextPrompter()
                }
            }

            return prompter
        }

        private fun filterByLang(
            extensionList: List<ContextPrompter>,
            langLowercase: String,
        ): List<ContextPrompter> {
            val contextPrompter = extensionList.filter {
                it.language?.lowercase() == langLowercase
            }

            return contextPrompter
        }
    }
}

