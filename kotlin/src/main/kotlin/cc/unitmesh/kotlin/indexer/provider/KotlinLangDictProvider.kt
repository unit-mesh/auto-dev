package cc.unitmesh.kotlin.indexer.provider

import cc.unitmesh.devti.indexer.provider.LangDictProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.idea.KotlinFileType

class KotlinLangDictProvider : LangDictProvider {
    override suspend fun collectFileNames(project: Project): List<String> {
        val searchScope = ProjectScope.getProjectScope(project)
        val javaFiles = runReadAction {
            FileTypeIndex.getFiles(KotlinFileType.INSTANCE, searchScope)
        }

        val filenames = javaFiles.mapNotNull {
            it.nameWithoutExtension
        }

        return filenames
    }
}
