package cc.unitmesh.idea.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.provider.TestContextProvider
import cc.unitmesh.devti.provider.TestFileContext
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
import java.io.File
import kotlin.jvm.internal.Ref

class JavaTestContextProvider : TestContextProvider() {
    companion object {
        val log = logger<JavaTestContextProvider>()
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        val sourceFilePath = sourceFile.virtualFile
        val parentDir = sourceFilePath.parent

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
            result.element = TestFileContext(isNewFile, testFile, relatedModels)
        } else {
            val targetFile = createTestFile(sourceFile, testDir!!, packageName, project)
            result.element = TestFileContext(isNewFile = true, targetFile, relatedModels)
        }

        return result.element
    }

    /**
     * TODO: return ClassContext with package name with [cc.unitmesh.devti.context.FileContext]
     */
    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        val result: Ref.ObjectRef<List<ClassContext>> = Ref.ObjectRef()
        result.element = emptyList()

        ApplicationManager.getApplication().runReadAction {
            val elements = mutableListOf<ClassContext>()
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
                    elements += (ClassContextProvider(false).from(psiClass))
                }
            }

            result.element = elements
        }

        return result.element
    }

    // TODO: handle generic type
    private fun resolveByMethod(element: PsiElement): MutableMap<String, PsiClass?> {
        val resolvedClasses = mutableMapOf<String, PsiClass?>()
        if (element is PsiMethod) {
            element.parameterList.parameters.filter {
                it.type is PsiClassReferenceType
            }.map {
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

    override fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        log.info("methodCode: $code")
        if (!code.contains("@Test")) {
            log.error("methodCode does not contain @Test annotation: $code")
            return false
        }

        // if code is a class code, we need to insert
        if (code.contains("public class ")) {
            // todo: if the class is new, we need to sync PsiClass, if not, we cannot insert the code in next
            return insertClassCode(sourceFile, project, code)
        }

        ApplicationManager.getApplication().invokeLater {
            val rootElement = runReadAction {
                val psiJavaFile = PsiManager.getInstance(project).findFile(sourceFile) as PsiJavaFile
                val psiClass = psiJavaFile.classes.firstOrNull()
                if (psiClass == null) {
                    log.error("Failed to find PsiClass in the source file: $psiJavaFile")
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

    private fun createTestFile(sourceFile: PsiFile, testDir: VirtualFile, packageName: String, project: Project): VirtualFile {
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