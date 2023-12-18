package cc.unitmesh.idea.service

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.provider.context.TestFileContext
import cc.unitmesh.devti.provider.WriteTestService
import com.intellij.execution.configurations.RunProfile
import com.intellij.lang.Language
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import java.io.File

class JavaWriteTestService : WriteTestService() {
    override fun runConfigurationClass(project: Project): Class<out RunProfile> {
        return GradleRunConfiguration::class.java
    }

    override fun isApplicable(element: PsiElement): Boolean {
        return element.language == Language.findLanguageByID("JAVA")
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        val sourceFilePath = sourceFile.virtualFile
        val parentDir = sourceFilePath.parent
        val className = sourceFile.name.replace(".java", "") + "Test"

        val packageName = ReadAction.compute<String, Throwable> {
            (sourceFile as PsiJavaFile).packageName
        }
        val parentDirPath = ReadAction.compute<String, Throwable> {
            parentDir?.path
        }

        val relatedModels = lookupRelevantClass(project, element)

        // Check if the source file is in the src/main/java directory
        if (!parentDirPath?.contains("/src/main/java/")!!) {
            log.error("Source file is not in the src/main/java directory: ${parentDirPath}")
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

        // Test directory already exists, find the corresponding test file
        val testFilePath = testDirPath + "/" + sourceFile.name.replace(".java", "Test.java")
        val testFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)

        project.guessProjectDir()?.refresh(true, true)

        return if (testFile != null) {
            TestFileContext(isNewFile, testFile, relatedModels, className, sourceFile.language, null)
        } else {
            val targetFile = createTestFile(sourceFile, testDir!!, packageName, project)
            TestFileContext(isNewFile = true, targetFile, relatedModels, "", sourceFile.language, null)
        }
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        return ReadAction.compute<List<ClassContext>, Throwable> {
            val elements = mutableListOf<ClassContext>()
            val projectPath = project.guessProjectDir()?.path

            val resolvedClasses: MutableMap<String, PsiClass> = mutableMapOf()
            resolvedClasses.putAll(JavaTypeUtil.resolveByField(element))

            when (element) {
                is PsiClass -> {
                    element.methods.forEach { method ->
                        resolvedClasses.putAll(JavaTypeUtil.resolveByMethod(method))
                    }
                }

                is PsiMethod -> {
                    resolvedClasses.putAll(JavaTypeUtil.resolveByMethod(element))
                }
            }

            // find the class in the same project
            resolvedClasses.forEach { (_, psiClass) ->
                val classPath = psiClass.containingFile?.virtualFile?.path
                if (classPath?.contains(projectPath!!) == true) {
                    elements += ClassContextProvider(false).from(psiClass)
                }
            }

            elements
        }
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

        return WriteCommandAction.runWriteCommandAction<VirtualFile>(project) {
            val testFile = testDir.createChildData(this, testFileName)

            val document = FileDocumentManager.getInstance().getDocument(testFile)
            document?.setText(testFileContent)

            testFile
        }
    }

    companion object {
        val log = logger<JavaWriteTestService>()
    }
}
