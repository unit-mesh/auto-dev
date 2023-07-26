package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.TestContextProvider
import cc.unitmesh.devti.provider.TestFileContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile

class JavaTestContextProvider : TestContextProvider() {

    override fun prepareTestFile(sourceFile: PsiFile, project: Project): TestFileContext? {
        val sourceFilePath = sourceFile.virtualFile
        val sourceDir = sourceFilePath.parent

        val packageName = (sourceFile as PsiJavaFile).packageName

        // Check if the source file is in the src/main/java directory
        if (!sourceDir?.path?.contains("/src/main/java/")!!) {
            // Not in the src/main/java directory, return null (cannot find test directory)
            return null
        }

        // Find the test directory
        val testDirPath = sourceDir.path.replace("/src/main/java/", "/src/test/java/")
        val testDir = LocalFileSystem.getInstance().findFileByPath(testDirPath)

        // Check if the test directory exists, if not, create it
        if (testDir == null || !testDir.isDirectory) {
            val testDirCreated = LocalFileSystem.getInstance().refreshAndFindFileByPath(testDirPath)
            return if (testDirCreated != null) {
                // Successfully created the test directory
                val targetFile = createTestFile(sourceFile, testDirCreated, packageName)
                TestFileContext(true, targetFile)
            } else {
                // Failed to create the test directory, return null
                null
            }
        }

        // Test directory already exists, find the corresponding test file
        val testFilePath = testDirPath + "/" + sourceFile.name.replace(".java", "Test.java")
        val testFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)

        return if (testFile != null) {
            TestFileContext(false, testFile)
        } else {
            val targetFile = createTestFile(sourceFile, testDir, packageName)
            TestFileContext(true, targetFile)
        }
    }

    override fun insertTestMethod(methodName: String, code: String): String {
        TODO("Not yet implemented")
    }

    private fun createTestFile(sourceFile: PsiFile, testDir: VirtualFile, packageName: String): VirtualFile {
        // Create the test file content based on the source file
        val sourceFileName = sourceFile.name
        val testFileName = sourceFileName.replace(".java", "Test.java")
        val testFileContent = """package $packageName;
            |$AUTO_DEV_PLACEHOLDER""".trimMargin()

        // Create the test file in the test directory
        val testFile = testDir.createChildData(this, testFileName)
        testFile.setBinaryContent(testFileContent.toByteArray())

        // Refresh the test directory to make sure the test file is visible
        testDir.refresh(false, true)

        return testFile
    }
}