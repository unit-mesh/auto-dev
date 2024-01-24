package cc.unitmesh.database.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention
import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.model.RawDataSource
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

        val tableColumns = DbContextProvider(dasTables).getTableColumns(dasTables.map { it.name })

        DbContext(
            requirement = selectedText ?: "",
            databaseVersion = databaseVersion.let {
                "name: ${it.name}, version: ${it.version}"
            },
            schemaName = schemaName,
            tableNames = dasTables.map { it.name },
        )
    }
}

data class DbContext(
    val requirement: String,
    val databaseVersion: String,
    val schemaName: String,
    val tableNames: List<String>,
    // for step 2
    val tableInfos: List<String> = emptyList(),
) {
}

data class DbContextProvider(val dasTables: List<DasTable>) {
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
