package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.TestContextProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

class JavaTestContextProvider : TestContextProvider() {

    override fun prepareTestFile(sourceFile: PsiFile, project: Project): VirtualFile? {
        val sourceFilePath = sourceFile.virtualFile
        val sourceDir = sourceFilePath.parent

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
                createTestFile(sourceFile, testDirCreated)
            } else {
                // Failed to create the test directory, return null
                null
            }
        }

        // Test directory already exists, find the corresponding test file
        val testFilePath = testDirPath + "/" + sourceFile.name.replace(".java", "Test.java")
        val testFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)

        return if (testFile != null) {
            testFile
        } else {
            createTestFile(sourceFile, testDir)
        }
    }

    override fun insertTestMethod(methodName: String, code: String): String {
        TODO("Not yet implemented")
    }

    private fun createTestFile(sourceFile: PsiFile, testDir: VirtualFile): VirtualFile? {
        // Create the test file content based on the source file
        val sourceFileName = sourceFile.name
        val testFileName = sourceFileName.replace(".java", "Test.java")
        val testFileContent = "<AutoDevPlaceHolder>"

        // Create the test file in the test directory
        val testFile = testDir.createChildData(this, testFileName)
        testFile.setBinaryContent(testFileContent.toByteArray())

        // Refresh the test directory to make sure the test file is visible
        testDir.refresh(false, true)

        return testFile
    }
}