package cc.unitmesh.idea.indexer.provider

import cc.unitmesh.devti.indexer.model.ElementType
import cc.unitmesh.devti.indexer.model.SemanticName
import cc.unitmesh.devti.indexer.naming.CamelCaseSplitter
import cc.unitmesh.devti.indexer.naming.LanguageSuffixRules
import cc.unitmesh.devti.indexer.scoring.FileWeightCalculator
import cc.unitmesh.idea.indexer.naming.JavaNamingRules
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope

/**
 * Java language-specific implementation of semantic name collection.
 * Extracts filenames, class names, and public method names for LLM context generation.
 * Automatically removes technical suffixes like Controller, Service, DTO, etc.
 */
class JavaLangDictProvider : BaseLangDictProvider() {
    override fun getSuffixRules(): LanguageSuffixRules = JavaNamingRules()

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

    override suspend fun collectLevel2(
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
            val vFile: VirtualFile,
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
private fun VirtualFile.findPsiFile(project: Project): PsiFile? {
    return runReadAction {
        PsiManager.getInstance(project).findFile(this)
    }
}
