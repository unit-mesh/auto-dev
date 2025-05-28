package cc.unitmesh.devti.gui.toolbar

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.settings.coder.coderSetting
import cc.unitmesh.devti.statusbar.AutoDevStatus
import cc.unitmesh.devti.statusbar.AutoDevStatusService
import cc.unitmesh.devti.template.GENIUS_CODE
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import javax.swing.JButton
import javax.swing.JComponent
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * Related prompts:
 * https://gist.github.com/transitive-bullshit/487c9cb52c75a9701d312334ed53b20c
 * https://www.reddit.com/r/ClaudeAI/comments/1jr52qj/here_is_claude_codes_compact_prompt/
 */
class SummaryMessagesAction : AnAction("Summary Messages", "Summary all current messages to memorize.md", AllIcons.Nodes.Target),
    CustomComponentAction {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val baseDir = project.coderSetting.state.teamPromptsDir
        val presentation = e.presentation

        copyMessages(project)

        AutoDevCoroutineScope.scope(project).launch {
            try {
                updatePresentation(presentation, AutoDevIcons.LOADING, false)
                AutoDevStatusService.notifyApplication(AutoDevStatus.InProgress)
                val promptDir = project.guessProjectDir()!!.toNioPath().resolve(baseDir)
                if (!promptDir.exists()) {
                    promptDir.createDirectories()
                }

                val file = promptDir.resolve("memories.md").toFile()
                if (!file.exists()) {
                    file.createNewFile()
                }

                val systemPrompt = buildPrompt(project)
                val userPrompt = copyMessages(project)

                val fileEditorManager = FileEditorManager.getInstance(project)
                ApplicationManager.getApplication().invokeAndWait {
                    val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                    if (virtualFile != null) {
                        fileEditorManager.setSelectedEditor(virtualFile, "text-editor")
                    }
                }

                val editor = fileEditorManager.selectedTextEditor
                val stream: Flow<String> = LlmFactory.create(project).stream(systemPrompt, userPrompt)
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
                updatePresentation(presentation, AllIcons.Nodes.Target, true)
            }
        }
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val button: JButton = object : JButton() {
            init {
                putClientProperty("ActionToolbar.smallVariant", true)
                putClientProperty("customButtonInsets", JBInsets(1, 1, 1, 1).asUIResource())

                setOpaque(false)
                addActionListener {
                    copyMessages(ActionToolbar.getDataContextFor(this).getData(CommonDataKeys.PROJECT))
                }
            }
        }

        return Wrapper(button).also {
            it.setBorder(JBUI.Borders.empty(0, 10))
        }
    }

    private fun copyMessages(project: Project?): String {
        val agentStateService = project?.getService(AgentStateService::class.java) ?: return ""
        val allText = agentStateService.getAllMessages().joinToString("\n") { it.content }
        return allText
    }

    private fun updatePresentation(presentation: Presentation, icon: javax.swing.Icon, enabled: Boolean) {
        presentation.icon = icon
        presentation.isEnabled = enabled
    }

    private suspend fun buildPrompt(project: Project): String {
        val templateRender = TemplateRender(GENIUS_CODE)
        val template = templateRender.getTemplate("memory.vm")

        val prompt = templateRender.renderTemplate(template)
        return prompt
    }
}
