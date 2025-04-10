package cc.unitmesh.devti.indexer.provider

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface LangDictProvider {
    suspend fun collectFileNames(project: Project): List<String>

    companion object {
        private val EP_NAME: ExtensionPointName<LangDictProvider> =
            ExtensionPointName("cc.unitmesh.langDictProvider")


        suspend fun all(project: Project): List<String> {
            return EP_NAME.extensions.flatMap { provider ->
                provider.collectFileNames(project)
            }
        }
    }
}