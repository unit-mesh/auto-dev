package cc.unitmesh.devins.ui.nano

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.xuiper.ir.NanoActionIR
import cc.unitmesh.xuiper.ir.NanoIR
import cc.unitmesh.xuiper.ir.NanoStateIR
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Stateful NanoUI Compose Renderer
 *
 * This renderer maintains state and handles actions for interactive NanoDSL components.
 * It wraps the component rendering with a state context that:
 * 1. Initializes state from NanoIR state definitions
 * 2. Passes state values to components via bindings
 * 3. Updates state when actions are triggered
 */
object StatefulNanoRenderer {

    /**
     * Render a NanoIR tree with state management.
     * Automatically initializes state from the IR and provides action handlers.
     */
    @Composable
    fun Render(ir: NanoIR, modifier: Modifier = Modifier) {
        // Initialize state from IR
        val stateMap = remember { mutableStateMapOf<String, Any>() }

        // Initialize state values from IR state definitions
        LaunchedEffect(ir) {
            ir.state?.variables?.forEach { (name, varDef) ->
                val defaultValue = varDef.defaultValue
                stateMap[name] = when (varDef.type) {
                    "int" -> defaultValue?.jsonPrimitive?.intOrNull ?: 0
                    "float" -> defaultValue?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f
                    "bool" -> defaultValue?.jsonPrimitive?.booleanOrNull ?: false
                    "str" -> defaultValue?.jsonPrimitive?.content ?: ""
                    else -> defaultValue?.jsonPrimitive?.content ?: ""
                }
            }
        }

        // Create action handler
        val handleAction: (NanoActionIR) -> Unit = handleAction@{ action ->
            when (action.type) {
                "stateMutation" -> {
                    val payload = action.payload ?: return@handleAction
                    val path = payload["path"]?.jsonPrimitive?.content ?: return@handleAction
                    val operation = payload["operation"]?.jsonPrimitive?.content ?: "SET"
                    val valueStr = payload["value"]?.jsonPrimitive?.content ?: ""

                    val currentValue = stateMap[path]
                    val newValue = when (operation) {
                        "ADD" -> {
                            when (currentValue) {
                                is Int -> currentValue + (valueStr.toIntOrNull() ?: 1)
                                is Float -> currentValue + (valueStr.toFloatOrNull() ?: 1f)
                                else -> currentValue
                            }
                        }
                        "SUBTRACT" -> {
                            when (currentValue) {
                                is Int -> currentValue - (valueStr.toIntOrNull() ?: 1)
                                is Float -> currentValue - (valueStr.toFloatOrNull() ?: 1f)
                                else -> currentValue
                            }
                        }
                        "SET" -> {
                            when (currentValue) {
                                is Int -> valueStr.toIntOrNull() ?: 0
                                is Float -> valueStr.toFloatOrNull() ?: 0f
                                is Boolean -> valueStr.toBooleanStrictOrNull() ?: false
                                else -> valueStr
                            }
                        }
                        else -> valueStr
                    }

                    if (newValue != null) {
                        stateMap[path] = newValue
                    }
                }
            }
        }

        RenderNode(ir, stateMap, handleAction, modifier)
    }

    @Composable
    private fun RenderNode(
        ir: NanoIR,
        state: Map<String, Any>,
        onAction: (NanoActionIR) -> Unit,
        modifier: Modifier = Modifier
    ) {
        when (ir.type) {
            "VStack" -> RenderVStack(ir, state, onAction, modifier)
            "HStack" -> RenderHStack(ir, state, onAction, modifier)
            "Card" -> RenderCard(ir, state, onAction, modifier)
            "Form" -> RenderForm(ir, state, onAction, modifier)
            "Text" -> RenderText(ir, state, modifier)
            "Image" -> RenderImage(ir, modifier)
            "Badge" -> RenderBadge(ir, modifier)
            "Divider" -> RenderDivider(modifier)
            "Button" -> RenderButton(ir, onAction, modifier)
            "Input" -> RenderInput(ir, state, onAction, modifier)
            "Checkbox" -> RenderCheckbox(ir, state, onAction, modifier)
            "TextArea" -> RenderTextArea(ir, state, onAction, modifier)
            "Select" -> RenderSelect(ir, state, onAction, modifier)
            "Component" -> RenderComponent(ir, state, onAction, modifier)
            else -> RenderUnknown(ir, modifier)
        }
    }

    // Layout Components

