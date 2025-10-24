package cc.unitmesh.idea.indexer.provider

import cc.unitmesh.devti.indexer.model.ElementType
import cc.unitmesh.devti.indexer.model.SemanticName
import cc.unitmesh.devti.indexer.naming.CamelCaseSplitter
import cc.unitmesh.devti.indexer.naming.LanguageSuffixRules
import cc.unitmesh.devti.indexer.provider.BaseLangDictProvider
import cc.unitmesh.devti.indexer.scoring.FileWeightCalculator
import cc.unitmesh.idea.indexer.naming.JavaNamingRules
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope

/**
 * Java language-specific implementation of semantic name collection.
 * Extracts filenames, class names, and public method names for LLM context generation.
 * Automatically removes technical suffixes like Controller, Service, DTO, etc.
 */
class JavaLangDictProvider : BaseLangDictProvider() {

    override fun getSuffixRules(): LanguageSuffixRules {
        return JavaNamingRules()
    }

    override fun shouldIncludeFile(fileName: String, filePath: String): Boolean {
        if (filePath.contains("src/test/") || filePath.contains("src\\test\\") ||
            fileName.endsWith("Test.java") || fileName.endsWith("Tests.java") ||
            fileName.endsWith("TestCase.java") || fileName.endsWith("Mock.java")) {
            return false
        }

        // Exclude generated code
        if (filePath.contains("/.gradle/") || filePath.contains("\\.gradle\\") ||
            filePath.contains("/generated/") || filePath.contains("\\generated\\") ||
            filePath.contains("/generated-sources/") || filePath.contains("\\generated-sources\\")) {
            return false
        }

        return true
    }

    /**
     * Collect Level 1: Filenames with weights and suffix removal
     * Token cost: ~0.5 tokens per file
     */
    override suspend fun collectLevel1(project: Project): List<SemanticName> {
        val suffixRules = getSuffixRules()
        val names = mutableListOf<SemanticName>()

        // Step 1: Collect VirtualFiles and filenames inside ReadAction
        data class FileInfo(
            val vFile: com.intellij.openapi.vfs.VirtualFile,
            val fileName: String,
            val normalized: String
        )

        val fileInfoList = mutableListOf<FileInfo>()

        runReadAction {
            val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, ProjectScope.getProjectScope(project))

            for (vFile in javaFiles) {
                if (!shouldIncludeFile(vFile.name, vFile.path)) continue

                // Get filename without extension
                val fileName = vFile.nameWithoutExtension

                // Normalize: remove suffixes
                val normalized = suffixRules.normalize(fileName)
                if (normalized.isEmpty()) continue

                fileInfoList.add(
                    FileInfo(
                        vFile = vFile,
                        fileName = fileName,
                        normalized = normalized
                    )
                )
            }
        }

        // Step 2: Calculate weights OUTSIDE ReadAction (allows Git operations)
        for (fileInfo in fileInfoList) {
            // Calculate weight
            val weight = FileWeightCalculator.calculateWeight(project, fileInfo.vFile)
            val category = FileWeightCalculator.getWeightCategory(weight)

            // Split into words for better LLM understanding
            val words = CamelCaseSplitter.split(fileInfo.normalized)

            // Create semantic names for each word
            for (word in words) {
                if (word.isNotEmpty()) {
                    val tokenCost = tokenCounter.countTokens(word)
                    names.add(
                        SemanticName(
                            name = word,
                            type = ElementType.FILE,
                            tokens = tokenCost,
                            source = fileInfo.vFile.name,
                            original = fileInfo.fileName,
                            weight = weight,
                            weightCategory = category
                        )
                    )
                }
            }
        }

        return names.distinctBy { it.name }  // Remove duplicates
    }

}
