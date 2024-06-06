package cc.unitmesh.database.flow

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.util.parser.parseCodeFromString
import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFileFactory
import com.intellij.sql.psi.SqlFile

class AutoSqlBackgroundTask(
    private val project: Project,
    private val flow: AutoSqlFlow,
    private val editor: Editor,
    private val language: Language
) : Task.Backgroundable(project, "Gen SQL", true) {
    private val logger = logger<AutoSqlBackgroundTask>()

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

        try {
            val sqlCode = parseCodeFromString(sqlScript).first()
            val sqlFile = runReadAction {
                PsiFileFactory.getInstance(project).createFileFromText("temp.sql", language, sqlCode)
                        as SqlFile
            }

            val errors = sqlFile.verifySqlElement()
            if (errors.isNotEmpty()) {
                val response = flow.fix(errors.joinToString("\n"))
                val code = parseCodeFromString(response).last()
                writeToFile(code, indicator)
            }
        } catch (e: Exception) {
            logger.error("SQL Script parse error: $e")
        }

        writeToFile(sqlScript, indicator)
        indicator.fraction = 1.0
    }

    private fun writeToFile(sqlScript: String, indicator: ProgressIndicator) {
        WriteCommandAction.runWriteCommandAction(project, "Gen SQL", "cc.unitmesh.livingDoc", {
            editor.document.insertString(editor.caretModel.offset, "\n")
            val code = parseCodeFromString(sqlScript).first()
            editor.document.insertString(editor.caretModel.offset + "\n".length, code)
        })
    }
}

fun SqlFile.verifySqlElement(): MutableList<String> {
    val errors = mutableListOf<String>()
    val visitor = object : SqlSyntaxCheckingVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is PsiErrorElement) {
                errors.add("Syntax error at position ${element.textRange.startOffset}: ${element.errorDescription}")
            }
            super.visitElement(element)
        }
    }

    this.accept(visitor)
    return errors
}

abstract class SqlSyntaxCheckingVisitor : com.intellij.psi.PsiElementVisitor() {
    override fun visitElement(element: PsiElement) {
        runReadAction {
            element.children.forEach { it.accept(this) }
        }
    }
}