    @Composable
    private fun RenderVStack(
        ir: NanoIR, state: Map<String, Any>, onAction: (NanoActionIR) -> Unit, modifier: Modifier
    ) {
        val spacing = ir.props["spacing"]?.jsonPrimitive?.content?.toSpacing() ?: 8.dp
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
            ir.children?.forEach { child -> RenderNode(child, state, onAction) }
        }
    }

    @Composable
    private fun RenderHStack(
        ir: NanoIR, state: Map<String, Any>, onAction: (NanoActionIR) -> Unit, modifier: Modifier
    ) {
        val spacing = ir.props["spacing"]?.jsonPrimitive?.content?.toSpacing() ?: 8.dp
        val align = ir.props["align"]?.jsonPrimitive?.content
        val justify = ir.props["justify"]?.jsonPrimitive?.content

        val verticalAlignment = when (align) {
            "center" -> Alignment.CenterVertically
            "top" -> Alignment.Top
            "bottom" -> Alignment.Bottom
            else -> Alignment.CenterVertically
        }
        val horizontalArrangement = when (justify) {
            "center" -> Arrangement.Center
            "between" -> Arrangement.SpaceBetween
            "end" -> Arrangement.End
            else -> Arrangement.spacedBy(spacing)
        }

        Row(
            modifier = modifier,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment
        ) {
            ir.children?.forEach { child -> RenderNode(child, state, onAction) }
        }
    }

    @Composable
    private fun RenderCard(
        ir: NanoIR, state: Map<String, Any>, onAction: (NanoActionIR) -> Unit, modifier: Modifier
    ) {
        val padding = ir.props["padding"]?.jsonPrimitive?.content?.toPadding() ?: 16.dp
        val shadow = ir.props["shadow"]?.jsonPrimitive?.content

        val elevation = when (shadow) {
            "sm" -> CardDefaults.cardElevation(defaultElevation = 2.dp)
            "md" -> CardDefaults.cardElevation(defaultElevation = 4.dp)
            "lg" -> CardDefaults.cardElevation(defaultElevation = 8.dp)
            else -> CardDefaults.cardElevation()
        }

        Card(modifier = modifier.fillMaxWidth(), elevation = elevation) {
            Column(modifier = Modifier.padding(padding)) {
                ir.children?.forEach { child -> RenderNode(child, state, onAction) }
            }
        }
    }

    @Composable
    private fun RenderForm(
        ir: NanoIR, state: Map<String, Any>, onAction: (NanoActionIR) -> Unit, modifier: Modifier
    ) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ir.children?.forEach { child -> RenderNode(child, state, onAction) }
        }
    }

    @Composable
    private fun RenderComponent(
        ir: NanoIR, state: Map<String, Any>, onAction: (NanoActionIR) -> Unit, modifier: Modifier
    ) {
        Column(modifier = modifier) {
            ir.children?.forEach { child -> RenderNode(child, state, onAction) }
        }
    }

    // Content Components

    @Composable
    private fun RenderText(ir: NanoIR, state: Map<String, Any>, modifier: Modifier) {
        // Check for binding first
        val binding = ir.bindings?.get("content")
        val content = if (binding != null) {
            // Get value from state based on binding expression
            val expr = binding.expression.removePrefix("state.")
            state[expr]?.toString() ?: ""
        } else {
            ir.props["content"]?.jsonPrimitive?.content ?: ""
        }

        val style = ir.props["style"]?.jsonPrimitive?.content

        val textStyle = when (style) {
            "h1" -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            "h2" -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
            "h3" -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium)
            "h4" -> MaterialTheme.typography.titleLarge
            "body" -> MaterialTheme.typography.bodyLarge
            "caption" -> MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> MaterialTheme.typography.bodyMedium
        }

        Text(text = content, style = textStyle, modifier = modifier)
    }

    @Composable
    private fun RenderImage(ir: NanoIR, modifier: Modifier) {
        val src = ir.props["src"]?.jsonPrimitive?.content ?: ""
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("Image: $src", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    @Composable
    private fun RenderBadge(ir: NanoIR, modifier: Modifier) {
        val text = ir.props["text"]?.jsonPrimitive?.content ?: ""
        val colorName = ir.props["color"]?.jsonPrimitive?.content

        val bgColor = when (colorName) {
            "green" -> Color(0xFF4CAF50)
            "red" -> Color(0xFFF44336)
            "blue" -> Color(0xFF2196F3)
            "yellow" -> Color(0xFFFFEB3B)
            "orange" -> Color(0xFFFF9800)
            else -> MaterialTheme.colorScheme.primaryContainer
        }

        val textColor = if (colorName == "yellow") Color.Black else Color.White

        Surface(modifier = modifier, shape = RoundedCornerShape(4.dp), color = bgColor) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = textColor,
                fontSize = 12.sp
            )
        }
    }

    @Composable
    private fun RenderDivider(modifier: Modifier) {
        HorizontalDivider(modifier.padding(vertical = 8.dp))
    }

    // Input Components

    @Composable
    private fun RenderButton(ir: NanoIR, onAction: (NanoActionIR) -> Unit, modifier: Modifier) {
        val label = ir.props["label"]?.jsonPrimitive?.content ?: "Button"
        val intent = ir.props["intent"]?.jsonPrimitive?.content
        val onClick = ir.actions?.get("onClick")

        val colors = when (intent) {
            "primary" -> ButtonDefaults.buttonColors()
            "secondary" -> ButtonDefaults.outlinedButtonColors()
            "danger" -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            else -> ButtonDefaults.buttonColors()
        }

        Button(
            onClick = { onClick?.let { onAction(it) } },
            colors = colors,
            modifier = modifier
        ) {
            Text(label)
        }
    }

    @Composable
    private fun RenderInput(
        ir: NanoIR, state: Map<String, Any>, onAction: (NanoActionIR) -> Unit, modifier: Modifier
    ) {
        val placeholder = ir.props["placeholder"]?.jsonPrimitive?.content ?: ""
        val binding = ir.bindings?.get("value")
        val statePath = binding?.expression?.removePrefix("state.")

        var value by remember(statePath, state[statePath]) {
            mutableStateOf(state[statePath]?.toString() ?: "")
        }

        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                value = newValue
                if (statePath != null) {
                    onAction(NanoActionIR(
                        type = "stateMutation",
                        payload = mapOf(
                            "path" to JsonPrimitive(statePath),
                            "operation" to JsonPrimitive("SET"),
                            "value" to JsonPrimitive(newValue)
                        )
                    ))
                }
            },
            placeholder = { Text(placeholder) },
            modifier = modifier.fillMaxWidth(),
            singleLine = true
        )
    }

    @Composable
    private fun RenderCheckbox(
        ir: NanoIR, state: Map<String, Any>, onAction: (NanoActionIR) -> Unit, modifier: Modifier
    ) {
        val binding = ir.bindings?.get("checked")
        val statePath = binding?.expression?.removePrefix("state.")
        val checked = (state[statePath] as? Boolean) ?: false

        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = checked,
                onCheckedChange = { newValue ->
                    if (statePath != null) {
                        onAction(NanoActionIR(
                            type = "stateMutation",
                            payload = mapOf(
                                "path" to JsonPrimitive(statePath),
                                "operation" to JsonPrimitive("SET"),
                                "value" to JsonPrimitive(newValue.toString())
                            )
                        ))
                    }
                }
            )
        }
    }

    @Composable
    private fun RenderTextArea(
        ir: NanoIR, state: Map<String, Any>, onAction: (NanoActionIR) -> Unit, modifier: Modifier
    ) {
        val placeholder = ir.props["placeholder"]?.jsonPrimitive?.content ?: ""
        val rows = ir.props["rows"]?.jsonPrimitive?.intOrNull ?: 4
        val binding = ir.bindings?.get("value")
        val statePath = binding?.expression?.removePrefix("state.")

        var value by remember(statePath, state[statePath]) {
            mutableStateOf(state[statePath]?.toString() ?: "")
        }

        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                value = newValue
                if (statePath != null) {
                    onAction(NanoActionIR(
                        type = "stateMutation",
                        payload = mapOf(
                            "path" to JsonPrimitive(statePath),
                            "operation" to JsonPrimitive("SET"),
                            "value" to JsonPrimitive(newValue)
                        )
                    ))
                }
            },
            placeholder = { Text(placeholder) },
            modifier = modifier.fillMaxWidth().height((rows * 24).dp),
            minLines = rows
        )
    }

    @Composable
    private fun RenderSelect(
        ir: NanoIR, state: Map<String, Any>, onAction: (NanoActionIR) -> Unit, modifier: Modifier
    ) {
        val placeholder = ir.props["placeholder"]?.jsonPrimitive?.content ?: "Select..."
        val binding = ir.bindings?.get("value")
        val statePath = binding?.expression?.removePrefix("state.")
        val selectedValue = state[statePath]?.toString() ?: ""
        var expanded by remember { mutableStateOf(false) }

        Box(modifier = modifier) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (selectedValue.isNotEmpty()) selectedValue else placeholder)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("Option 1", "Option 2", "Option 3").forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            if (statePath != null) {
                                onAction(NanoActionIR(
                                    type = "stateMutation",
                                    payload = mapOf(
                                        "path" to JsonPrimitive(statePath),
                                        "operation" to JsonPrimitive("SET"),
                                        "value" to JsonPrimitive(option)
                                    )
                                ))
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun RenderUnknown(ir: NanoIR, modifier: Modifier) {
        Surface(
            modifier = modifier.border(1.dp, Color.Red, RoundedCornerShape(4.dp)),
            color = Color.Red.copy(alpha = 0.1f)
        ) {
            Text(
                text = "Unknown: ${ir.type}",
                modifier = Modifier.padding(8.dp),
                color = Color.Red
            )
        }
    }

    // Utility extensions
    private fun String.toSpacing() = when (this) {
        "xs" -> 4.dp
        "sm" -> 8.dp
        "md" -> 16.dp
        "lg" -> 24.dp
        "xl" -> 32.dp
        else -> 8.dp
    }

    private fun String.toPadding() = when (this) {
        "xs" -> 4.dp
        "sm" -> 8.dp
        "md" -> 16.dp
        "lg" -> 24.dp
        "xl" -> 32.dp
        else -> 16.dp
    }
}

