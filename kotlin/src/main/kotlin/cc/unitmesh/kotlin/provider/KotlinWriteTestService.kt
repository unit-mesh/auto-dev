package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.context.FileContext
import cc.unitmesh.devti.context.FileContextProvider
import cc.unitmesh.devti.provider.TestFileContext
import cc.unitmesh.devti.provider.WriteTestService
import com.intellij.openapi.application.ApplicationManager
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
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtTypeReference
import java.io.File
import kotlin.jvm.internal.Ref

class KotlinWriteTestService : WriteTestService() {
    companion object {
        val log = logger<KotlinWriteTestService>()
    }

    override fun isApplicable(element: PsiElement): Boolean {
        return element.language is KotlinLanguage
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        val sourceFilePath = sourceFile.virtualFile
        val parentDir = sourceFilePath.parent
        val className = sourceFile.name.replace(".kt", "") + "Test"

        val parentDirPath: Ref.ObjectRef<String> = Ref.ObjectRef()
        val packageRef: Ref.ObjectRef<String> = Ref.ObjectRef()
        packageRef.element = ""
        ApplicationManager.getApplication().runReadAction {
            packageRef.element = (sourceFile as KtFile).packageFqName.asString()
            parentDirPath.element = parentDir?.path
        }

        val packageName = packageRef.element

        val relatedModels = lookupRelevantClass(project, element)

        // Check if the source file is in the src/main/java directory
        if (!parentDirPath.element?.contains("/src/main/kotlin/")!!) {
            log.error("Source file is not in the src/main/java directory: ${parentDirPath.element}")
            return null
        }

        var isNewFile = false

        // Find the test directory
        val testDirPath = parentDir.path.replace("/src/main/kotlin/", "/src/test/kotlin/")
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

        val result: Ref.ObjectRef<TestFileContext?> = Ref.ObjectRef()

        // Test directory already exists, find the corresponding test file
        val testFilePath = testDirPath + "/" + sourceFile.name.replace(".kt", "Test.kt")
        val testFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)

        project.guessProjectDir()?.refresh(true, true)

        if (testFile != null) {
            result.element = TestFileContext(isNewFile, testFile, relatedModels, className, sourceFile.language)
        } else {
            val targetFile = createTestFile(sourceFile, testDir!!, packageName, project)
            result.element = TestFileContext(isNewFile = true, targetFile, relatedModels, "", sourceFile.language)
        }

        return result.element
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<FileContext> {
        val result: Ref.ObjectRef<List<FileContext>> = Ref.ObjectRef()
        result.element = emptyList()

        ApplicationManager.getApplication().runReadAction {
            val elements = mutableListOf<FileContext>()
            val projectPath = project.guessProjectDir()?.path

            val resolvedClasses = resolveByMethod(element)
//            resolvedClasses.putAll(resolveByField(element))

//            if (element is PsiClass) {
//                element.methods.forEach { method ->
//                    resolvedClasses.putAll(resolveByMethod(method))
//                }
//            }

            // find the class in the same project
            resolvedClasses.forEach { (_, psiClass) ->
                val classPath = psiClass?.containingFile?.virtualFile?.path
                if (classPath?.contains(projectPath!!) == true) {
                    elements += FileContextProvider().from(psiClass)
                }
            }

            result.element = elements
        }

        return result.element
    }

    private fun resolveByMethod(element: PsiElement): MutableMap<String, KtFile?> {
        val resolvedClasses = mutableMapOf<String, KtFile?>()
        if (element is KtFunction) {
//            element.parameterList.parameters.filter {
////                it.type is PsiClassReferenceType
//                it.type is PsiClassReferenceType && it.type.canonicalText != "java.lang.String"
//            }.map {
//                resolvedClasses[it.name] = (it.type as PsiClassReferenceType).resolveFile()
//            }
            element.valueParameters.filter {
                it.typeReference is KtTypeReference
            }


//            val outputType = element.returnTypeElement?.type
//            if (outputType is PsiClassReferenceType) {
//                if (outputType.parameters.isNotEmpty()) {
//                    outputType.parameters.forEach {
//                        if (it is PsiClassReferenceType) {
//                            resolvedClasses[it.canonicalText] = outputType.resolveFile()
//                        }
//                    }
//                }
//
//                val canonicalText = outputType.canonicalText
//                resolvedClasses[canonicalText] = outputType.resolveFile()
//            }
        }

        return resolvedClasses
    }


    private fun resolveByField(element: PsiElement): Map<out String, PsiJavaFile?> {
        val file = element.containingFile as PsiJavaFile
        val psiClass = file.classes.firstOrNull() ?: return emptyMap()

        val resolvedClasses = mutableMapOf<String, PsiJavaFile?>()
        psiClass.fields.forEach { field ->
//            val fieldType = field.type
//            if (fieldType is PsiClassReferenceType) {
//                if (fieldType.parameters.isNotEmpty()) {
//                    fieldType.parameters.forEach {
//                        if (it is PsiClassReferenceType) {
//                            resolvedClasses[it.canonicalText] = it.resolveFile()
//                        }
//                    }
//                }
//
//                val canonicalText = fieldType.canonicalText
//                resolvedClasses[canonicalText] = fieldType.resolveFile()
//            }
        }

        return resolvedClasses
    }

    private fun createTestFile(
        sourceFile: PsiFile,
        testDir: VirtualFile,
        packageName: String,
        project: Project
    ): VirtualFile {
        val sourceFileName = sourceFile.name
        val testFileName = sourceFileName.replace(".java", "Test.java")
        val testFileContent = "package $packageName;\n\n"

        val testFileRef: Ref.ObjectRef<VirtualFile> = Ref.ObjectRef()

        WriteCommandAction.runWriteCommandAction(project) {
            val testFile = testDir.createChildData(this, testFileName)
            testFileRef.element = testFile

            val document = FileDocumentManager.getInstance().getDocument(testFile)
            document?.setText(testFileContent)
        }

        return testFileRef.element!!
    }
}

private fun KtTypeReference.resolveFile(): KtFile? {
    return this.containingFile.virtualFile?.let { PsiManager.getInstance(project).findFile(it) as KtFile }
}
