package cc.unitmesh.devti.indexer.provider

import cc.unitmesh.devti.indexer.model.DomainDictionary
import cc.unitmesh.devti.indexer.model.ElementType
import cc.unitmesh.devti.indexer.model.SemanticName
import cc.unitmesh.devti.indexer.naming.CamelCaseSplitter
import cc.unitmesh.devti.indexer.naming.LanguageSuffixRules
import cc.unitmesh.devti.indexer.scoring.FileWeightCalculator
import cc.unitmesh.devti.vcs.context.TokenCounter
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope

/**
 * Base class for language-specific dictionary providers.
 * Implements the two-level collection strategy with weight calculation:
 * - Level 1: Filenames with weights (low token cost, basic domain understanding)
 * - Level 2: Class names and public method names with weights (medium token cost, better domain understanding)
 *
 * Subclasses only need to provide language-specific rules and PSI element extraction.
 */
abstract class BaseLangDictProvider : LangDictProvider {
    protected val tokenCounter = TokenCounter.DEFAULT

    /**
     * Get language-specific suffix rules (e.g., remove "Controller", "Service" for Java)
     */
    protected abstract fun getSuffixRules(): LanguageSuffixRules

    /**
     * Get language-specific file filter
     */
    protected abstract fun shouldIncludeFile(fileName: String, filePath: String): Boolean

    /**
     * Extract classes and methods from a single Java file
     */
    protected open fun extractClassesAndMethods(
        javaFile: PsiJavaFile
    ): Pair<List<PsiClass>, List<PsiMethod>> {
        val classes = javaFile.classes.toList()
        val methods = classes.flatMap { getPublicMethods(it) }
        return Pair(classes, methods)
    }

    /**
     * Get public methods from a class (exclude private, protected, and test methods)
     */
    protected open fun getPublicMethods(psiClass: PsiClass): List<PsiMethod> {
        return psiClass.methods
            .filter { !it.name.startsWith("test") }  // Exclude test methods
            .toList()
    }

    /**
     * Get package name for a Java class (if available)
     */
    protected open fun getPackageName(psiClass: PsiClass): String {
        return (psiClass.containingFile as? PsiJavaFile)?.packageName ?: ""
    }

    /**
     * Collect semantic names in two levels based on token budget with weights
     */
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

    abstract suspend fun collectLevel1(project: Project): List<SemanticName>

    /**
     * Collect Level 2: Class names and public method names with weights
     * Token cost: ~1-2 tokens per name
     * Methods are tagged with their parent class name for aggregation
     */
    private suspend fun collectLevel2(
        project: Project,
        remainingTokenBudget: Int
    ): List<SemanticName> {
        val suffixRules = getSuffixRules()
        val names = mutableListOf<SemanticName>()
        var tokenUsed = 0

        // Step 1: Collect PSI data and VirtualFiles inside ReadAction
        data class ClassInfo(
            val psiClass: PsiClass,
            val className: String,
            val normalized: String,
            val vFile: com.intellij.openapi.vfs.VirtualFile,
            val packageName: String,
            val methods: List<PsiMethod>
        )

        val classInfoList = mutableListOf<ClassInfo>()

        runReadAction {
            val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, ProjectScope.getProjectScope(project))

            for (vFile in javaFiles) {
                if (!shouldIncludeFile(vFile.name, vFile.path)) continue

                val psiFile = runReadAction { vFile.findPsiFile(project) } ?: continue
                if (psiFile !is PsiJavaFile) continue

                val (classes, _) = extractClassesAndMethods(psiFile)

                for (psiClass in classes) {
                    val className = psiClass.name ?: continue
                    val normalized = suffixRules.normalize(className)
                    if (normalized.isEmpty()) continue

                    val packageName = getPackageName(psiClass)
                    val methods = getPublicMethods(psiClass)

                    classInfoList.add(
                        ClassInfo(
                            psiClass = psiClass,
                            className = className,
                            normalized = normalized,
                            vFile = vFile,
                            packageName = packageName,
                            methods = methods
                        )
                    )
                }
            }
        }

        // Step 2: Calculate weights OUTSIDE ReadAction (allows Git operations)
        for (classInfo in classInfoList) {
            if (tokenUsed > remainingTokenBudget) break

            val fileWeight = FileWeightCalculator.calculateWeight(project, classInfo.vFile)
            val classWeight = FileWeightCalculator.calculateClassWeight(project, classInfo.vFile, classInfo.psiClass)
            val classWeightCategory = FileWeightCalculator.getWeightCategory(classWeight)

            val tokenCost = tokenCounter.countTokens(classInfo.normalized)
            if (tokenUsed + tokenCost > remainingTokenBudget) continue

            names.add(
                SemanticName(
                    name = classInfo.normalized,
                    type = ElementType.CLASS,
                    tokens = tokenCost,
                    source = classInfo.className,
                    original = classInfo.className,
                    weight = classWeight,
                    packageName = classInfo.packageName,
                    weightCategory = classWeightCategory
                )
            )
            tokenUsed += tokenCost

            // Collect public method names (aggregate under class)
            for (method in classInfo.methods) {
                val methodName = method.name
                if (methodName.isEmpty() || methodName.startsWith("get") || methodName.startsWith("set")) continue

                val methodTokenCost = tokenCounter.countTokens(methodName)
                if (tokenUsed + methodTokenCost > remainingTokenBudget) continue

                names.add(
                    SemanticName(
                        name = methodName,
                        type = ElementType.METHOD,
                        tokens = methodTokenCost,
                        source = classInfo.className,
                        original = methodName,
                        weight = classWeight,  // Inherit class weight
                        packageName = classInfo.packageName,
                        parentClassName = classInfo.normalized,  // Link to parent class
                        weightCategory = classWeightCategory
                    )
                )
                tokenUsed += methodTokenCost
            }
        }

        return names.distinctBy { "${it.parentClassName}#${it.name}" }  // Remove duplicates
    }
}

/**
 * Helper to get PSI file from virtual file
 */
private fun com.intellij.openapi.vfs.VirtualFile.findPsiFile(project: Project): PsiFile? {
    return runReadAction {
        com.intellij.psi.PsiManager.getInstance(project).findFile(this)
    }
}
