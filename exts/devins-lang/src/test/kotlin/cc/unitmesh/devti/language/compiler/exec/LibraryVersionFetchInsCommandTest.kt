package cc.unitmesh.devti.language.compiler.exec

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking

class LibraryVersionFetchInsCommandTest : BasePlatformTestCase() {
    fun testAutoDetection() = runBlocking {
        val project = myFixture.project

        val command = LibraryVersionFetchInsCommand(
            project, "npm", codeContent = """{
  "name": "react",
  "type": "npm"
}"""
        )
        val result = command.execute()
        assertNotNull(result)
        // Should either find versions or show no versions found
        assertTrue(result!!.contains("."))
    }
}