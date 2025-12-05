package cc.unitmesh.devti.prompting

import cc.unitmesh.devti.custom.compile.VariableTemplateCompiler
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import cc.unitmesh.devti.provider.devins.LanguageProcessor
import cc.unitmesh.devti.template.TemplateRender
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import cc.unitmesh.devti.intentions.action.getElementToAction
import kotlinx.coroutines.runBlocking
import com.intellij.openapi.application.runReadAction

abstract class SimpleDevinPrompter {
    abstract val templateRender: TemplateRender
    abstract val template: String

    suspend fun prompting(project: Project, userInput: String, editor: Editor?): String {
        val variableCompile = runReadAction {
            VariableTemplateCompiler.create(project, editor)
        }
        if (variableCompile == null) {
            val frameworkContext = collectFrameworkContext(editor, project)
            templateRender.addVariable("input", userInput)
            templateRender.addVariable("frameworkContext", frameworkContext)
            return templateRender.renderTemplate(template)
        }

        val postProcessors = LanguageProcessor.devin()
        val compiledTemplate = postProcessors?.compile(project, template) ?: template

        variableCompile.set("input", userInput)
        return variableCompile.compile(compiledTemplate)
    }

    fun collectFrameworkContext(myEditor: Editor?, project: Project): String {
        val editor = myEditor ?: FileEditorManager.getInstance(project).selectedTextEditor ?: return ""
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)

        val element = getElementToAction(project, editor)
        val creationContext =
            ChatCreationContext(ChatOrigin.Intention, ChatActionType.SKETCH, psiFile, listOf(), element = element)
        val contextItems: List<ChatContextItem> = runBlocking {
            return@runBlocking ChatContextProvider.collectChatContextList(project, creationContext)
        }

        return contextItems.joinToString("\n") { it.text }
    }
}