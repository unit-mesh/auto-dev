package cc.unitmesh.devti.sketch

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.gui.chat.ui.AutoDevInputListener
import cc.unitmesh.devti.gui.chat.ui.AutoDevInputSection
import cc.unitmesh.devti.gui.chat.ui.AutoDevInputTrigger
import cc.unitmesh.devti.prompting.SimpleDevinPrompter
import cc.unitmesh.devti.template.GENIUS_CODE
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch

class SketchInputListener(
    private val project: Project,
    private val chatCodingService: ChatCodingService,
    private val toolWindow: SketchToolWindow
) : AutoDevInputListener,
    SimpleDevinPrompter() {
    override val template = templateRender.getTemplate("sketch-chat.vm")
    override val templateRender: TemplateRender get() = TemplateRender(GENIUS_CODE)

    override fun onStop(component: AutoDevInputSection) {
        chatCodingService.stop()
        toolWindow.hiddenProgressBar()
    }

    override fun onSubmit(component: AutoDevInputSection, trigger: AutoDevInputTrigger) {
        var prompt = component.text
        component.text = ""

        if (prompt.isEmpty() || prompt.isBlank()) {
            component.showTooltip(AutoDevBundle.message("chat.input.tips"))
            return
        }

        prompt = prompting(project, prompt, null)

        toolWindow.addRequestPrompt(prompt)

        ApplicationManager.getApplication().executeOnPooledThread {
            val flow = chatCodingService.makeChatBotRequest(prompt, true, emptyList())
            val suggestion = StringBuilder()

            AutoDevCoroutineScope.scope(project).launch {
                flow.cancellable().collect { char ->
                    suggestion.append(char)

                    invokeLater {
                        toolWindow.onUpdate(suggestion.toString())
                    }
                }

                toolWindow.onFinish(suggestion.toString())
            }
        }
    }
}