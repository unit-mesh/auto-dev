package cc.unitmesh.database.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.database.util.DbUtil
import com.intellij.openapi.project.Project

class DatabaseSchemaContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return DbUtil.getDataSources(project).isNotEmpty
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val dataSources = DbUtil.getDataSources(project)
        if (dataSources.isEmpty) return emptyList()

        val infos = dataSources.mapNotNull {
            val dbNames = it.delegateDataSource?.databaseVersion ?: return@mapNotNull null
            val nameInfo = dbNames.name + " " + dbNames.version
            val text = "This project use $nameInfo"
            return@mapNotNull ChatContextItem(DatabaseSchemaContextProvider::class, text)
        }.toList()

        return infos
    }
}

