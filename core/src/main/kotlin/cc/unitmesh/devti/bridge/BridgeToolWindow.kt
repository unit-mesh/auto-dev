package cc.unitmesh.devti.bridge

import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.sketch.SketchInputListener
import cc.unitmesh.devti.sketch.SketchToolWindow
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

class BridgeToolWindow(val myProject: Project, val myEditor: Editor?, private val showInput: Boolean = false) :
    SketchToolWindow(myProject, myEditor, showInput, ChatActionType.BRIDGE) {
    override val inputListener
        get() = object : SketchInputListener(project, chatCodingService, this) {
            init {
                // no super
                val template = templateRender.getTemplate("bridge.vm")
                var systemPrompt = ""
                val customContext = BridgeRunContext.create(project, null, "")

                systemPrompt = templateRender.renderTemplate(template, customContext)
                invokeLater {
                    toolWindow.addSystemPrompt(systemPrompt)
                }
            }
        }
}