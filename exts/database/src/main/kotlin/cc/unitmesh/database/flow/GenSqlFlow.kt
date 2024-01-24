package cc.unitmesh.database.flow

import cc.unitmesh.database.DbContextActionProvider
import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.util.LLMCoroutineScope
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking

class GenSqlFlow(
    val genSqlContext: GenSqlContext,
    val actions: DbContextActionProvider,
    val ui: ChatCodingPanel,
    val llm: LLMProvider,
    val project: Project
) {
    private val logger = logger<GenSqlFlow>()

    fun clarify(): String {
        val stepOnePrompt = generateStepOnePrompt(genSqlContext, actions)

        LLMCoroutineScope.scope(project).runCatching {
            ui.addMessage(stepOnePrompt, true, stepOnePrompt)
            ui.addMessage(AutoDevBundle.message("autodev.loading"))
        }.onFailure {
            logger.warn("Error: $it")
        }

        return runBlocking {
            val prompt = llm.stream(stepOnePrompt, "")
            return@runBlocking ui.updateMessage(prompt)
        }
    }

    fun generate(tableNames: List<String>): String {
        val stepTwoPrompt = generateStepTwoPrompt(genSqlContext, actions, tableNames)

        LLMCoroutineScope.scope(project).runCatching {
            ui.addMessage(stepTwoPrompt, true, stepTwoPrompt)
            ui.addMessage(AutoDevBundle.message("autodev.loading"))
        }.onFailure {
            logger.warn("Error: $it")
        }

        return runBlocking {
            val prompt = llm.stream(stepTwoPrompt, "")
            return@runBlocking ui.updateMessage(prompt)
        }
    }

    private fun generateStepOnePrompt(context: GenSqlContext, actions: DbContextActionProvider): String {
        val templateRender = TemplateRender("genius/sql")
        val template = templateRender.getTemplate("sql-gen-clarify.vm")

        templateRender.context = context
        templateRender.actions = actions

        val prompter = templateRender.renderTemplate(template)

        logger.info("Prompt: $prompter")
        return prompter
    }

    private fun generateStepTwoPrompt(
        genSqlContext: GenSqlContext,
        actions: DbContextActionProvider,
        tableInfos: List<String>
    ): String {
        val templateRender = TemplateRender("genius/sql")
        val template = templateRender.getTemplate("sql-gen-design.vm")

        genSqlContext.tableInfos = actions.getTableColumns(tableInfos)

        templateRender.context = genSqlContext
        templateRender.actions = actions

        val prompter = templateRender.renderTemplate(template)

        logger.info("Prompt: $prompter")
        return prompter
    }

    fun getAllTables(): List<String> {
        return actions.dasTables.map { it.name }
    }
}