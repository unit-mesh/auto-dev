package cc.unitmesh.idea.service

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import java.io.File

class JavaAutoTestService : AutoTestService() {
    override fun runConfigurationClass(project: Project): Class<out RunProfile> = GradleRunConfiguration::class.java
    override fun isApplicable(element: PsiElement): Boolean = element.language is JavaLanguage

    override fun createConfiguration(project: Project, path: String): RunConfiguration? {
        val file = LocalFileSystem.getInstance().refreshAndFindFileByPath(path)
        return file?.let { createConfiguration(project, it) }
    }

    override fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? {
        val name = virtualFile.name

        val psiFile: PsiJavaFile = PsiManager.getInstance(project).findFile(virtualFile) as? PsiJavaFile ?: return null
        val canonicalName = psiFile.packageName + "." + virtualFile.nameWithoutExtension

        val runManager = RunManager.getInstance(project)

        var moduleName = ""
        val moduleForFile = ProjectFileIndex.getInstance(project).getModuleForFile(virtualFile)
        // a moduleForFile.name will be like <project>.<module>.<testModule>, so we need to remove the last part and first part
        if (moduleForFile != null) {
            val moduleNameSplit = moduleForFile.name.split(".").drop(1).dropLast(1).joinToString(":")
            if (moduleNameSplit.isNotEmpty()) {
                moduleName = "$moduleNameSplit:"
            }
        }

        // todo: add maven ??
        val configuration = runManager.createConfiguration(name, GradleExternalTaskConfigurationType::class.java)
        val runConfiguration = configuration.configuration as GradleRunConfiguration

        runConfiguration.isDebugServerProcess = false
        runConfiguration.settings.externalProjectPath = project.guessProjectDir()?.path
        // todo: add module for test
        runConfiguration.rawCommandLine = moduleName + "test --tests \"${canonicalName}\""

        runManager.addConfiguration(configuration)
        runManager.selectedConfiguration = configuration

        return runConfiguration
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        val sourceFilePath = sourceFile.virtualFile
        val parentDir = sourceFilePath.parent
        val testFileName = sourceFile.name.replace(".java", "") + "Test"

        val packageName = ReadAction.compute<String, Throwable> {
            (sourceFile as PsiJavaFile).packageName
        }
        val parentDirPath = ReadAction.compute<String, Throwable> {
            parentDir?.path
        }

        val relatedModels = lookupRelevantClass(project, element).distinctBy { it.name }

        // Check if the source file is in the src/main/java directory
        if (!parentDirPath?.contains("/src/main/java/")!!) {
            log.error("Source file is not in the src/main/java directory: $parentDirPath")
            return null
        }

        var isNewFile = false

        // Find the test directory
        val testDirPath = parentDirPath.replace("/src/main/java/", "/src/test/java/")
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

        val imports = runReadAction {
            val importList = PsiTreeUtil.getChildrenOfTypeAsList(sourceFile, PsiImportList::class.java)
            importList.flatMap { it.allImportStatements.map { import -> import.text } }
        }

        return if (testFile != null) {
            TestFileContext(isNewFile, testFile, relatedModels, testFileName, sourceFile.language, null, imports)
        } else {
            val targetFile = createTestFile(sourceFile, testDir!!, packageName, project)
            TestFileContext(isNewFile = true, targetFile, relatedModels, "", sourceFile.language, null, imports)
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
        val log = logger<JavaAutoTestService>()
    }
}
