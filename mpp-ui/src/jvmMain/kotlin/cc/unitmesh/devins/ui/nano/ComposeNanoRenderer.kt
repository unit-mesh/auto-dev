package cc.unitmesh.devins.ui.nano

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.xuiper.ir.NanoIR
import kotlinx.serialization.json.jsonPrimitive

/**
 * NanoUI Compose Renderer
 *
 * Renders NanoIR components to Compose UI.
 * Uses Material 3 components for consistent theming.
 *
 * This renderer follows the component-specific method pattern from NanoRenderer interface.
 * Each component type has its own Render* method, making it easy to identify
 * missing implementations when new components are added.
 *
 * @see cc.unitmesh.xuiper.render.NanoRenderer for the interface pattern
 */
object ComposeNanoRenderer {

    // ============================================================================
    // Main Entry Point
    // ============================================================================

    /**
     * Render a NanoIR node to Compose UI.
     * Dispatches to component-specific render methods.
     */
    @Composable
    fun Render(ir: NanoIR, modifier: Modifier = Modifier) {
        RenderNode(ir, modifier)
    }

    /**
     * Dispatch rendering based on component type.
     * Routes to the appropriate component-specific render method.
     */
    @Composable
    fun RenderNode(ir: NanoIR, modifier: Modifier = Modifier) {
        when (ir.type) {
            // Layout
            "VStack" -> RenderVStack(ir, modifier)
            "HStack" -> RenderHStack(ir, modifier)
            // Container
            "Card" -> RenderCard(ir, modifier)
            "Form" -> RenderForm(ir, modifier)
            // Content
            "Text" -> RenderText(ir, modifier)
            "Image" -> RenderImage(ir, modifier)
            "Badge" -> RenderBadge(ir, modifier)
            "Divider" -> RenderDivider(ir, modifier)
            // Input
            "Button" -> RenderButton(ir, modifier)
            "Input" -> RenderInput(ir, modifier)
            "Checkbox" -> RenderCheckbox(ir, modifier)
            "TextArea" -> RenderTextArea(ir, modifier)
            "Select" -> RenderSelect(ir, modifier)
            // Control Flow
            "Conditional" -> RenderConditional(ir, modifier)
            "ForLoop" -> RenderForLoop(ir, modifier)
            // Meta
            "Component" -> RenderComponent(ir, modifier)
            else -> RenderUnknown(ir, modifier)
        }
    }

    // ============================================================================
    // Layout Components
    // ============================================================================

