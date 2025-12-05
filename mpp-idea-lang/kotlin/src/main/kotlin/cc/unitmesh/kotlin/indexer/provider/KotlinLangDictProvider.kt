package cc.unitmesh.kotlin.indexer.provider

import cc.unitmesh.devti.indexer.model.DomainDictionary
import cc.unitmesh.devti.indexer.model.ElementType
import cc.unitmesh.devti.indexer.model.SemanticName
import cc.unitmesh.devti.indexer.naming.CamelCaseSplitter
import cc.unitmesh.devti.indexer.provider.LangDictProvider
import cc.unitmesh.devti.indexer.scoring.FileWeightCalculator
import cc.unitmesh.devti.vcs.context.TokenCounter
import cc.unitmesh.kotlin.indexer.naming.KotlinNamingRules
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

/**
 * Kotlin language-specific implementation of semantic name collection.
 * Extracts filenames, class names, and public function names for LLM context generation.
 *
 * Key differences from Java:
 * - Handles data classes, sealed classes, object declarations
 * - Collects top-level functions (not just class methods)
 * - Filters out auto-generated methods from data classes (component1, copy, etc.)
 * - Handles extension functions
 * - Automatically removes technical suffixes like Controller, Service, DTO, Kt, etc.
 */
class KotlinLangDictProvider : LangDictProvider {
    private val tokenCounter = TokenCounter.DEFAULT
    private val suffixRules = KotlinNamingRules()

    override suspend fun collectFileNames(project: Project, maxTokenLength: Int): List<String> {
        // For backward compatibility, return Level 1 only
        return collectLevel1(project).map { it.name }
    }

    override suspend fun collectSemanticNames(
        project: Project,
        maxTokenLength: Int
    ): DomainDictionary {
        val level1 = collectLevel1(project)
        val level1Tokens = level1.sumOf { it.tokens }

        // If Level 1 uses less than 50% of budget, collect Level 2
        val level2 = if (level1Tokens < maxTokenLength * 0.5) {
            collectLevel2(project, maxTokenLength - level1Tokens)
        } else {
            emptyList()
        }

        val metadata = mapOf(
            "level1_count" to level1.size,
            "level2_count" to level2.size,
            "total_tokens" to (level1Tokens + level2.sumOf { it.tokens }),
            "max_tokens" to maxTokenLength
        )

        return DomainDictionary(level1, level2, metadata)
    }

