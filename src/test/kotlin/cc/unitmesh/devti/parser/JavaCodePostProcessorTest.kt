package cc.unitmesh.devti.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class JavaCodePostProcessorTest {
    @Test
    fun should_handle_with_no_indent_code() {
        val prefix = "public class Test {\n"
        val suffix = "\n}"

        val complete = """
public void test() {
    System.out.println("hello world");
}
        """.trimIndent()

        val postProcessor = JavaCodePostProcessor(prefix, suffix, complete)
        val result = postProcessor.execute()

        assertEquals(
            result, """    public void test() {
        System.out.println("hello world");
    }"""
        )
    }

    @Test
    fun should_remove_right_parenthesis() {
        val prefix = "public class Test {\n"
        val suffix = "}\n}"

        val complete = """public void test() {
}
}"""
        val postProcessor = JavaCodePostProcessor(prefix, suffix, complete)
        val result = postProcessor.execute()

        assertEquals(result, """    public void test() {
            |
        """.trimMargin())
    }
}