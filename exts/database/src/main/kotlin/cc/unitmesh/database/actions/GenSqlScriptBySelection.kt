package cc.unitmesh.database.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention
import com.intellij.database.model.DasColumn
import com.intellij.database.model.ObjectKind
import com.intellij.database.psi.DbElement
import com.intellij.database.psi.DbPsiFacade
import com.intellij.database.util.DasUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile


class GenSqlScriptBySelection : AbstractChatIntention() {
    override fun priority(): Int = 1001

    override fun startInWriteAction(): Boolean = false

    override fun getFamilyName(): String = AutoDevBundle.message("migration.database.plsql")

    override fun getText(): String = AutoDevBundle.message("migration.database.sql.generate")

    override fun isAvailable(project: Project, editor: Editor?, psiFile: PsiFile?): Boolean {
        return true
    }

    override fun invoke(project: Project, editor: Editor?, psiFile: PsiFile?) {
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

        val prompt = """
            |Database: $databaseVersion
            |Requirement: $selectedText
            |Tables: ${dasTables.joinToString { it.name }}
        """.trimMargin()

        println(prompt)
    }
}
