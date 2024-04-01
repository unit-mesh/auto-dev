package cc.unitmesh.database.flow;

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
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

        // verify sqlFile syntax correct
        // Verify
        val errors = mutableListOf<String>()
        val visitor = object : SqlSyntaxCheckingVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is PsiErrorElement) {
                    errors.add("Syntax error at position ${element.textRange.startOffset}: ${element.errorDescription}")
                }
                super.visitElement(element)
            }
        }
        sqlFile.accept(visitor)

        // err msg: SQL syntax contains errors: Syntax error at position 30: <expression>, ALL, ANY or SOME expected, got ';'
        assertTrue(errors.isNotEmpty())
        assertEquals("Syntax error at position 30: <expression>, ALL, ANY or SOME expected, got ';'", errors[0])
    }

    abstract class SqlSyntaxCheckingVisitor : com.intellij.psi.PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {
            element.children.forEach { it.accept(this) }
        }
    }
}