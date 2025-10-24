package cc.unitmesh.devti.indexer

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.indexer.provider.LangDictProvider
import cc.unitmesh.devti.indexer.usage.PromptEnhancer
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.settings.AutoDevSettingsState
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
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

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
                updatePresentation(presentation, AutoDevIcons.LOADING, false)
                AutoDevStatusService.notifyApplication(AutoDevStatus.InProgress)
                val promptDir = project.guessProjectDir()!!.toNioPath().resolve(baseDir)
                if (!promptDir.exists()) {
                    promptDir.createDirectories()
                }

                logger<DomainDictGenerateAction>().debug("Prompt: $prompt")

                val file = promptDir.resolve("domain.csv").toFile()

                // Stream LLM response and write directly to file
                val stream: Flow<String> = LlmFactory.create(project).stream(prompt, "")
                val result = StringBuilder()

                stream.cancellable().collect { chunk ->
                    result.append(chunk)
                }

                file.writeText(result.toString())

                // After streaming is complete, open the file in editor
                ApplicationManager.getApplication().invokeLater {
                    val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                    if (virtualFile != null) {
                        FileEditorManager.getInstance(project).openFile(virtualFile, true)
                    } else {
                        logger<DomainDictGenerateAction>().warn("Failed to open domain.csv after generation")
                    }
                }

                AutoDevStatusService.notifyApplication(AutoDevStatus.Done)
            } catch (e: Exception) {
                AutoDevStatusService.notifyApplication(AutoDevStatus.Error)
                e.printStackTrace()
            } finally {
                updatePresentation(presentation, AutoDevIcons.AI_COPILOT, true)
            }
        }
    }

    private suspend fun buildPrompt(project: Project): String {
        val maxTokenLength = AutoDevSettingsState.maxTokenLength
        
        // Collect semantic names with Level 1 + Level 2 structure and weights
        val domainDict = LangDictProvider.allSemantic(project, maxTokenLength)
        
        // Use weighted list for better LLM attention
        val codeContext = domainDict.toWeightedList()
        
        val templateRender = TemplateRender(GENIUS_CODE)
        val template = templateRender.getTemplate("indexer.vm")
        val readmeMe = PromptEnhancer.readmeFile(project)

        // Log semantic extraction info with weight analysis
        val weightStats = domainDict.getWeightStatistics()
        logger<DomainDictGenerateAction>().info(
            "Domain Dictionary: ${domainDict.metadata["level1_count"]} files, " +
            "${domainDict.metadata["level2_count"]} classes/methods, " +
            "Total tokens: ${domainDict.getTotalTokens()}, " +
            "Weight: Avg=${String.format("%.2f", weightStats["averageWeight"])}, " +
            "Critical=${weightStats["criticalCount"]}, " +
            "High=${weightStats["highCount"]}"
        )

        if (codeContext.isEmpty()) {
            throw IllegalStateException("No code context found for domain dictionary generation")
        }

        val context = DomainDictGenerateContext(codeContext, readmeMe)
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
