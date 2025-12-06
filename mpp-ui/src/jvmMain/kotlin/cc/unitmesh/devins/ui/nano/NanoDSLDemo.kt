package cc.unitmesh.devins.ui.nano

import androidx.compose.foundation.background
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
import cc.unitmesh.xuiper.dsl.NanoDSL
import cc.unitmesh.xuiper.ir.NanoIR

/**
 * NanoDSL Demo Component
 *
 * Interactive demo that allows:
 * 1. Pasting NanoDSL code manually
 * 2. Generating UI from natural language via LLM (future)
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

    Row(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Left Panel - Editor
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 8.dp)
        ) {
            // Prompt input (for LLM generation)
            if (onGenerateWithLLM != null) {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    label = { Text("Describe the UI you want") },
                    placeholder = { Text("e.g., A product card with image, title, price, and buy button") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { onGenerateWithLLM(promptText) }) {
                            Icon(Icons.Default.PlayArrow, "Generate")
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
            }

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

