package cc.unitmesh.database.provider

import cc.unitmesh.database.util.SqlContextBuilder
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.openapi.project.Project
import com.intellij.sql.dialects.SqlLanguageDialect
import com.intellij.sql.psi.SqlFile
import com.intellij.sql.psi.SqlLanguage
import com.intellij.sql.psi.impl.SqlImplUtil

class SqlChatContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        val element = creationContext.element ?: return false
        return element.language is SqlLanguageDialect || element.language is SqlLanguage
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val file = creationContext.element?.containingFile as? SqlFile ?: return emptyList()
        val ds = SqlImplUtil.getDataSources(file).firstOrNull() ?: return emptyList()

        val currentNamespace = SqlContextBuilder.getCurrentNamespace(file) ?: return emptyList()
        val schema = SqlContextBuilder.getSchema(ds, currentNamespace) ?: return emptyList()

        val displayName: String = file.language.displayName
        val displayCode: String = creationContext.element?.text ?: ""
        val schemaDescription: String? = SqlContextBuilder.formatSchema(schema)

        val text = if (schemaDescription == null) {
            "```$displayName\n\nConsider the following database schema:\n\n$displayCode\n\nNo schema description available\n```"
        } else {
            "```$displayName\n\nConsider the following database schema:\n$schemaDescription\n```"
        }

        return listOf(ChatContextItem(SqlChatContextProvider::class, text))
    }
}

