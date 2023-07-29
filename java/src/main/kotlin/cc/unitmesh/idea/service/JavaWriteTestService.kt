package cc.unitmesh.idea.service

import cc.unitmesh.devti.context.FileContext
import cc.unitmesh.devti.context.FileContextProvider
import cc.unitmesh.devti.provider.TestFileContext
import cc.unitmesh.devti.provider.WriteTestService
import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistryImpl
import com.intellij.execution.RunManager
import com.intellij.ide.actions.runAnything.RunAnythingPopupUI.getExecutor
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import java.io.File
import kotlin.jvm.internal.Ref

class JavaWriteTestService : WriteTestService() {
    companion object {
        val log = logger<JavaWriteTestService>()
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        val sourceFilePath = sourceFile.virtualFile
        val parentDir = sourceFilePath.parent
        val className = sourceFile.name.replace(".java", "") + "Test"

        val parentDirPath: Ref.ObjectRef<String> = Ref.ObjectRef()
        val packageRef: Ref.ObjectRef<String> = Ref.ObjectRef()
        packageRef.element = ""
        ApplicationManager.getApplication().runReadAction {
            packageRef.element = (sourceFile as PsiJavaFile).packageName
            parentDirPath.element = parentDir?.path
        }

        val packageName = packageRef.element

        val relatedModels = lookupRelevantClass(project, element)

        // Check if the source file is in the src/main/java directory
        if (!parentDirPath.element?.contains("/src/main/java/")!!) {
            log.error("Source file is not in the src/main/java directory: ${parentDirPath.element}")
            return null
        }

        var isNewFile = false

        // Find the test directory
        val testDirPath = parentDir.path.replace("/src/main/java/", "/src/test/java/")
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
        val testFilePath = testDirPath + "/" + sourceFile.name.replace(".java", "Test.java")
        val testFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)

        project.guessProjectDir()?.refresh(true, true)

        if (testFile != null) {
            result.element = TestFileContext(isNewFile, testFile, relatedModels, className)
        } else {
            val targetFile = createTestFile(sourceFile, testDir!!, packageName, project)
            result.element = TestFileContext(isNewFile = true, targetFile, relatedModels, className)
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
            resolvedClasses.putAll(resolveByField(element))

            if (element is PsiClass) {
                element.methods.forEach { method ->
                    resolvedClasses.putAll(resolveByMethod(method))
                }
            }

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

    private fun resolveByMethod(element: PsiElement): MutableMap<String, PsiJavaFile?> {
        val resolvedClasses = mutableMapOf<String, PsiJavaFile?>()
        if (element is PsiMethod) {
            element.parameterList.parameters.filter {
                it.type is PsiClassReferenceType
            }.map {
                resolvedClasses[it.name] = (it.type as PsiClassReferenceType).resolveFile()
            }

            val outputType = element.returnTypeElement?.type
            if (outputType is PsiClassReferenceType) {
                if (outputType.parameters.isNotEmpty()) {
                    outputType.parameters.forEach {
                        if (it is PsiClassReferenceType) {
                            resolvedClasses[it.canonicalText] = outputType.resolveFile()
                        }
                    }
                }

                val canonicalText = outputType.canonicalText
                resolvedClasses[canonicalText] = outputType.resolveFile()
            }
        }

        return resolvedClasses
    }


    private fun resolveByField(element: PsiElement): Map<out String, PsiJavaFile?> {
        // find element's class and lookup fields
        val file = element.containingFile as PsiJavaFile
        val psiClass = file.classes.firstOrNull() ?: return emptyMap()

        val resolvedClasses = mutableMapOf<String, PsiJavaFile?>()
        psiClass.fields.forEach { field ->
            val fieldType = field.type
            if (fieldType is PsiClassReferenceType) {
                if (fieldType.parameters.isNotEmpty()) {
                    fieldType.parameters.forEach {
                        if (it is PsiClassReferenceType) {
                            resolvedClasses[it.canonicalText] = it.resolveFile()
                        }
                    }
                }

                val canonicalText = fieldType.canonicalText
                resolvedClasses[canonicalText] = fieldType.resolveFile()
            }
        }

        return resolvedClasses
    }

    override fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        log.info("methodCode: $code")
        if (!code.contains("@Test")) {
            log.error("methodCode does not contain @Test annotation: $code")
            return false
        }

        if (code.startsWith("import") && code.contains("class ")) {
            return insertClassCode(sourceFile, project, code)
        }

        ApplicationManager.getApplication().invokeLater {
            val rootElement = runReadAction {
                val psiJavaFile = PsiManager.getInstance(project).findFile(sourceFile) as PsiJavaFile
                val psiClass = psiJavaFile.classes.firstOrNull()
                if (psiClass == null) {
                    log.error("Failed to find PsiClass in the source file: $psiJavaFile, code: $code")
                    return@runReadAction null
                }

                return@runReadAction psiClass
            } ?: return@invokeLater

            val psiElementFactory = PsiElementFactory.getInstance(project)

            val newTestMethod = psiElementFactory.createMethodFromText(code, rootElement)
            if (rootElement.findMethodsByName(newTestMethod.name, false).isNotEmpty()) {
                log.error("Method already exists in the class: ${newTestMethod.name}")
            }

            log.info("newTestMethod: ${newTestMethod.text}")

            WriteCommandAction.runWriteCommandAction(project) {
                val lastMethod = rootElement.methods.lastOrNull()
                val lastMethodEndOffset = lastMethod?.textRange?.endOffset ?: 0

                val document = PsiDocumentManager.getInstance(project).getDocument(rootElement.containingFile)
                // insert new line with indent before the new method
                document?.insertString(lastMethodEndOffset, "\n    ")
                document?.insertString(lastMethodEndOffset, newTestMethod.text)
            }

            project.guessProjectDir()?.refresh(true, true)
        }

        return true
    }

    override fun insertClassCode(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        log.info("start insertClassCode: $code")
        WriteCommandAction.runWriteCommandAction(project) {
            val psiFile = PsiManager.getInstance(project).findFile(sourceFile) as PsiJavaFile
            val document = psiFile.viewProvider.document!!
            document.insertString(document.textLength, code)
        }

        return true
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

    override fun runTest(project: Project, virtualFile: VirtualFile) {
        val runManager = RunManager.getInstance(project)
        val allConfigurationsList = runManager.allConfigurationsList
        log.warn(virtualFile.nameWithoutExtension)
        val testConfig = allConfigurationsList.firstOrNull {
            it.name == virtualFile.nameWithoutExtension && it is GradleRunConfiguration
        }

        if (testConfig == null) {
            log.warn("Failed to find test configuration for: ${virtualFile.nameWithoutExtension}")
            return
        }

        val configurationSettings =
            runManager.findConfigurationByTypeAndName(testConfig.getType(), testConfig.name)

        if (configurationSettings == null) {
            log.warn("Failed to find test configuration for: ${virtualFile.nameWithoutExtension}")
            return
        }

        log.info("configurationSettings: $configurationSettings")
        runManager.selectedConfiguration = configurationSettings

        val executor: Executor = getExecutor()
        ExecutorRegistryImpl.RunnerHelper.run(
            project,
            testConfig,
            configurationSettings,
            DataContext.EMPTY_CONTEXT,
            executor
        )
    }
}

private fun PsiClassReferenceType.resolveFile(): PsiJavaFile? {
    return this.resolve()?.containingFile as PsiJavaFile
}
