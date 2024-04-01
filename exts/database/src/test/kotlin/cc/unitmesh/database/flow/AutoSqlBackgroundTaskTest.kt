package cc.unitmesh.database.flow;

import com.intellij.psi.PsiFileFactory
import com.intellij.sql.psi.SqlFile
import com.intellij.sql.psi.SqlLanguage
import com.intellij.testFramework.LightPlatformTestCase

class AutoSqlBackgroundTaskTest: LightPlatformTestCase() {

    fun testShouldParseCode() {
        // Given
        val code = """
            SELECT * FROM table where id =;
        """.trimIndent()

        // When
        val sqlFile: SqlFile =
            PsiFileFactory.getInstance(project).createFileFromText("temp.sql", SqlLanguage.INSTANCE, code)
                    as SqlFile

        val errors = sqlFile.verifySqlElement()

        // Then
        assertTrue(errors.isNotEmpty())
        assertEquals("Syntax error at position 30: <expression>, ALL, ANY or SOME expected, got ';'", errors[0])
    }
}