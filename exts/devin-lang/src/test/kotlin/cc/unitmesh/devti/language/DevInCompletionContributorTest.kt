package cc.unitmesh.devti.language

import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiFile
import com.intellij.testFramework.VfsTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class DevInCompletionContributorTest: LightJavaCodeInsightFixtureTestCase() {
    fun testCommandCompletion() {
        myFixture.loadNewFile("CodeComplete.devin", "")
        editor.caretModel.moveToOffset(1)
        myFixture.type("/")
        myFixture.completeBasic()

        val lookupElements: List<String> = myFixture.lookupElements?.map { it.lookupString } ?: emptyList()

        assertTrue(lookupElements.contains("rev"))
        assertTrue(lookupElements.contains("write"))
        assertTrue(lookupElements.contains("file"))
    }
}

fun CodeInsightTestFixture.loadNewFile(path: String, contents: String): PsiFile {
    val virtualFile = VfsTestUtil.createFile(project.guessProjectDir()!!, path, contents)
    configureFromExistingVirtualFile(virtualFile)
    return file
}

