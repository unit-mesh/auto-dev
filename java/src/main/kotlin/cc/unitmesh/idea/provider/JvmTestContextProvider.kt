package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.TestContextProvider
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile

class JvmTestContextProvider : TestContextProvider() {
    override fun prepareTestFile(sourceFile: PsiFile): VirtualFile? {
        val psiJavaFile = sourceFile as PsiJavaFile
        val packageName = psiJavaFile.packageName
        val className = psiJavaFile.name
        val testClassName = className.replace(".java", "Test.java")
        val testPackageName = packageName.replace("main", "test")
        val testPath = psiJavaFile.virtualFile.parent.path.replace("main", "test")

        // check if test file exists , if not, create test file, if exists, return true
        val projectDir = psiJavaFile.project.guessProjectDir()
        val testFile = projectDir?.findFileByRelativePath(testPath)
        if (testFile != null) {
            return testFile
        }

        if (projectDir != null) {

        }

        return null
    }

    override fun insertTestMethod(methodName: String, code: String): String {
        TODO("Not yet implemented")
    }

}
