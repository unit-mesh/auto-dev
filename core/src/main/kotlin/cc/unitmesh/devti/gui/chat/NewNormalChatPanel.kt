package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.sketch.SketchToolWindow
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor

class NewNormalChatPanel(
    override val chatCodingService: ChatCodingService,
    val disposable: Disposable?,
    override val editor: Editor?
) :
    SketchToolWindow(chatCodingService.project, editor, true, ChatActionType.BRIDGE) {
}