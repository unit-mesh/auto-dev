package cc.unitmesh.python.indexer.provider

import cc.unitmesh.devti.indexer.provider.LangDictProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import com.jetbrains.python.PythonFileType

class PythonLangDictProvider : LangDictProvider {
    override suspend fun collectFileNames(project: Project, maxTokenLength: Int): List<String> {
        val searchScope = ProjectScope.getProjectScope(project)
        val javaFiles = runReadAction {
            FileTypeIndex.getFiles(PythonFileType.INSTANCE, searchScope)
        }

        val filenames = javaFiles.mapNotNull {
            it.nameWithoutExtension
        }

        return filenames
    }
}