    /**
     * Collect Level 1: Filenames with weights and suffix removal
     * Token cost: ~0.5 tokens per file
     */
    private suspend fun collectLevel1(project: Project): List<SemanticName> {
        val names = mutableListOf<SemanticName>()

        // Step 1: Collect VirtualFiles and filenames inside ReadAction
        data class FileInfo(
            val vFile: com.intellij.openapi.vfs.VirtualFile,
            val fileName: String,
            val normalized: String
        )

        val fileInfoList = mutableListOf<FileInfo>()

        runReadAction {
            val kotlinFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, ProjectScope.getProjectScope(project))

            for (vFile in kotlinFiles) {
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

    /**
     * Collect Level 2: Class names and public function names with weights
     * Token cost: ~1-2 tokens per name
     * Functions are tagged with their parent class name for aggregation
     */
    private suspend fun collectLevel2(
        project: Project,
        remainingTokenBudget: Int
    ): List<SemanticName> {
        val names = mutableListOf<SemanticName>()
        var tokenUsed = 0

        // Step 1: Collect PSI data and VirtualFiles inside ReadAction
        data class ClassInfo(
            val ktClass: KtClass,
            val className: String,
            val normalized: String,
            val vFile: com.intellij.openapi.vfs.VirtualFile,
            val packageName: String,
            val functions: List<KtNamedFunction>
        )

        val classInfoList = mutableListOf<ClassInfo>()

        runReadAction {
            val kotlinFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, ProjectScope.getProjectScope(project))

            for (vFile in kotlinFiles) {
                if (!shouldIncludeFile(vFile.name, vFile.path)) continue

                val psiFile = PsiManager.getInstance(project).findFile(vFile) as? KtFile ?: continue

                // Extract classes from the file
                val classes = psiFile.declarations.filterIsInstance<KtClass>()

                for (ktClass in classes) {
                    val className = ktClass.name ?: continue
                    val normalized = suffixRules.normalize(className)
                    if (normalized.isEmpty()) continue

                    val packageName = psiFile.packageFqName.asString()
                    val functions = getPublicFunctions(ktClass)

                    classInfoList.add(
                        ClassInfo(
                            ktClass = ktClass,
                            className = className,
                            normalized = normalized,
                            vFile = vFile,
                            packageName = packageName,
                            functions = functions
                        )
                    )
                }
            }
        }

        // Step 2: Calculate weights OUTSIDE ReadAction (allows Git operations)
        for (classInfo in classInfoList) {
            if (tokenUsed > remainingTokenBudget) break

            val weight = FileWeightCalculator.calculateWeight(project, classInfo.vFile)
            val category = FileWeightCalculator.getWeightCategory(weight)

            val tokenCost = tokenCounter.countTokens(classInfo.normalized)
            if (tokenUsed + tokenCost > remainingTokenBudget) continue

            names.add(
                SemanticName(
                    name = classInfo.normalized,
                    type = ElementType.CLASS,
                    tokens = tokenCost,
                    source = classInfo.className,
                    original = classInfo.className,
                    weight = weight,
                    packageName = classInfo.packageName,
                    weightCategory = category
                )
            )
            tokenUsed += tokenCost

            // Collect public function names (aggregate under class)
            for (function in classInfo.functions) {
                val functionName = function.name ?: continue
                if (functionName.isEmpty() || shouldSkipFunction(functionName)) continue

                val functionTokenCost = tokenCounter.countTokens(functionName)
                if (tokenUsed + functionTokenCost > remainingTokenBudget) continue

                names.add(
                    SemanticName(
                        name = functionName,
                        type = ElementType.METHOD,
                        tokens = functionTokenCost,
                        source = classInfo.className,
                        original = functionName,
                        weight = weight,  // Inherit class weight
                        packageName = classInfo.packageName,
                        parentClassName = classInfo.normalized,  // Link to parent class
                        weightCategory = category
                    )
                )
                tokenUsed += functionTokenCost
            }
        }

        return names.distinctBy { "${it.parentClassName}#${it.name}" }  // Remove duplicates
    }

    internal fun shouldIncludeFile(fileName: String, filePath: String): Boolean {
        if (filePath.contains("src/test/") || filePath.contains("src\\test\\") ||
            fileName.endsWith("Test.kt") || fileName.endsWith("Tests.kt") ||
            fileName.endsWith("TestCase.kt") || fileName.endsWith("Mock.kt") ||
            fileName.endsWith("Spec.kt")) {
            return false
        }

        if (filePath.contains("/.gradle/") || filePath.contains("\\.gradle\\") ||
            filePath.contains("/generated/") || filePath.contains("\\generated\\") ||
            filePath.contains("/generated-sources/") || filePath.contains("\\generated-sources\\") ||
            filePath.contains("/build/generated/") || filePath.contains("\\build\\generated\\")) {
            return false
        }

        return true
    }

    private fun getPublicFunctions(ktClass: KtClass): List<KtNamedFunction> {
        return ktClass.declarations
            .filterIsInstance<KtNamedFunction>()
            .filter { function ->
                !function.isPrivate() && !function.name.isNullOrEmpty()
            }
            .toList()
    }

    internal fun shouldSkipFunction(functionName: String): Boolean {
        if (functionName.startsWith("test")) return true

        if (functionName.startsWith("component") ||
            functionName == "copy" ||
            functionName == "equals" ||
            functionName == "hashCode" ||
            functionName == "toString") {
            return true
        }

        if (functionName.startsWith("get") || functionName.startsWith("set")) {
            return true
        }

        return false
    }
}
