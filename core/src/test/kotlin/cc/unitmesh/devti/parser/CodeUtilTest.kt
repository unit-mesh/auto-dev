package cc.unitmesh.devti.parser

import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class CodeUtilTest  : BasePlatformTestCase() {
    fun testShould_parse_code_from_markdown_java_hello_world() {
        val markdown = """
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```
        """.trimMargin()

        val code = CodeFence.parse(markdown)

        assertEquals(
            code.text, """
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
        """.trimMargin()
        )
        assertTrue(code.isComplete)
    }

    fun testShould_handle_code_not_complete_from_markdown() {
        val markdown = """
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
        """.trimMargin()

        val code = CodeFence.parse(markdown)
        assertEquals(
            code.text, """
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
        """.trimMargin()
        )
        assertTrue(!code.isComplete)
    }

    fun testShould_handle_pure_markdown_content() {
        val content = "```markdown\nGET /wp/v2/posts\n```"
        val code = CodeFence.parse(content)
        assertEquals(code.text, "GET /wp/v2/posts")
    }

    fun testShould_handle_http_request() {
        val content = "```http request\nGET /wp/v2/posts\n```"
        val code = CodeFence.parse(content)
        assertEquals(code.text, "GET /wp/v2/posts")
    }
}
