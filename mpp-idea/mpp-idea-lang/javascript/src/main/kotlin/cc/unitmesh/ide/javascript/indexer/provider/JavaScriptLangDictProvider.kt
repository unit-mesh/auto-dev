package cc.unitmesh.ide.javascript.indexer.provider

import cc.unitmesh.devti.indexer.provider.LangDictProvider
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope

class JavaScriptLangDictProvider : LangDictProvider {
    override suspend fun collectFileNames(project: Project, maxTokenLength: Int): List<String> {
        val searchScope = ProjectScope.getProjectScope(project)
        val jsFileType = FileTypeManager.getInstance().findFileTypeByName("JavaScript")
        val javaFiles = if (jsFileType != null) {
            runReadAction {
                FileTypeIndex.getFiles(jsFileType, searchScope)
            }
        } else {
            emptyList()
        }

        val filenames = javaFiles.mapNotNull {
            it.nameWithoutExtension
        }

        return filenames
    }
}
