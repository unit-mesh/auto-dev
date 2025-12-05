package cc.unitmesh.devti.indexer.provider

import cc.unitmesh.devti.indexer.model.DomainDictionary
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface LangDictProvider {
    /**
     * Collect filenames as a simple list (backward compatible).
     * Used for basic domain understanding.
     */
    suspend fun collectFileNames(project: Project, maxTokenLength: Int): List<String>

    /**
     * Collect semantic names with structure (Level 1 + Level 2).
     * Includes class names and method names for better LLM context.
     * Can be overridden by subclasses to return DomainDictionary.
     */
    suspend fun collectSemanticNames(
        project: Project,
        maxTokenLength: Int
    ): DomainDictionary {
        // Default implementation: return Level 1 only (filenames)
        val names = collectFileNames(project, maxTokenLength)
        val semanticNames = names.map {
            cc.unitmesh.devti.indexer.model.SemanticName(
                name = it,
                type = cc.unitmesh.devti.indexer.model.ElementType.FILE,
                tokens = 1,
                source = it
            )
        }
        return DomainDictionary(semanticNames, emptyList())
    }

    companion object {
        private val EP_NAME: ExtensionPointName<LangDictProvider> =
            ExtensionPointName("cc.unitmesh.langDictProvider")

        suspend fun all(project: Project, maxTokenLength: Int): List<String> {
            return EP_NAME.extensions.flatMap { provider ->
                provider.collectFileNames(project, maxTokenLength)
            }
        }

        /**
         * Collect all semantic names from all language providers
         */
        suspend fun allSemantic(project: Project, maxTokenLength: Int): DomainDictionary {
            val allLevel1 = mutableListOf<cc.unitmesh.devti.indexer.model.SemanticName>()
            val allLevel2 = mutableListOf<cc.unitmesh.devti.indexer.model.SemanticName>()

            for (provider in EP_NAME.extensions) {
                try {
                    val dict = provider.collectSemanticNames(project, maxTokenLength)
                    allLevel1.addAll(dict.level1)
                    allLevel2.addAll(dict.level2)
                } catch (e: Exception) {
                    // Log and continue with next provider
                    e.printStackTrace()
                }
            }

            return DomainDictionary(allLevel1, allLevel2)
        }
    }
}