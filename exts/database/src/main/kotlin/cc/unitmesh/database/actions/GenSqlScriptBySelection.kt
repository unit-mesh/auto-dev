package cc.unitmesh.database.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.template.TemplateRender
import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.psi.DbPsiFacade
import com.intellij.database.util.DasUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking


class GenSqlScriptBySelection : AbstractChatIntention() {
    override fun priority(): Int = 1001
    override fun startInWriteAction(): Boolean = false
    override fun getFamilyName(): String = AutoDevBundle.message("migration.database.plsql")
    override fun getText(): String = AutoDevBundle.message("migration.database.sql.generate")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        DbPsiFacade.getInstance(project).dataSources.firstOrNull() ?: return false
        return true
    }

    private val logger = logger<GenSqlScriptBySelection>()

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return

        val dbPsiFacade = DbPsiFacade.getInstance(project)
        val dataSource = dbPsiFacade.dataSources.firstOrNull() ?: return

        val selectedText = editor.selectionModel.selectedText

        val rawDataSource = dbPsiFacade.getDataSourceManager(dataSource).dataSources.firstOrNull() ?: return
        val databaseVersion = rawDataSource.databaseVersion
        val schemaName = rawDataSource.name.substringBeforeLast('@')
        val dasTables = rawDataSource.let {
            val tables = DasUtil.getTables(it)
            tables.filter { table -> table.kind == ObjectKind.TABLE && table.dasParent?.name == schemaName }
        }.toList()

        val dbContext = DbContext(
            requirement = selectedText ?: "",
            databaseVersion = databaseVersion.let {
                "name: ${it.name}, version: ${it.version}"
            },
            schemaName = schemaName,
            tableNames = dasTables.map { it.name },
        )

        val actions = DbContextActionProvider(dasTables)

        sendToChatPanel(project) { contentPanel, _ ->
            val llmProvider = LlmFactory().create(project)
            val prompter = GenSqlFlow(dbContext, actions, contentPanel, llmProvider)
            ApplicationManager.getApplication().invokeLater {

                ProgressManager.getInstance()
                    .run(generateSqlWorkflow(project, prompter, editor))
            }
        }
    }

    private fun generateSqlWorkflow(
        project: Project,
        flow: GenSqlFlow,
        editor: Editor,
    ) =
        object : Task.Backgroundable(project, "Loading retained test failure", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.fraction = 0.2


                indicator.text = AutoDevBundle.message("migration.database.sql.generate.clarify")
                val tables = flow.clarify()

                logger.info("Tables: $tables")
                // tables will be list in string format, like: `[table1, table2]`, we need to parse to Lists
                val tableNames = tables.substringAfter("[").substringBefore("]")
                    .split(", ").map { it.trim() }

                indicator.fraction = 0.6
                indicator.text = AutoDevBundle.message("migration.database.sql.generate.generate")
                val sqlScript = flow.generate(tableNames)

                logger.info("SQL Script: $sqlScript")
                WriteCommandAction.runWriteCommandAction(project, "Gen SQL", "cc.unitmesh.livingDoc", {
                    editor.document.insertString(editor.caretModel.offset, sqlScript)
                })

                indicator.fraction = 1.0
            }
        }
}

class GenSqlFlow(
    val dbContext: DbContext,
    val actions: DbContextActionProvider,
    val ui: ChatCodingPanel,
    val llm: LLMProvider
) {
    private val logger = logger<GenSqlFlow>()

    fun clarify(): String {
        val stepOnePrompt = generateStepOnePrompt(dbContext, actions)
        try {
            ui.addMessage(stepOnePrompt, true, stepOnePrompt)
            // for answer
            ui.addMessage(AutoDevBundle.message("autodev.loading"))
        } catch (e: Exception) {
            logger.error("Error: $e")
        }

        return runBlocking {
            val prompt = llm.stream(stepOnePrompt, "")
            return@runBlocking ui.updateMessage(prompt)
        }
    }

    fun generate(tableNames: List<String>): String {
        val stepTwoPrompt = generateStepTwoPrompt(dbContext, actions, tableNames)
        try {
            ui.addMessage(stepTwoPrompt, true, stepTwoPrompt)
            // for answer
            ui.addMessage(AutoDevBundle.message("autodev.loading"))
        } catch (e: Exception) {
            logger.error("Error: $e")
        }

        return runBlocking {
            val prompt = llm.stream(stepTwoPrompt, "")
            return@runBlocking ui.updateMessage(prompt)
        }
    }

    private fun generateStepOnePrompt(context: DbContext, actions: DbContextActionProvider): String {
        val templateRender = TemplateRender("genius/sql")
        val template = templateRender.getTemplate("sql-gen-clarify.vm")

        templateRender.context = context
        templateRender.actions = actions

        val prompter = templateRender.renderTemplate(template)

        logger.info("Prompt: $prompter")
        return prompter
    }

    private fun generateStepTwoPrompt(
        dbContext: DbContext,
        actions: DbContextActionProvider,
        tableInfos: List<String>
    ): String {
        val templateRender = TemplateRender("genius/sql")
        val template = templateRender.getTemplate("sql-gen-design.vm")

        dbContext.tableInfos = actions.getTableColumns(tableInfos)

        templateRender.context = dbContext
        templateRender.actions = actions

        val prompter = templateRender.renderTemplate(template)

        logger.info("Prompt: $prompter")
        return prompter
    }
}


data class DbContext(
    val requirement: String,
    val databaseVersion: String,
    val schemaName: String,
    val tableNames: List<String>,
    // for step 2
    var tableInfos: List<String> = emptyList(),
)

data class DbContextActionProvider(val dasTables: List<DasTable>) {
    /**
     * Retrieves the columns of the specified tables.
     *
     * @param tables A list of table names to retrieve the columns from.
     * @return A list of column names from the specified tables.
     */
    fun getTableColumns(tables: List<String>): List<String> {
        return dasTables.flatMap { tableName ->
            if (tables.contains(tableName.name)) {
                DasUtil.getColumns(tableName).map {
                    "${it.name}: ${it.dasType.toDataType()}"
                }
            } else {
                emptyList()
            }
        }
    }
}
