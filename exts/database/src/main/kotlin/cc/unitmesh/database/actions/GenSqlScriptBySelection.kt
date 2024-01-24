package cc.unitmesh.database.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.sendToChatWindow
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.template.TemplateRender
import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.psi.DbPsiFacade
import com.intellij.database.util.DasUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile


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
        val dbPsiFacade = DbPsiFacade.getInstance(project)
        val dataSource = dbPsiFacade.dataSources.firstOrNull() ?: return

        val selectedText = editor?.selectionModel?.selectedText

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
        val prompter = generateStepOnePrompt(dbContext, actions)

        sendToChatWindow(project, getActionType()) { panel, service ->
            service.handlePromptAndResponse(panel, object : ContextPrompter() {
                override fun displayPrompt(): String = prompter
                override fun requestPrompt(): String = prompter
            }, null, false)
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
}

data class DbContext(
    val requirement: String,
    val databaseVersion: String,
    val schemaName: String,
    val tableNames: List<String>,
    // for step 2
    val tableInfos: List<String> = emptyList(),
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
