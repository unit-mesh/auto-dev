package cc.unitmesh.devti.indexer.provider

import cc.unitmesh.devti.indexer.model.DomainDictionary
import cc.unitmesh.devti.indexer.model.ElementType
import cc.unitmesh.devti.indexer.model.SemanticName
import cc.unitmesh.devti.indexer.naming.CamelCaseSplitter
import cc.unitmesh.devti.indexer.naming.LanguageSuffixRules
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
 * Implements the two-level collection strategy:
 * - Level 1: Filenames (low token cost, basic domain understanding)
 * - Level 2: Class names and public method names (medium token cost, better domain understanding)
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
     * Collect semantic names in two levels based on token budget
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
    
    /**
     * Collect Level 1: Filenames with suffix removal and word splitting
     * Token cost: ~0.5 tokens per file
     */
    private suspend fun collectLevel1(project: Project): List<SemanticName> {
        val suffixRules = getSuffixRules()
        val names = mutableListOf<SemanticName>()
        
        runReadAction {
            val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, ProjectScope.getProjectScope(project))
            
            for (vFile in javaFiles) {
                if (!shouldIncludeFile(vFile.name, vFile.path)) continue
                
                // Get filename without extension
                val fileName = vFile.nameWithoutExtension
                
                // Normalize: remove suffixes
                val normalized = suffixRules.normalize(fileName)
                if (normalized.isEmpty()) continue
                
                // Split into words for better LLM understanding
                val words = CamelCaseSplitter.split(normalized)
                
                // Create semantic names for each word
                for (word in words) {
                    if (word.isNotEmpty()) {
                        val tokenCost = tokenCounter.countTokens(word)
                        names.add(SemanticName(
                            name = word,
                            type = ElementType.FILE,
                            tokens = tokenCost,
                            source = vFile.name,
                            original = fileName
                        ))
                    }
                }
            }
        }
        
        return names.distinctBy { it.name }  // Remove duplicates
    }
    
    /**
     * Collect Level 2: Class names and public method names
     * Token cost: ~1-2 tokens per name
     */
    private suspend fun collectLevel2(
        project: Project,
        remainingTokenBudget: Int
    ): List<SemanticName> {
        val suffixRules = getSuffixRules()
        val names = mutableListOf<SemanticName>()
        var tokenUsed = 0
        
        runReadAction {
            val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, ProjectScope.getProjectScope(project))
            
            for (vFile in javaFiles) {
                if (!shouldIncludeFile(vFile.name, vFile.path)) continue
                if (tokenUsed > remainingTokenBudget) break
                
                val psiFile = runReadAction { vFile.findPsiFile(project) } ?: continue
                if (psiFile !is PsiJavaFile) continue
                
                val (classes, _) = extractClassesAndMethods(psiFile)
                
                // Collect class names
                for (psiClass in classes) {
                    val className = psiClass.name ?: continue
                    val normalized = suffixRules.normalize(className)
                    if (normalized.isEmpty()) continue
                    
                    val tokenCost = tokenCounter.countTokens(normalized)
                    if (tokenUsed + tokenCost > remainingTokenBudget) continue
                    
                    names.add(SemanticName(
                        name = normalized,
                        type = ElementType.CLASS,
                        tokens = tokenCost,
                        source = className,
                        original = className
                    ))
                    tokenUsed += tokenCost
                    
                    // Collect public method names
                    for (method in getPublicMethods(psiClass)) {
                        val methodName = method.name
                        if (methodName.isEmpty() || methodName.startsWith("get") || methodName.startsWith("set")) continue
                        
                        val methodTokenCost = tokenCounter.countTokens(methodName)
                        if (tokenUsed + methodTokenCost > remainingTokenBudget) continue
                        
                        names.add(SemanticName(
                            name = methodName,
                            type = ElementType.METHOD,
                            tokens = methodTokenCost,
                            source = className,
                            original = methodName
                        ))
                        tokenUsed += methodTokenCost
                    }
                }
            }
        }
        
        return names.distinctBy { it.name }
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
