package cc.unitmesh.xuiper.render

import cc.unitmesh.xuiper.dsl.NanoDSL
import cc.unitmesh.xuiper.ir.NanoIR
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class HtmlRendererTest {

    private val renderer = HtmlRenderer()

    @Test
    fun `should render simple text component`() {
        val ir = NanoIR.text("Hello World", "h2")

        val html = renderer.renderNode(ir)

        assertContains(html, "<h2")
        assertContains(html, "Hello World")
        assertContains(html, "style-h2")
    }

    @Test
    fun `should render VStack with children`() {
        val ir = NanoIR.vstack(
            spacing = "md",
            children = listOf(
                NanoIR.text("First"),
                NanoIR.text("Second")
            )
        )

        val html = renderer.renderNode(ir)

        assertContains(html, "nano-vstack")
        assertContains(html, "spacing-md")
        assertContains(html, "First")
        assertContains(html, "Second")
    }

    @Test
    fun `should render HStack with alignment`() {
        val ir = NanoIR.hstack(
            spacing = "sm",
            align = "center",
            justify = "between",
            children = listOf(
                NanoIR.text("Left"),
                NanoIR.text("Right")
            )
        )

        val html = renderer.renderNode(ir)

        assertContains(html, "nano-hstack")
        assertContains(html, "align-center")
        assertContains(html, "justify-between")
    }

    @Test
    fun `should render Card with padding and shadow`() {
        val ir = NanoIR.card(
            padding = "lg",
            shadow = "md",
            children = listOf(NanoIR.text("Card Content"))
        )

        val html = renderer.renderNode(ir)

        assertContains(html, "nano-card")
        assertContains(html, "padding-lg")
        assertContains(html, "shadow-md")
        assertContains(html, "Card Content")
    }

    @Test
    fun `should render Button with intent`() {
        val ir = NanoIR.button("Click me", "primary")

        val html = renderer.renderNode(ir)

        assertContains(html, "<button")
        assertContains(html, "nano-button")
        assertContains(html, "intent-primary")
        assertContains(html, "Click me")
    }

    @Test
    fun `should render full HTML document`() {
        val ir = NanoIR.card(
            padding = "md",
            children = listOf(NanoIR.text("Hello"))
        )
        
        val html = renderer.render(ir)
        
        assertContains(html, "<!DOCTYPE html>")
        assertContains(html, "<html>")
        assertContains(html, "<style>")
        assertContains(html, "</html>")
    }

    @Test
    fun `should render from NanoDSL source`() {
        val source = """
component GreetingCard:
    Card:
        VStack(spacing="sm"):
            Text("Hello!", style="h2")
            Text("Welcome to NanoDSL")
        """.trimIndent()

        val ir = NanoDSL.toIR(source)
        val html = renderer.renderNode(ir)

        assertContains(html, "nano-card")
        assertContains(html, "nano-vstack")
        assertContains(html, "Hello!")
        assertContains(html, "Welcome to NanoDSL")
    }

    @Test
    fun `should render divider`() {
        val ir = NanoIR(type = "Divider")

        val html = renderer.renderNode(ir)

        assertContains(html, "<hr")
        assertContains(html, "nano-divider")
    }

    @Test
    fun `should render all basic component types`() {
        val types = listOf("VStack", "HStack", "Card", "Text", "Button", "Image", "Badge", "Divider")

        types.forEach { type ->
            val ir = NanoIR(type = type)
            val html = renderer.renderNode(ir)
            assertTrue(html.isNotEmpty(), "Should render $type")
        }
    }
}

