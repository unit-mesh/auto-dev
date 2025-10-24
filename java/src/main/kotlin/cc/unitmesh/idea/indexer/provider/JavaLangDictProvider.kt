package cc.unitmesh.idea.indexer.provider

import cc.unitmesh.devti.indexer.naming.LanguageSuffixRules
import cc.unitmesh.devti.indexer.provider.BaseLangDictProvider
import cc.unitmesh.idea.indexer.naming.JavaNamingRules

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
        // Only include Java source files
        if (!fileName.endsWith(".java")) {
            return false
        }

        // Exclude test files
        if (filePath.contains("/test/") || filePath.contains("\\test\\") ||
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
}
