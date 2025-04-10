package cc.unitmesh.devti.indexer

import cc.unitmesh.devti.indexer.provider.LangDictProvider
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.settings.coder.coderSetting
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.presentationText
import cc.unitmesh.devti.statusbar.AutoDevStatus
import cc.unitmesh.devti.statusbar.AutoDevStatusService
import cc.unitmesh.devti.template.GENIUS_CODE
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.launch
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.indexer.usage.PromptEnhancer
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.vfs.LocalFileSystem

class DomainDictGenerateAction : AnAction() {
    init {
        presentationText("indexer.generate.domain", templatePresentation)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val presentation = event.presentation
        
        AutoDevCoroutineScope.scope(project).launch {
            val baseDir = project.coderSetting.state.teamPromptsDir
            val prompt = buildPrompt(project)

            try {
                updatePresentation(presentation, AutoDevIcons.InProgress, false)
                AutoDevStatusService.notifyApplication(AutoDevStatus.InProgress)
                val promptDir = project.guessProjectDir()!!.toNioPath().resolve(baseDir)
                if (!promptDir.exists()) {
                    promptDir.createDirectories()
                }

                logger<DomainDictGenerateAction>().debug("Prompt: $prompt")

                val file = promptDir.resolve("domain.csv").toFile()
                if (!file.exists()) {
                    file.createNewFile()
                }

                val fileEditorManager = FileEditorManager.getInstance(project)
                var editors: Array<FileEditor> = emptyArray()
                ApplicationManager.getApplication().invokeAndWait {
                    val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                    if (virtualFile != null) {
                        editors = fileEditorManager.openFile(virtualFile, true)
                        fileEditorManager.setSelectedEditor(virtualFile, "text-editor")
                    }
                }

                val editor = fileEditorManager.selectedTextEditor
                val stream: Flow<String> = LlmFactory.create(project).stream(prompt, "")
                val result = StringBuilder()

                stream.cancellable().collect { chunk ->
                    result.append(chunk)
                    WriteCommandAction.writeCommandAction(project).compute<Any, RuntimeException> {
                        editor?.document?.setText(result.toString())
                        editor?.caretModel?.moveToOffset(editor?.document?.textLength ?: 0)
                        editor?.scrollingModel?.scrollToCaret(ScrollType.RELATIVE)
                    }
                }

                AutoDevStatusService.notifyApplication(AutoDevStatus.Done)
            } catch (e: Exception) {
                AutoDevStatusService.notifyApplication(AutoDevStatus.Error)
                e.printStackTrace()
            } finally {
                // Restore icon and enable the action
                updatePresentation(presentation, AutoDevIcons.AI_COPILOT, true)
            }
        }
    }

    private suspend fun buildPrompt(project: Project): String {
        val names = LangDictProvider.all(project)
        val templateRender = TemplateRender(GENIUS_CODE)
        val template = templateRender.getTemplate("indexer.vm")
        val readmeMe = PromptEnhancer.readmeFile(project)

        val context = DomainDictGenerateContext(names.joinToString(", "), readmeMe)
        val prompt = templateRender.renderTemplate(template, context)
        return prompt
    }

    private fun updatePresentation(presentation: Presentation, icon: javax.swing.Icon, enabled: Boolean) {
        presentation.icon = icon
        presentation.isEnabled = enabled
    }
}

data class DomainDictGenerateContext(
    val code: String,
    val readme: String
) : TemplateContext
