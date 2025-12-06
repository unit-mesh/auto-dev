package cc.unitmesh.xuiper.dsl

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NanoDSLParserTest {

    @Test
    fun `should parse simple component`() {
        val source = """
component GreetingCard:
    Card:
        Text("Hello!", style="h2")
        """.trimIndent()

        val result = NanoDSL.parse(source)

        assertEquals("GreetingCard", result.name)
        assertEquals(1, result.children.size)
        assertTrue(result.children[0] is NanoNode.Card)
    }

    @Test
    fun `should parse VStack with spacing`() {
        val source = """
component TestComponent:
    VStack(spacing="md"):
        Text("First")
        Text("Second")
        """.trimIndent()

        val result = NanoDSL.parse(source)

        assertEquals("TestComponent", result.name)
        val vstack = result.children[0] as NanoNode.VStack
        assertEquals("md", vstack.spacing)
        assertEquals(2, vstack.children.size)
    }

    @Test
    fun `should parse HStack with alignment`() {
        val source = """
component TestComponent:
    HStack(align="center", justify="between"):
        Text("Left")
        Text("Right")
        """.trimIndent()

        val result = NanoDSL.parse(source)

        val hstack = result.children[0] as NanoNode.HStack
        assertEquals("center", hstack.align)
        assertEquals("between", hstack.justify)
    }

    @Test
    fun `should parse Card with nested content`() {
        val source = """
component ProductCard:
    Card:
        padding: "md"
        VStack:
            Text("Product Name", style="h3")
            Text("$99.99")
        """.trimIndent()

        val result = NanoDSL.parse(source)

        val card = result.children[0] as NanoNode.Card
        assertEquals("md", card.padding)
        assertEquals(1, card.children.size)
        assertTrue(card.children[0] is NanoNode.VStack)
    }

    @Test
    fun `should parse Button with properties`() {
        val source = """
component TestComponent:
    Button("Click me", intent="primary")
        """.trimIndent()

        val result = NanoDSL.parse(source)

        val button = result.children[0] as NanoNode.Button
        assertEquals("Click me", button.label)
        assertEquals("primary", button.intent)
    }

    @Test
    fun `should parse conditional rendering`() {
        val source = """
component TestComponent:
    if item.is_new:
        Badge("New", color="green")
        """.trimIndent()

        val result = NanoDSL.parse(source)

        val conditional = result.children[0] as NanoNode.Conditional
        assertEquals("item.is_new", conditional.condition)
        assertEquals(1, conditional.thenBranch.size)
        assertTrue(conditional.thenBranch[0] is NanoNode.Badge)
    }

    @Test
    fun `should parse for loop`() {
        val source = """
component TaskList:
    for task in state.tasks:
        Text("Task item")
        """.trimIndent()

        val result = NanoDSL.parse(source)

        val forLoop = result.children[0] as NanoNode.ForLoop
        assertEquals("task", forLoop.variable)
        assertEquals("state.tasks", forLoop.iterable)
        assertEquals(1, forLoop.body.size)
    }

    @Test
    fun `should convert to JSON IR`() {
        val source = """
component GreetingCard:
    Card:
        Text("Hello!", style="h2")
        """.trimIndent()

        val ir = NanoDSL.toIR(source)

        assertEquals("Component", ir.type)
        assertNotNull(ir.children)
        assertEquals(1, ir.children!!.size)
        assertEquals("Card", ir.children!![0].type)
    }

    @Test
    fun `should convert to JSON string`() {
        val source = """
component SimpleCard:
    Text("Hello")
        """.trimIndent()

        val json = NanoDSL.toJson(source)

        assertTrue(json.contains("\"type\": \"Component\""))
        assertTrue(json.contains("\"type\": \"Text\""))
    }
}

