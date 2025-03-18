package cc.unitmesh.devti.sketch.ui.patch

import cc.unitmesh.devti.llm2.model.ModelType
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.template.GENIUS_CODE
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch

fun applyDiffRepairSuggestion(project: Project, editor: Editor, oldCode: String, patchedCode: String) {
    val templateRender = TemplateRender(GENIUS_CODE)
    val template = templateRender.getTemplate("repair-diff.vm")

    val intention = project.getService(AgentStateService::class.java).buildOriginIntention()

    templateRender.context = DiffRepairContext(intention, patchedCode, oldCode)
    val prompt = templateRender.renderTemplate(template)

    val flow: Flow<String> = LlmFactory.create(project, ModelType.FastApply).stream(prompt, "", false)
    AutoDevCoroutineScope.Companion.scope(project).launch {
        val suggestion = StringBuilder()
        flow.cancellable().collect { char ->
            suggestion.append(char)
            val code = CodeFence.Companion.parse(suggestion.toString())
            if (code.text.isNotEmpty()) {
                ApplicationManager.getApplication().invokeLater({
                    runWriteAction {
                        editor.document.setText(code.text)
                    }
                }, ModalityState.defaultModalityState())
            }
        }
    }
}

fun applyDiffRepairSuggestionSync(
    project: Project,
    oldCode: String,
    patchedCode: String,
    callback: (newContent: String) -> Unit
) {
    val templateRender = TemplateRender(GENIUS_CODE)
    val template = templateRender.getTemplate("repair-diff.vm")

    val intention = project.getService(AgentStateService::class.java).buildOriginIntention()

    templateRender.context = DiffRepairContext(intention, patchedCode, oldCode)
    val prompt = templateRender.renderTemplate(template)

    val flow: Flow<String> = LlmFactory.create(project, ModelType.FastApply).stream(prompt, "", false)
    AutoDevCoroutineScope.Companion.scope(project).launch {
        val suggestion = StringBuilder()
        flow.cancellable().collect { char ->
            suggestion.append(char)
            return@collect
        }

        val code = CodeFence.Companion.parse(suggestion.toString())
        callback.invoke(code.text)
    }
}