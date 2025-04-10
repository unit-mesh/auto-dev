package cc.unitmesh.devti.bridge

import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.sketch.SketchInputListener
import cc.unitmesh.devti.sketch.SketchToolWindow
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

class BridgeToolWindow(val myProject: Project, val myEditor: Editor?, private val showInput: Boolean = false) :
    SketchToolWindow(myProject, myEditor, showInput, ChatActionType.BRIDGE) {
    override val inputListener = object : SketchInputListener(project, chatCodingService, this) {
        override val template = templateRender.getTemplate("bridge.vm")
        override var systemPrompt = ""

        override fun collectSystemPrompt(): String = systemPrompt

        override suspend fun setup() {
            val customContext = BridgeRunContext.create(project, null, "")
            systemPrompt = templateRender.renderTemplate(template, customContext)
            toolWindow.addSystemPrompt(systemPrompt)
        }
    }
}