package cc.unitmesh.ide.javascript.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.flow.TaskFlow
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.intentions.action.base.ChatBaseIntention
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.ide.javascript.flow.DsComponent
import cc.unitmesh.ide.javascript.flow.ReactAutoPage
import cc.unitmesh.ide.javascript.util.LanguageApplicableUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking

class GenPageAction : ChatBaseIntention() {
    override fun priority(): Int = 1010
    override fun startInWriteAction(): Boolean = false
    override fun getFamilyName(): String = AutoDevBundle.message("frontend.generate")
    override fun getText(): String = AutoDevBundle.message("frontend.component.generate")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return LanguageApplicableUtil.isWebLLMContext(file)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val selectedText = editor.selectionModel.selectedText ?: return

        val reactAutoPage = ReactAutoPage(project, selectedText, editor)

        sendToChatPanel(project) { contentPanel, _ ->
            val llmProvider = LlmFactory().create(project)
            val context = AutoPageContext.build(reactAutoPage)
            val prompter = GenComponentFlow(context, contentPanel, llmProvider)

            val task = GenComponentTask(project, prompter, editor, reactAutoPage)
            ProgressManager.getInstance()
                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        }

    }
}

class GenComponentTask(
    private val project: Project,
    private val flow: GenComponentFlow,
    private val editor: Editor,
    private val autoPage: ReactAutoPage
) : Task.Backgroundable(project, "Gen Page", true) {
    override fun run(indicator: ProgressIndicator) {
        indicator.fraction = 0.2

        indicator.text = AutoDevBundle.message("frontend.page.generate.clarify")
        val components = flow.clarify()
        // tables will be list in string format, like: `[table1, table2]`, we need to parse to Lists
        val componentNames = components.substringAfter("[").substringBefore("]")
            .split(", ").map { it.trim() }

        val filterComponents = autoPage.filterComponents(componentNames)
        flow.context.components = filterComponents.map { it.format() }

        indicator.fraction = 0.6
        indicator.text = AutoDevBundle.message("frontend.page.generating")
        flow.design(filterComponents)

        indicator.fraction = 0.8

    }
}

data class AutoPageContext(
    val requirement: String,
    var pages: List<String>,
    val pageNames: List<String>,
    var components: List<String>,
    val componentNames: List<String>,
    val routes: List<String>,
) {
    companion object {
        fun build(reactAutoPage: ReactAutoPage): AutoPageContext {
            return AutoPageContext(
                requirement = reactAutoPage.userTask,
                pages = reactAutoPage.getPages().map { it.format() },
                pageNames = reactAutoPage.getPages().map { it.name },
                components = reactAutoPage.getComponents().map { it.format() },
                componentNames = reactAutoPage.getComponents().map { it.name },
                routes = reactAutoPage.getRoutes(),
            )
        }
    }
}

class GenComponentFlow(val context: AutoPageContext, val panel: ChatCodingPanel, val llm: LLMProvider) :
    TaskFlow<String> {
    override fun clarify(): String {
        val stepOnePrompt = generateStepOnePrompt(context)

        panel.addMessage(stepOnePrompt, true, stepOnePrompt)
        panel.addMessage(AutoDevBundle.message("autodev.loading"))

        return runBlocking {
            val prompt = llm.stream(stepOnePrompt, "")
            return@runBlocking panel.updateMessage(prompt)
        }
    }

    private fun generateStepOnePrompt(context: AutoPageContext): String {
        val templateRender = TemplateRender("genius/page")
        val template = templateRender.getTemplate("page-gen-clarify.vm")

        templateRender.context = context

        val prompter = templateRender.renderTemplate(template)
        return prompter
    }


    override fun design(context: Any): List<String> {
        val componentList = context as List<DsComponent>
        val stepTwoPrompt = generateStepTwoPrompt(componentList)

        panel.addMessage(stepTwoPrompt, true, stepTwoPrompt)
        panel.addMessage(AutoDevBundle.message("autodev.loading"))

        return runBlocking {
            val prompt = llm.stream(stepTwoPrompt, "")
            return@runBlocking panel.updateMessage(prompt)
        }.let { listOf(it) }
    }

    private fun generateStepTwoPrompt(selectedComponents: List<DsComponent>): String {
        val templateRender = TemplateRender("genius/page")
        val template = templateRender.getTemplate("page-gen-design.vm")

        context.pages = selectedComponents.map { it.format() }
        templateRender.context = context

        val prompter = templateRender.renderTemplate(template)
        return prompter
    }
}
