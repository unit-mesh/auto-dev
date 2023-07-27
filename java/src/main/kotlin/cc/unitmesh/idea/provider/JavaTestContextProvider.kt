package cc.unitmesh.idea.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.provider.TestContextProvider
import cc.unitmesh.devti.provider.TestFileContext
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import java.io.File

class JavaTestContextProvider : TestContextProvider() {
    companion object {
        val log = logger<JavaTestContextProvider>()
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        val sourceFilePath = sourceFile.virtualFile
        val sourceDir = sourceFilePath.parent

        val packageName = (sourceFile as PsiJavaFile).packageName

        // Check if the source file is in the src/main/java directory
        if (!sourceDir?.path?.contains("/src/main/java/")!!) {
            log.error("Source file is not in the src/main/java directory: $sourceDir")
            return null
        }

        // Find the test directory
        val testDirPath = sourceDir.path.replace("/src/main/java/", "/src/test/java/")
        val testDir = LocalFileSystem.getInstance().findFileByPath(testDirPath)

        if (testDir == null || !testDir.isDirectory) {
            // Create the test directory if it doesn't exist
            val testDirFile = File(testDirPath)
            if (!testDirFile.exists()) {
                testDirFile.mkdirs()
                // Refresh the VirtualFileManager to make sure the newly created directory is visible in IntelliJ
                VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
            }
        }

        val testDirCreated = LocalFileSystem.getInstance().refreshAndFindFileByPath(testDirPath)
        if (testDirCreated == null) {
            log.error("Failed to create test directory: $testDirPath")
            return null
        }


        // Test directory already exists, find the corresponding test file
        val testFilePath = testDirPath + "/" + sourceFile.name.replace(".java", "Test.java")
        val testFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)

        // update file index
        VirtualFileManager.getInstance().syncRefresh()

        val relatedModels = lookupRelevantClass(project, element)

        return if (testFile != null) {
            TestFileContext(false, testFile, relatedModels)
        } else {
            val targetFile = createTestFile(sourceFile, testDir!!, packageName)
            TestFileContext(true, targetFile, relatedModels)
        }
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        val models = mutableListOf<ClassContext>()
        val projectPath = project.guessProjectDir()?.path

        val resolvedClasses = try {
            resolveByMethod(element)
        } catch (e: Exception) {
            log.error("Failed to resolve class by method: ${e.message}")
            mutableMapOf()
        }

        if (element is PsiClass) {
            val methods = element.methods
            methods.forEach { method ->
                resolvedClasses.putAll(resolveByMethod(method))
            }
        }

        resolvedClasses.forEach { (_, psiClass) ->
            val classPath = psiClass?.containingFile?.virtualFile?.path
            if (classPath?.contains(projectPath!!) == true) {
                models.add(ClassContextProvider(false).from(psiClass))
            }
        }

        return models
    }

    // TODO: handle generic type
    private fun resolveByMethod(element: PsiElement): MutableMap<String, PsiClass?> {
        val resolvedClasses = mutableMapOf<String, PsiClass?>()
        if (element is PsiMethod) {
            element.parameterList.parameters.map {
                resolvedClasses[it.name] = (it.type as PsiClassReferenceType).resolve()
            }

            val outputType = element.returnTypeElement?.type
            if (outputType is PsiClassReferenceType) {
                if (outputType.parameters.isNotEmpty()) {
                    outputType.parameters.forEach {
                        if (it is PsiClassReferenceType) {
                            resolvedClasses[it.canonicalText] = it.resolve()
                        }
                    }
                }

                val canonicalText = outputType.canonicalText
                resolvedClasses[canonicalText] = outputType.resolve()
            }
        }

        return resolvedClasses
    }

    override fun insertTestMethod(sourceFile: PsiFile, project: Project, code: String): Boolean {
        // Check if the provided methodCode contains @Test annotation
        log.info("methodCode: $code")
        if (!code.contains("@Test")) {
            log.error("methodCode does not contain @Test annotation: $code")
            return false
        }

        // if code is a class code, we need to insert
        if (code.contains("public class ")) {
            return insertClassCode(sourceFile, project, code)
        }

        return runWriteAction {
            // Check if the root element (usually a class) of the source file is PsiClass
            val rootElement = sourceFile.children.find { it is PsiClass } as? PsiClass ?: return@runWriteAction false

            // Create the new test method
            val psiElementFactory = PsiElementFactory.getInstance(project)

            val newTestMethod = psiElementFactory.createMethodFromText(code, rootElement)

            // Check if the method already exists in the class
            if (rootElement.findMethodsByName(newTestMethod.name, false).isNotEmpty()) {
                log.error("Method already exists in the class: ${newTestMethod.name}")
                return@runWriteAction false
            }

            // Add the @Test annotation if it's missing
            val modifierList: PsiModifierList = newTestMethod.modifierList
            val testAnnotation: PsiAnnotation = psiElementFactory.createAnnotationFromText("@Test", newTestMethod)
            modifierList.add(testAnnotation)

            // Insert the new test method into the class
            val addedMethod: PsiMethod = rootElement.add(newTestMethod) as PsiMethod

            // Format the newly inserted code
            addedMethod.navigate(true)

            // Refresh the project to make the changes visible
            project.guessProjectDir()?.refresh(true, true)

            return@runWriteAction true
        }
    }

    override fun insertClassCode(sourceFile: PsiFile, project: Project, code: String): Boolean {
        val psiTestFile = PsiManager.getInstance(project).findFile(sourceFile.virtualFile) ?: return false

        WriteCommandAction.runWriteCommandAction(project) {
            // add code to test file by string
            val document = psiTestFile.viewProvider.document!!
            document.insertString(document.textLength, code)
        }

        return true
    }

    private fun createTestFile(sourceFile: PsiFile, testDir: VirtualFile, packageName: String): VirtualFile {
        val sourceFileName = sourceFile.name
        val testFileName = sourceFileName.replace(".java", "Test.java")
        val testFileContent = "package $packageName;\n\n"

        val testFile = testDir.createChildData(this, testFileName)
        testFile.setBinaryContent(testFileContent.toByteArray())

        // Refresh the test directory to make sure the test file is visible
        testDir.refresh(false, true)

        return testFile
    }
}