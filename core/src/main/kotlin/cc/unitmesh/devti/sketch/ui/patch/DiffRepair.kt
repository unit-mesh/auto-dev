package cc.unitmesh.devti.sketch.ui.patch

import cc.unitmesh.devti.llms.LlmFactory
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

    templateRender.context = DiffRepairContext(oldCode, patchedCode)
    val prompt = templateRender.renderTemplate(template)

    val flow: Flow<String> = LlmFactory.create(project).stream(prompt, "", false)
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