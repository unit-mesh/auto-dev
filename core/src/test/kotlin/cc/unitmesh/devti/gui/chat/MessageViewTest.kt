package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.gui.chat.view.MessageView
import com.intellij.temporary.gui.block.SimpleMessage
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MessageViewTest: BasePlatformTestCase() {
    fun testShould_parse_code_from_markdown_java_hello_world() {
        val markdown = """
            |complete code:
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```
        """.trimMargin()

        val message = SimpleMessage(markdown, markdown, ChatRole.User)
        val parts = MessageView.layoutAll(message)

        assert(parts.size == 2)
        assertEquals(parts[0].getTextContent(), "complete code:\n")
        assertEquals(parts[1].getTextContent(), """
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```
        """.trimMargin())
    }

    fun testShould_spilt_three_parts_when_has_two_code_block() {
        val markdown = """
            |complete code:
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```
        """.trimMargin()

        val message = SimpleMessage(markdown, markdown, ChatRole.User)
        val parts = MessageView.layoutAll(message)

        assert(parts.size == 3)
        assertEquals(parts[0].getTextContent(), "complete code:\n")
        assertEquals(parts[1].getTextContent(), """
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```
            |""".trimMargin())
        assertEquals(parts[2].getTextContent(), """
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```""".trimMargin())
    }
}
