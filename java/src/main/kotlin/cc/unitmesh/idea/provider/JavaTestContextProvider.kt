package cc.unitmesh.idea.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.provider.TestContextProvider
import cc.unitmesh.devti.provider.TestFileContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.IndexNotReadyException
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
        var isNewFile = false

        // Check if the source file is in the src/main/java directory
        if (!sourceDir?.path?.contains("/src/main/java/")!!) {
            log.error("Source file is not in the src/main/java directory: $sourceDir")
            return null
        }

        // Find the test directory
        val testDirPath = sourceDir.path.replace("/src/main/java/", "/src/test/java/")
        val testDir = LocalFileSystem.getInstance().findFileByPath(testDirPath)

        if (testDir == null || !testDir.isDirectory) {
            isNewFile = true
            // Create the test directory if it doesn't exist
            val testDirFile = File(testDirPath)
            if (!testDirFile.exists()) {
                testDirFile.mkdirs()
                // Refresh the VirtualFileManager to make sure the newly created directory is visible in IntelliJ
                VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
            }
        }

        val testDirCreated = LocalFileSystem.getInstance().findFileByPath(testDirPath)
        if (testDirCreated == null) {
            log.error("Failed to create test directory: $testDirPath")
            return null
        }

        // Test directory already exists, find the corresponding test file
        val testFilePath = testDirPath + "/" + sourceFile.name.replace(".java", "Test.java")
        val testFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)

        // commit and update file index
        project.guessProjectDir()?.refresh(true, true)

        val relatedModels = lookupRelevantClass(project, element)

        return if (testFile != null) {
            TestFileContext(isNewFile, testFile, relatedModels)
        } else {
            val targetFile = createTestFile(sourceFile, testDir!!, packageName)
            TestFileContext(isNewFile = true, targetFile, relatedModels)
        }
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        val models = mutableListOf<ClassContext>()
        val projectPath = project.guessProjectDir()?.path

        val resolvedClasses = resolveByMethod(element)

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
            element.parameterList.parameters.filter {
                it.type is PsiClassReferenceType
            }.map {
                try {
                    resolvedClasses[it.name] = (it.type as PsiClassReferenceType).resolve()
                } catch (e: IndexNotReadyException) {
                    log.warn("Failed to resolve class: ${it.type.canonicalText}")
                }
            }

            val outputType = element.returnTypeElement?.type
            if (outputType is PsiClassReferenceType) {
                if (outputType.parameters.isNotEmpty()) {
                    outputType.parameters.forEach {
                        if (it is PsiClassReferenceType) {
                            try {
                                resolvedClasses[it.canonicalText] = it.resolve()
                            } catch (e: IndexNotReadyException) {
                                log.warn("Failed to resolve class: ${it.canonicalText}")
                            }
                        }
                    }
                }

                val canonicalText = outputType.canonicalText
                resolvedClasses[canonicalText] = outputType.resolve()
            }
        }

        return resolvedClasses
    }

    override fun insertTestCode(sourceFile: PsiFile, project: Project, code: String): Boolean {
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

        val rootElement = runReadAction {
            sourceFile.children.find { it is PsiClass } as? PsiClass
        } ?: return false

        ApplicationManager.getApplication().invokeLater {
            val psiElementFactory = PsiElementFactory.getInstance(project)

            val newTestMethod = psiElementFactory.createMethodFromText(code, rootElement)
            if (rootElement.findMethodsByName(newTestMethod.name, false).isNotEmpty()) {
                log.error("Method already exists in the class: ${newTestMethod.name}")
            }

            log.info("newTestMethod: ${newTestMethod.text}")

            WriteCommandAction.runWriteCommandAction(project) {
                val addedMethod: PsiMethod = rootElement.add(newTestMethod) as PsiMethod
                addedMethod.navigate(true)
            }

            project.guessProjectDir()?.refresh(true, true)
        }

        return true
    }

    override fun insertClassCode(sourceFile: PsiFile, project: Project, code: String): Boolean {
        log.info("start insertClassCode: $code")
        val psiTestFile = PsiManager.getInstance(project).findFile(sourceFile.virtualFile) ?: return false

        WriteCommandAction.runWriteCommandAction(project) {
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

        testDir.refresh(false, true)

        return testFile
    }
}