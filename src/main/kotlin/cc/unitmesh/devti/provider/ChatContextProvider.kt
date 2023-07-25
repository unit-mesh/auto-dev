package cc.unitmesh.devti.provider

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import kotlin.coroutines.Continuation

class ChatCreationContext(
    val origin: ChatOrigin,
    val action: ChatActionType,
    val sourceFile: PsiFile?,
    val extraItems: List<ChatContextItem>
)

class ChatContextItem(
    val clazz: Class<*>,
    var text: String
)

interface ChatContextProvider {
    @RequiresReadLock
    fun isApplicable(project: Project, chatCreationContext: ChatCreationContext): Boolean

    @RequiresBackgroundThread
    fun collect(
        project: Project,
        chatCreationContext: ChatCreationContext,
        continuation: Continuation<MutableList<ChatContextItem?>?>
    ): Any?

    fun filterItems(list: List<ChatContextItem?>, creationContext: ChatCreationContext): List<ChatContextItem?> {
        return list
    }

    class Companion {
        companion object {
            val EP_NAME = ExtensionPointName<ChatContextProvider>("cc.unitmesh.chatContextProvider")
        }
    }
}
