package cc.unitmesh.devti.parser

import cc.unitmesh.devti.util.parser.Code
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class CodeUtilTest {
    @Test
    fun should_parse_code_from_markdown_java_hello_world() {
        val markdown = """
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```
        """.trimMargin()

        val code = Code.parse(markdown)

//        assertEquals(code.language.id, "java")
        assertEquals(code.text, """
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
        """.trimMargin())
        assertTrue(code.isComplete)
    }

    @Test
    fun should_handle_code_not_complete_from_markdown() {
        val markdown = """
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
        """.trimMargin()

        val code = Code.parse(markdown)
        assertEquals(code.text, """
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
        """.trimMargin())
        assertTrue(!code.isComplete)
    }
}