package cc.unitmesh.xuiper.dsl

import cc.unitmesh.xuiper.action.HttpMethod
import cc.unitmesh.xuiper.action.NanoAction
import cc.unitmesh.xuiper.ast.NanoNode
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

    @Test
    fun `should parse Button with Fetch action`() {
        val source = """
component LoginForm:
    Button("Submit", intent="primary"):
        on_click: Fetch(url="/api/login", method="POST")
        """.trimIndent()

        val result = NanoDSL.parse(source)

        val button = result.children[0] as NanoNode.Button
        assertEquals("Submit", button.label)
        assertNotNull(button.onClick)
        assertTrue(button.onClick is NanoAction.Fetch)

        val fetch = button.onClick as NanoAction.Fetch
        assertEquals("/api/login", fetch.url)
        assertEquals(HttpMethod.POST, fetch.method)
    }

    @Test
    fun `should parse Fetch with body parameters`() {
        val source = """
component LoginForm:
    Button("Submit"):
        on_click: Fetch(url="/api/login", method="POST", body={"email": state.email, "password": state.password})
        """.trimIndent()

        val result = NanoDSL.parse(source)

        val button = result.children[0] as NanoNode.Button
        val fetch = button.onClick as NanoAction.Fetch

        assertEquals("/api/login", fetch.url)
        assertEquals(HttpMethod.POST, fetch.method)
        assertNotNull(fetch.body)
        assertEquals(2, fetch.body!!.size)
        assertTrue(fetch.body!!.containsKey("email"))
        assertTrue(fetch.body!!.containsKey("password"))
    }

    @Test
    fun `should parse Fetch with headers`() {
        val source = """
component ApiForm:
    Button("Call API"):
        on_click: Fetch(url="/api/data", method="GET", headers={"Authorization": "Bearer token123"})
        """.trimIndent()

        val result = NanoDSL.parse(source)

        val button = result.children[0] as NanoNode.Button
        val fetch = button.onClick as NanoAction.Fetch

        assertEquals("/api/data", fetch.url)
        assertNotNull(fetch.headers)
        assertEquals("Bearer token123", fetch.headers!!["Authorization"])
    }

    @Test
    fun `should parse TextArea component`() {
        val source = """
component MessageForm:
    TextArea(value := state.message, placeholder="Enter message", rows=4)
        """.trimIndent()

        val result = NanoDSL.parse(source)

        val textArea = result.children[0] as NanoNode.TextArea
        assertNotNull(textArea.value)
        assertEquals("Enter message", textArea.placeholder)
        assertEquals(4, textArea.rows)
    }

    @Test
    fun `should parse Select component`() {
        val source = """
component CountryForm:
    Select(value := state.country, options=countries, placeholder="Select country")
        """.trimIndent()

        val result = NanoDSL.parse(source)

        val select = result.children[0] as NanoNode.Select
        assertNotNull(select.value)
        assertEquals("countries", select.options)
        assertEquals("Select country", select.placeholder)
    }

    @Test
    fun `should parse Form component with children`() {
        val source = """
component ContactForm:
    Form(onSubmit=request.submit):
        Input(value := state.email, placeholder="Email")
        Button("Submit", intent="primary")
        """.trimIndent()

        val result = NanoDSL.parse(source)

        val form = result.children[0] as NanoNode.Form
        assertEquals("request.submit", form.onSubmit)
        assertEquals(2, form.children.size)
        assertTrue(form.children[0] is NanoNode.Input)
        assertTrue(form.children[1] is NanoNode.Button)
    }

    @Test
    fun `should parse multi-line on_click with state mutation and Fetch`() {
        val source = """
component LoginForm:
    Button("Submit"):
        on_click:
            state.loading = True
            Fetch(url="/api/login", method="POST")
        """.trimIndent()

        val result = NanoDSL.parse(source)

        val button = result.children[0] as NanoNode.Button
        assertNotNull(button.onClick)

        // Should be a Sequence with 2 actions
        assertTrue(button.onClick is NanoAction.Sequence)
        val sequence = button.onClick as NanoAction.Sequence
        assertEquals(2, sequence.actions.size)

        // First action is state mutation
        assertTrue(sequence.actions[0] is NanoAction.StateMutation)
        val mutation = sequence.actions[0] as NanoAction.StateMutation
        assertEquals("loading", mutation.path)

        // Second action is Fetch
        assertTrue(sequence.actions[1] is NanoAction.Fetch)
        val fetch = sequence.actions[1] as NanoAction.Fetch
        assertEquals("/api/login", fetch.url)
        assertEquals(HttpMethod.POST, fetch.method)
    }

    @Test
    fun `should parse multi-line Fetch with callbacks`() {
        val source = """
component LoginForm:
    Button("Submit"):
        on_click:
            Fetch(
                url="/api/login",
                method="POST",
                body={"email": state.email, "password": state.password},
                on_success: Navigate(to="/dashboard"),
                on_error: ShowToast("Login failed")
            )
        """.trimIndent()

        val result = NanoDSL.parse(source)

        val button = result.children[0] as NanoNode.Button
        assertNotNull(button.onClick)
        assertTrue(button.onClick is NanoAction.Fetch)

        val fetch = button.onClick as NanoAction.Fetch
        assertEquals("/api/login", fetch.url)
        assertEquals(HttpMethod.POST, fetch.method)

        // Check body
        assertNotNull(fetch.body)
        assertEquals(2, fetch.body!!.size)

        // Check callbacks
        assertNotNull(fetch.onSuccess)
        assertTrue(fetch.onSuccess is NanoAction.Navigate)
        assertEquals("/dashboard", (fetch.onSuccess as NanoAction.Navigate).to)

        assertNotNull(fetch.onError)
        assertTrue(fetch.onError is NanoAction.ShowToast)
        assertEquals("Login failed", (fetch.onError as NanoAction.ShowToast).message)
    }

    @Test
    fun `should parse AI generated contact form with HTTP request`() {
        val source = """
component ContactForm:
    state:
        name: str = ""
        email: str = ""
        message: str = ""
        is_submitting: bool = False

    Card:
        VStack(spacing="md"):
            Text("Contact Us", style="h2")
            Input(value:=state.name, placeholder="Enter your name")
            Input(value:=state.email, placeholder="Enter your email")
            Button("Send Message", intent="primary"):
                on_click:
                    state.is_submitting = True
                    Fetch(
                        url="/api/contact",
                        method="POST",
                        body={"name": state.name, "email": state.email, "message": state.message},
                        headers={"Content-Type": "application/json"},
                        on_success: ShowToast("Message sent!"),
                        on_error: ShowToast("Failed to send")
                    )
        """.trimIndent()

        val result = NanoDSL.parse(source)

        // Should parse successfully
        assertEquals("ContactForm", result.name)

        // Check state
        assertNotNull(result.state)
        assertTrue(result.state!!.variables.any { it.name == "name" })
        assertTrue(result.state!!.variables.any { it.name == "is_submitting" })

        // Find the Card
        val card = result.children[0] as NanoNode.Card
        val vstack = card.children[0] as NanoNode.VStack

        // Find the button
        val button = vstack.children.filterIsInstance<NanoNode.Button>().firstOrNull()
        assertNotNull(button)
        assertEquals("Send Message", button!!.label)

        // Check onClick action (should be Sequence with state mutation + Fetch)
        assertNotNull(button.onClick)
        assertTrue(button.onClick is NanoAction.Sequence)

        val sequence = button.onClick as NanoAction.Sequence
        assertTrue(sequence.actions.size >= 2)

        // Find Fetch action
        val fetchAction = sequence.actions.filterIsInstance<NanoAction.Fetch>().firstOrNull()
        assertNotNull(fetchAction)
        assertEquals("/api/contact", fetchAction!!.url)
        assertEquals(HttpMethod.POST, fetchAction.method)
    }
}
