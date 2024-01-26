package cc.unitmesh.database.flow

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.util.parser.parseCodeFromString
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

class AutoSqlTask(
    private val project: Project,
    private val flow: AutoSqlFlow,
    private val editor: Editor
) : Task.Backgroundable(project, "Gen SQL", true) {
    private val logger = logger<AutoSqlTask>()

    override fun run(indicator: ProgressIndicator) {
        indicator.fraction = 0.2

        indicator.text = AutoDevBundle.message("autosql.generate.clarify")
        val tables = flow.clarify()

        logger.info("Tables: $tables")
        // tables will be list in string format, like: `[table1, table2]`, we need to parse to Lists
        val tableNames = tables.substringAfter("[").substringBefore("]")
            .split(", ").map { it.trim() }

        if (tableNames.isEmpty()) {
            indicator.fraction = 1.0
            val allTables = flow.getAllTables()
            logger.warn("no table related: $allTables")
            return
        }

        indicator.fraction = 0.6
        indicator.text = AutoDevBundle.message("autosql.generate.generate")
        val sqlScript = flow.design(tableNames)[0]

        logger.info("SQL Script: $sqlScript")
        WriteCommandAction.runWriteCommandAction(project, "Gen SQL", "cc.unitmesh.livingDoc", {
            // new line
            editor.document.insertString(editor.caretModel.offset, "\n")
            // insert sql script
            val code = parseCodeFromString(sqlScript).first()
            editor.document.insertString(editor.caretModel.offset + "\n".length, code)
        })

        indicator.fraction = 1.0
    }
}