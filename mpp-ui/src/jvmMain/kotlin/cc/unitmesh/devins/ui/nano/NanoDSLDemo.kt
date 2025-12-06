package cc.unitmesh.devins.ui.nano

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.parser.CodeFence
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.xuiper.dsl.NanoDSL
import cc.unitmesh.xuiper.ir.NanoIR
import cc.unitmesh.xuiper.prompt.PromptTemplateRegistry
import kotlinx.coroutines.launch

/**
 * NanoDSL Demo Component
 *
 * Interactive demo that allows:
 * 1. Pasting NanoDSL code manually
 * 2. Generating UI from natural language via LLM
 * 3. Real-time preview of rendered components
 */
@Composable
fun NanoDSLDemo(
    onGenerateWithLLM: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var dslSource by remember { mutableStateOf(SAMPLE_DSL) }
    var parsedIR by remember { mutableStateOf<NanoIR?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var promptText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var llmService by remember { mutableStateOf<KoogLLMService?>(null) }
    var modelConfig by remember { mutableStateOf<ModelConfig?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Load LLM config on startup
    LaunchedEffect(Unit) {
        try {
            val wrapper = ConfigManager.load()
            val config = wrapper.getActiveModelConfig()
            if (config != null && config.isValid()) {
                modelConfig = config
                llmService = KoogLLMService.create(config)
                println("✅ NanoDSL Demo: Loaded LLM config - ${config.provider.displayName}/${config.modelName}")
            } else {
                println("⚠️ NanoDSL Demo: No valid LLM config found")
            }
        } catch (e: Exception) {
            println("❌ NanoDSL Demo: Failed to load config - ${e.message}")
        }
    }

    // Parse DSL when source changes
    LaunchedEffect(dslSource) {
        try {
            val ir = NanoDSL.toIR(dslSource)
            parsedIR = ir
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.message
            parsedIR = null
        }
    }

    // Function to generate DSL with LLM
    fun generateWithLLM(prompt: String) {
        val service = llmService ?: run {
            errorMessage = "LLM not configured. Please setup ~/.autodev/config.yaml"
            return
        }

        isGenerating = true
        errorMessage = null

        coroutineScope.launch {
            try {
                val template = PromptTemplateRegistry.get("default")
                val rendered = template.render(mapOf("user_prompt" to prompt))
                val fullPrompt = "${rendered.systemPrompt}\n\nUser Request: ${rendered.userPrompt}"

                val responseBuilder = StringBuilder()
                service.streamPrompt(
                    userPrompt = fullPrompt,
                    compileDevIns = false
                ).collect { chunk ->
                    responseBuilder.append(chunk)
                }

                val llmResponse = responseBuilder.toString()

                // Extract code from markdown code fence
                val codeFence = CodeFence.parse(llmResponse)
                val extractedCode = if (codeFence.text.isNotBlank()) {
                    codeFence.text
                } else {
                    // If no code fence, use the raw response
                    llmResponse.trim()
                }

                dslSource = extractedCode
                isGenerating = false
            } catch (e: Exception) {
                errorMessage = "LLM Error: ${e.message}"
                isGenerating = false
            }
        }
    }

    Row(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Left Panel - Editor
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 8.dp)
        ) {
            // Prompt input for LLM generation
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                label = { Text("Describe the UI you want") },
                placeholder = { Text("e.g., A contact form with name, email, message and submit button with HTTP POST") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating,
                trailingIcon = {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (promptText.isNotBlank()) {
                                    onGenerateWithLLM?.invoke(promptText) ?: generateWithLLM(promptText)
                                }
                            },
                            enabled = llmService != null && promptText.isNotBlank()
                        ) {
                            Icon(Icons.Default.PlayArrow, "Generate")
                        }
                    }
                }
            )

            // LLM status indicator
            modelConfig?.let { config ->
                Text(
                    text = "🤖 ${config.provider.displayName} / ${config.modelName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } ?: Text(
                text = "⚠️ Configure LLM in ~/.autodev/config.yaml",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(8.dp))

            // DSL Editor
            Text(
                text = "NanoDSL Source",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = dslSource,
                onValueChange = { dslSource = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                label = { Text("Paste or edit NanoDSL code") }
            )

            Spacer(Modifier.height(8.dp))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { dslSource = SAMPLE_DSL }) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset")
                }

                FilledTonalButton(onClick = { dslSource = PRODUCT_CARD_DSL }) {
                    Text("Product Card")
                }

                FilledTonalButton(onClick = { dslSource = COUNTER_DSL }) {
                    Text("Counter")
                }

                FilledTonalButton(onClick = { dslSource = CONTACT_FORM_DSL }) {
                    Text("Contact Form")
                }

                FilledTonalButton(onClick = { dslSource = LOGIN_FORM_DSL }) {
                    Text("Login Form")
                }
            }

            // Error message
            errorMessage?.let { error ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Error: $error",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Right Panel - Preview
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 8.dp)
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))

            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    parsedIR?.let { ir ->
                        NanoUIRenderer.Render(ir)
                    } ?: run {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Enter valid NanoDSL to see preview",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Sample DSL templates
private val SAMPLE_DSL = """
component ProductCard:
    Card(padding="md", shadow="sm"):
        VStack(spacing="sm"):
            Image(src="product.jpg", aspect="16:9")
            Text("Product Name", style="h3")
            Text("$29.99", style="body")
            HStack(spacing="sm"):
                Badge("New", color="green")
                Badge("Sale", color="red")
            Button("Add to Cart", intent="primary")
""".trimIndent()

private val PRODUCT_CARD_DSL = """
component ShoppingItem:
    Card(padding="lg"):
        VStack(spacing="md"):
            Image(src="sneakers.jpg")
            HStack(justify="between"):
                Text("Nike Air Max", style="h3")
                Badge("In Stock", color="green")
            Text("Premium running shoes", style="caption")
            Divider
            HStack(spacing="sm", align="center"):
                Text("$129.00", style="h4")
                Button("Buy Now", intent="primary")
""".trimIndent()

private val COUNTER_DSL = """
component Counter:
    Card(padding="lg", shadow="md"):
        VStack(spacing="md"):
            Text("Counter Example", style="h2")
            HStack(spacing="md", align="center", justify="center"):
                Button("-", intent="secondary")
                Text("0", style="h1")
                Button("+", intent="primary")
            Divider
            Text("Click buttons to change value", style="caption")
""".trimIndent()

private val CONTACT_FORM_DSL = """
component ContactForm:
    state:
        name: str = ""
        email: str = ""
        message: str = ""
        is_submitting: bool = False

    Card(padding="lg", shadow="md"):
        VStack(spacing="md"):
            Text("Contact Us", style="h2")
            VStack(spacing="sm"):
                Text("Name", style="caption")
                Input(value:=state.name, placeholder="Enter your name")
            VStack(spacing="sm"):
                Text("Email", style="caption")
                Input(value:=state.email, placeholder="Enter your email")
            VStack(spacing="sm"):
                Text("Message", style="caption")
                Input(value:=state.message, placeholder="Enter your message")
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

private val LOGIN_FORM_DSL = """
component LoginForm:
    state:
        email: str = ""
        password: str = ""
        loading: bool = False

    Card(padding="lg", shadow="md"):
        VStack(spacing="md"):
            Text("Login", style="h2")
            VStack(spacing="sm"):
                Text("Email", style="caption")
                Input(value:=state.email, placeholder="Enter your email")
            VStack(spacing="sm"):
                Text("Password", style="caption")
                Input(value:=state.password, placeholder="Enter your password")
            Button("Login", intent="primary"):
                on_click:
                    state.loading = True
                    Fetch(
                        url="/api/login",
                        method="POST",
                        body={"email": state.email, "password": state.password},
                        on_success: Navigate(to="/dashboard"),
                        on_error: ShowToast("Login failed")
                    )
            HStack(align="center", justify="center"):
                Text("Don't have an account?", style="caption")
                Button("Sign Up", intent="secondary")
""".trimIndent()

