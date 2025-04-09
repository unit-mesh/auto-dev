package cc.unitmesh.devti.indexer

import cc.unitmesh.devti.AutoDevIcons
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

class DomainDictGenerateAction : AnAction() {
    init {
        presentationText("indexer.generate.domain", templatePresentation)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        AutoDevCoroutineScope.scope(project).launch {
            val names = LangDictProvider.all(project)

            val baseDir = project.coderSetting.state.teamPromptsDir
            val templateRender = TemplateRender(GENIUS_CODE)
            val template = templateRender.getTemplate("indexer.vm")
            val context = DomainDictGenerateContext(names.joinToString(", "))
            val prompt = templateRender.renderTemplate(template, context)

            try {
                this@DomainDictGenerateAction.templatePresentation.icon = AutoDevIcons.InProgress
                this@DomainDictGenerateAction.templatePresentation.isEnabled = false
                val result = StringBuilder()
                AutoDevStatusService.notifyApplication(AutoDevStatus.InProgress)
                LlmFactory.create(project).stream(prompt, "").collect {
                    result.append(it)
                }

                val dict = result.toString()
                val file = project.guessProjectDir()!!.toNioPath().resolve(baseDir).resolve("domain.csv").toFile()
                if (!file.exists()) {
                    file.createNewFile()
                }

                AutoDevStatusService.notifyApplication(AutoDevStatus.Done)
                file.writeText(dict)
            } catch (e: Exception) {
                AutoDevStatusService.notifyApplication(AutoDevStatus.Error)
                e.printStackTrace()
            } finally {
                this@DomainDictGenerateAction.templatePresentation.icon = AutoDevIcons.AI_COPILOT
                this@DomainDictGenerateAction.templatePresentation.isEnabled = true
            }
        }
    }
}

data class DomainDictGenerateContext(
    val code: String
) : TemplateContext