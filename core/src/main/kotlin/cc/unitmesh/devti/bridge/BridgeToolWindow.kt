package cc.unitmesh.devti.bridge

import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.sketch.SketchInputListener
import cc.unitmesh.devti.sketch.SketchToolWindow
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project

class BridgeToolWindow(val myProject: Project, val myEditor: Editor?, private val showInput: Boolean = false) :
    SketchToolWindow(myProject, myEditor, showInput, ChatActionType.BRIDGE) {
    override val inputListener = object : SketchInputListener(project, chatCodingService, this) {
        override val template = templateRender.getTemplate("bridge.vm")
        override var systemPrompt = ""

        override fun getInitPrompt(): String = systemPrompt

        override suspend fun setup() {
            invokeLater {
                val task = object : Task.Backgroundable(project, "Processing context", false) {
                    override fun run(indicator: ProgressIndicator) {
                        val customContext = BridgeRunContext.create(project, null, "")
                        systemPrompt = templateRender.renderTemplate(template, customContext)
                        toolWindow.addSystemPrompt(systemPrompt)
                    }
                }

                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            }
        }
    }
}