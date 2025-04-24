package cc.unitmesh.database.provider

import cc.unitmesh.database.util.DatabaseSchemaAssistant
import cc.unitmesh.database.util.SqlContextBuilder
import cc.unitmesh.devti.language.ast.variable.ToolchainVariable
import cc.unitmesh.devti.language.ast.variable.toolchain.DatabaseToolchainVariable
import cc.unitmesh.devti.language.provider.ToolchainVariableProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement


class DatabaseVariableProvider : ToolchainVariableProvider {
    override fun isResolvable(variable: ToolchainVariable, psiElement: PsiElement?, project: Project): Boolean {
        return variable is DatabaseToolchainVariable
    }

    override fun resolve(variable: ToolchainVariable, project: Project, editor: Editor, psiElement: PsiElement?): Any {
        if (variable !is DatabaseToolchainVariable) {
            return ""
        }

       return when (variable) {
            DatabaseToolchainVariable.DatabaseInfo -> SqlContextBuilder.buildDatabaseInfo(project)
            DatabaseToolchainVariable.Databases -> DatabaseSchemaAssistant.getDataSources(project)
            DatabaseToolchainVariable.Tables -> DatabaseSchemaAssistant.getAllTables(project)
            DatabaseToolchainVariable.Columns -> DatabaseSchemaAssistant.getTableColumns(project)
        }
    }
}

