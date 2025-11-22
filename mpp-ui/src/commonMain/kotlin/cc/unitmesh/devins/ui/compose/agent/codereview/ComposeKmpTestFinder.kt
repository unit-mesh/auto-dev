package cc.unitmesh.devins.ui.compose.agent.codereview

import cc.unitmesh.codegraph.parser.Language
import cc.unitmesh.devins.workspace.Workspace

/**
 * Test finder for Compose Kotlin Multiplatform projects
 * Handles KMP source set structures like commonMain/commonTest, jvmMain/jvmTest, etc.
 */
class ComposeKmpTestFinder : BaseTestFinder() {
    /**
     * KMP source set mappings: (main source set, test source set)
     */
    private val kmpSourceSets = listOf(
        "commonMain" to "commonTest",
        "jvmMain" to "jvmTest",
        "jsMain" to "jsTest",
        "androidMain" to "androidUnitTest",
        "iosMain" to "iosTest",
        "appleMain" to "appleTest",
        "wasmJsMain" to "wasmJsTest"
    )

    override fun isApplicable(language: String?): Boolean {
        return language?.lowercase() in listOf("java", "kotlin")
    }

    override suspend fun findTestFiles(sourceFile: String, workspace: Workspace): List<TestFileInfo> {
        // Check if this is a KMP project structure by looking for any KMP source set patterns
        val isKmpStructure = kmpSourceSets.any { (mainSet, _) ->
            sourceFile.contains("/${mainSet}/kotlin/") || sourceFile.contains("/${mainSet}/java/")
        }

        if (!isKmpStructure) {
            // Not a KMP project, return empty to let other finders handle it
            return emptyList()
        }

        // Determine language
        val language = when {
            sourceFile.endsWith(".kt") -> Language.KOTLIN
            sourceFile.endsWith(".java") -> Language.JAVA
            else -> return emptyList()
        }

        val results = mutableListOf<TestFileInfo>()

        // Try to find matching test paths for each KMP source set
        for ((mainSet, testSet) in kmpSourceSets) {
            // Check if source file is in this source set
            val mainKotlinPattern = "/${mainSet}/kotlin/"
            val mainJavaPattern = "/${mainSet}/java/"

            when {
                sourceFile.contains(mainKotlinPattern) -> {
                    // Replace sourceSet/kotlin with testSet/kotlin and add Test suffix
                    val testPath = sourceFile
                        .replace(mainKotlinPattern, "/${testSet}/kotlin/")
                        .replace(Regex("\\.kt$"), "Test.kt")

                    val testFile = parseTestFile(testPath, language, workspace)
                    if (testFile.exists) {
                        results.add(testFile)
                    }
                }
                sourceFile.contains(mainJavaPattern) -> {
                    // Replace sourceSet/java with testSet/java and add Test suffix
                    val testPath = sourceFile
                        .replace(mainJavaPattern, "/${testSet}/java/")
                        .replace(Regex("\\.java$"), "Test.java")

                    val testFile = parseTestFile(testPath, language, workspace)
                    if (testFile.exists) {
                        results.add(testFile)
                    }
                }
            }
        }

        return results
    }
}
