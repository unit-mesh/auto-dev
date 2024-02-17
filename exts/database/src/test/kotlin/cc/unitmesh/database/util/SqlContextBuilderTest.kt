package cc.unitmesh.database.util;

import com.intellij.psi.PsiFileFactory
import com.intellij.sql.SqlFileType
import com.intellij.sql.dialects.base.SqlElementFactory
import com.intellij.sql.psi.SqlPsiFacade
import com.intellij.testFramework.LightPlatformTestCase

class SqlContextBuilderTest: LightPlatformTestCase() {

    fun testShouldReturnCurrentNamespace() {
        val factory = PsiFileFactory.getInstance(project)
        val demoSqlCode = """
            CREATE SCHEMA demo;
            CREATE TABLE demo.users (
                id INT PRIMARY KEY,
                name VARCHAR(255) NOT NULL
            );
            """.trimIndent()

        val psiFile = factory.createFileFromText("demo.sql", SqlFileType.INSTANCE, demoSqlCode)
        val sqlFile = psiFile as com.intellij.sql.psi.SqlFile
        val currentNamespace = SqlContextBuilder.getCurrentNamespace(sqlFile)

        assertEquals(currentNamespace, null)
    }
}

