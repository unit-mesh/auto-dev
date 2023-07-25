package cc.unitmesh.devti.provider.context

import cc.unitmesh.devti.gui.chat.ChatActionType
import com.intellij.psi.PsiFile

class ChatCreationContext(
    val origin: ChatOrigin,
    val action: ChatActionType,
    val sourceFile: PsiFile?,
    val extraItems: List<ChatContextItem> = emptyList()
)