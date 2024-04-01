package cc.unitmesh.database.flow;

import com.intellij.psi.PsiFileFactory
import com.intellij.sql.psi.SqlLanguage
import com.intellij.testFramework.LightPlatformTestCase

class AutoSqlBackgroundTaskTest: LightPlatformTestCase() {

    fun testShouldParseCode() {
        // Given
        val code = """
            SELECT * FROM table where;
        """.trimIndent()
//
//        val inspectionManager = InspectionManager.getInstance(project)
//        inspectionManager.

        // When
        val psiFile =
            PsiFileFactory.getInstance(project).createFileFromText("temp.sql", SqlLanguage.INSTANCE, code)

        // Then

    }
}
