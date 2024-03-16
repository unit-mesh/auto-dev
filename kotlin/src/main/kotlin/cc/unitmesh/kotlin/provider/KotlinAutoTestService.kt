package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.provider.context.TestFileContext
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.kotlin.util.KotlinPsiUtil
import cc.unitmesh.kotlin.util.getReturnTypeReferences
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getValueParameters
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import java.io.File

class KotlinAutoTestService : AutoTestService() {
    companion object {
        val log = logger<KotlinAutoTestService>()
    }

    override fun runConfigurationClass(project: Project): Class<out RunProfile> {
        return GradleRunConfiguration::class.java
    }

    override fun isApplicable(element: PsiElement): Boolean {
        return element.language is KotlinLanguage
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        val sourceFilePath = sourceFile.virtualFile
        val parentDir = sourceFilePath.parent
        val className = sourceFile.name.replace(".kt", "") + "Test"

        val packageName = ReadAction.compute<String, Throwable> {
            (sourceFile as KtFile).packageFqName.asString()
        }

        val parentDirPath = ReadAction.compute<String, Throwable> {
            parentDir?.path
        }

        val relatedModels = lookupRelevantClass(project, element).distinctBy { it.name }

        if (!parentDirPath?.contains("/main/kotlin/")!!) {
            log.error("Source file is not in the src/main/java directory: $parentDirPath")
            return null
        }

        var isNewFile = false

        val testDirPath = parentDir.path.replace("/main/kotlin/", "/test/kotlin/")
        var testDir = LocalFileSystem.getInstance().findFileByPath(testDirPath)

        if (testDir == null || !testDir.isDirectory) {
            isNewFile = true
            // Create the test directory if it doesn't exist
            val testDirFile = File(testDirPath)
            if (!testDirFile.exists()) {
                testDirFile.mkdirs()

                LocalFileSystem.getInstance().refreshAndFindFileByPath(testDirPath)?.let { refreshedDir ->
                    testDir = refreshedDir
                }
            }
        }

        val testDirCreated: VirtualFile? =
            VirtualFileManager.getInstance().refreshAndFindFileByUrl("file://$testDirPath")
        if (testDirCreated == null) {
            log.error("Failed to create test directory: $testDirPath")
            return null
        }

        // Test directory already exists, find the corresponding test file
        val testFilePath = testDirPath + "/" + sourceFile.name.replace(".kt", "Test.kt")
        val testFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)

        project.guessProjectDir()?.refresh(true, true)

        val currentClass: String = ReadAction.compute<String, Throwable> {
            val classContext = when (element) {
                is KtClassOrObject -> ClassContextProvider(false).from(element)
                is KtNamedFunction -> {
                    PsiTreeUtil.getParentOfType(element, KtClassOrObject::class.java)?.let {
                        ClassContextProvider(false).from(it)
                    }
                }

                else -> null
            }

            return@compute classContext?.format() ?: ""
        }

        val imports: List<String> = runReadAction {
            (sourceFile as KtFile).importList?.imports?.map { it.text } ?: emptyList()
        }

        return if (testFile != null) {
            TestFileContext(isNewFile, testFile, relatedModels, className, sourceFile.language, currentClass, imports)
        } else {
            val targetFile = createTestFile(sourceFile, testDir!!, packageName, project)
            TestFileContext(isNewFile = true, targetFile, relatedModels, "", sourceFile.language, currentClass, imports)
        }
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        return ReadAction.compute<List<ClassContext>, Throwable> {
            val elements = mutableListOf<ClassContext>()
            val projectPath = project.guessProjectDir()?.path

            val resolvedClasses = resolveByMethod(element)

            if (element is KtClassOrObject) {
                KotlinPsiUtil.getFunctions(element).forEach {
                    resolvedClasses.putAll(resolveByMethod(it))
                }

                resolvedClasses.putAll(resolveByFields(element))
            }

            // find the class in the same project
            resolvedClasses.forEach { (_, psiClass) ->
                val classPath = psiClass?.containingFile?.virtualFile?.path
                if (classPath?.contains(projectPath!!) == true) {
                    elements += ClassContextProvider(false).from(psiClass)
                }
            }

            elements
        }
    }

    private fun resolveByFields(element: KtClassOrObject): Map<out String, KtClass?> {
        val resolvedClasses = mutableMapOf<String, KtClass?>()
        element.primaryConstructorParameters.forEach {
            val typeReference = it.typeReference
            val elements = resolveType(typeReference)
            elements.forEach { element ->
                if (element is KtClass) {
                    resolvedClasses[element.name!!] = element
                }
            }
        }

        return resolvedClasses
    }


    private fun resolveByMethod(element: PsiElement): MutableMap<String, KtClass?> {
        val resolvedClasses = mutableMapOf<String, KtClass?>()
        when (element) {
            is KtNamedDeclaration -> {
                element.getValueParameters().map {
                    val typeReference = it.typeReference
                    resolveType(typeReference)
                }.forEach {
                    if (it is KtClass) {
                        resolvedClasses[it.name!!] = it
                    }
                }

                // with Generic returnType, like: ResponseEntity<List<Item>>
                element.getReturnTypeReferences().forEach { returnType ->
                    resolveType(returnType).filterIsInstance<KtClass>().forEach {
                        resolvedClasses[it.name!!] = it
                    }
                }
            }
        }

        return resolvedClasses
    }

    private fun resolveType(typeReference: KtTypeReference?): List<PsiElement> {
        if (typeReference == null) return emptyList()
        val result = mutableListOf<PsiElement>()
        when (val ktTypeElement = typeReference.typeElement) {
            is KtUserType -> {
                ktTypeElement.typeArguments.forEach {
                    result += resolveType(it.typeReference)
                }

                val typeElementReference = ktTypeElement.referenceExpression?.mainReference?.resolve()
                if (typeElementReference is KtClass) {
                    result += typeElementReference
                }
            }
        }

        return result
    }

    private fun createTestFile(
        sourceFile: PsiFile,
        testDir: VirtualFile,
        packageName: String,
        project: Project
    ): VirtualFile {
        val sourceFileName = sourceFile.name
        val testFileName = sourceFileName.replace(".kt", "Test.kt")
        val testFileContent = "package $packageName;\n\n"

        return WriteCommandAction.runWriteCommandAction<VirtualFile>(project) {
            val testFile = testDir.createChildData(this, testFileName)

            val document = FileDocumentManager.getInstance().getDocument(testFile)
            document?.setText(testFileContent)

            testFile
        }
    }
}
