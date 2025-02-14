package cc.unitmesh.devti.provider.context

import cc.unitmesh.devti.gui.chat.message.ChatActionType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

data class ChatCreationContext(
    val origin: ChatOrigin,
    val action: ChatActionType,
    val sourceFile: PsiFile?,
    val extraItems: List<ChatContextItem> = emptyList(),
    val element: PsiElement?
)