    @Composable
    fun RenderVStack(ir: NanoIR, modifier: Modifier = Modifier) {
        val spacing = ir.props["spacing"]?.jsonPrimitive?.content?.toSpacing() ?: 8.dp
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            ir.children?.forEach { child ->
                RenderNode(child)
            }
        }
    }

    @Composable
    fun RenderHStack(ir: NanoIR, modifier: Modifier = Modifier) {
        val spacing = ir.props["spacing"]?.jsonPrimitive?.content?.toSpacing() ?: 8.dp
        val align = ir.props["align"]?.jsonPrimitive?.content?.toVerticalAlignment() ?: Alignment.CenterVertically
        val justify = ir.props["justify"]?.jsonPrimitive?.content?.toHorizontalArrangement() ?: Arrangement.Start

        Row(
            modifier = modifier,
            horizontalArrangement = justify,
            verticalAlignment = align
        ) {
            ir.children?.forEach { child ->
                RenderNode(child)
            }
        }
    }

    // ============================================================================
    // Container Components
    // ============================================================================

    @Composable
    fun RenderCard(ir: NanoIR, modifier: Modifier = Modifier) {
        val padding = ir.props["padding"]?.jsonPrimitive?.content?.toSpacing() ?: 16.dp
        val shadow = ir.props["shadow"]?.jsonPrimitive?.content?.toElevation() ?: 2.dp

        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = shadow),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(padding)) {
                ir.children?.forEach { child ->
                    RenderNode(child)
                }
            }
        }
    }

    @Composable
    fun RenderForm(ir: NanoIR, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ir.children?.forEach { child ->
                RenderNode(child)
            }
        }
    }

    // ============================================================================
    // Content Components
    // ============================================================================

    @Composable
    fun RenderText(ir: NanoIR, modifier: Modifier = Modifier) {
        val content = ir.props["content"]?.jsonPrimitive?.content ?: ""
        val style = ir.props["style"]?.jsonPrimitive?.content

        val textStyle = when (style) {
            "h1" -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            "h2" -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
            "h3" -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium)
            "h4" -> MaterialTheme.typography.titleLarge
            "body" -> MaterialTheme.typography.bodyLarge
            "caption" -> MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> MaterialTheme.typography.bodyMedium
        }

        Text(text = content, style = textStyle, modifier = modifier)
    }

    @Composable
    fun RenderImage(ir: NanoIR, modifier: Modifier = Modifier) {
        val src = ir.props["src"]?.jsonPrimitive?.content ?: ""
        // Placeholder for image - in real app would load from URL
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
    fun RenderBadge(ir: NanoIR, modifier: Modifier = Modifier) {
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

        val textColor = when (colorName) {
            "yellow" -> Color.Black
            else -> Color.White
        }

        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(4.dp),
            color = bgColor
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = textColor,
                fontSize = 12.sp
            )
        }
    }

    @Composable
    fun RenderDivider(ir: NanoIR, modifier: Modifier = Modifier) {
        HorizontalDivider(modifier.padding(vertical = 8.dp))
    }

    // ============================================================================
    // Input Components
    // ============================================================================

    @Composable
    fun RenderButton(ir: NanoIR, modifier: Modifier = Modifier) {
        val label = ir.props["label"]?.jsonPrimitive?.content ?: "Button"
        val intent = ir.props["intent"]?.jsonPrimitive?.content

        val colors = when (intent) {
            "primary" -> ButtonDefaults.buttonColors()
            "secondary" -> ButtonDefaults.outlinedButtonColors()
            "danger" -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            else -> ButtonDefaults.buttonColors()
        }

        Button(onClick = { }, colors = colors, modifier = modifier) {
            Text(label)
        }
    }

    @Composable
    fun RenderInput(ir: NanoIR, modifier: Modifier = Modifier) {
        val placeholder = ir.props["placeholder"]?.jsonPrimitive?.content ?: ""

        OutlinedTextField(
            value = "",
            onValueChange = { },
            placeholder = { Text(placeholder) },
            modifier = modifier.fillMaxWidth(),
            singleLine = true
        )
    }

    @Composable
    fun RenderCheckbox(ir: NanoIR, modifier: Modifier = Modifier) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = false, onCheckedChange = { })
        }
    }

    @Composable
    fun RenderTextArea(ir: NanoIR, modifier: Modifier = Modifier) {
        val placeholder = ir.props["placeholder"]?.jsonPrimitive?.content ?: ""
        val rows = ir.props["rows"]?.jsonPrimitive?.content?.toIntOrNull() ?: 4

        OutlinedTextField(
            value = "",
            onValueChange = { },
            placeholder = { Text(placeholder) },
            modifier = modifier
                .fillMaxWidth()
                .height((rows * 24 + 32).dp),
            minLines = rows,
            maxLines = rows
        )
    }

    @Composable
    fun RenderSelect(ir: NanoIR, modifier: Modifier = Modifier) {
        val placeholder = ir.props["placeholder"]?.jsonPrimitive?.content ?: "Select..."

        // Simple dropdown placeholder - in real app would use ExposedDropdownMenuBox
        Box(
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // ============================================================================
    // Control Flow Components
    // ============================================================================

    @Composable
    fun RenderConditional(ir: NanoIR, modifier: Modifier = Modifier) {
        // Render the then branch (conditional evaluation happens at runtime)
        // In static preview, we just render children directly
        Column(modifier = modifier) {
            ir.children?.forEach { child ->
                RenderNode(child)
            }
        }
    }

    @Composable
    fun RenderForLoop(ir: NanoIR, modifier: Modifier = Modifier) {
        // Render the loop body once as a preview
        // In static preview, show a single iteration of the loop
        Column(modifier = modifier) {
            ir.children?.forEach { child ->
                RenderNode(child)
            }
        }
    }

    // ============================================================================
    // Meta Components
    // ============================================================================

    @Composable
    fun RenderComponent(ir: NanoIR, modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            ir.children?.forEach { child ->
                RenderNode(child)
            }
        }
    }

    @Composable
    fun RenderUnknown(ir: NanoIR, modifier: Modifier = Modifier) {
        Text(
            text = "Unknown: ${ir.type}",
            modifier = modifier,
            color = MaterialTheme.colorScheme.error
        )
    }

    // ============================================================================
    // Extension Functions
    // ============================================================================

    private fun String.toSpacing() = when (this) {
        "xs" -> 4.dp
        "sm" -> 8.dp
        "md" -> 16.dp
        "lg" -> 24.dp
        "xl" -> 32.dp
        else -> 8.dp
    }

    private fun String.toElevation() = when (this) {
        "none" -> 0.dp
        "sm" -> 2.dp
        "md" -> 4.dp
        "lg" -> 8.dp
        else -> 2.dp
    }

    private fun String.toVerticalAlignment() = when (this) {
        "start", "top" -> Alignment.Top
        "center" -> Alignment.CenterVertically
        "end", "bottom" -> Alignment.Bottom
        else -> Alignment.CenterVertically
    }

    private fun String.toHorizontalArrangement() = when (this) {
        "start" -> Arrangement.Start
        "center" -> Arrangement.Center
        "end" -> Arrangement.End
        "between" -> Arrangement.SpaceBetween
        "around" -> Arrangement.SpaceAround
        else -> Arrangement.Start
    }
}

