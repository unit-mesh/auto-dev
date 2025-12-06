package cc.unitmesh.xuiper.render

import cc.unitmesh.xuiper.ir.NanoIR
import kotlinx.serialization.json.jsonPrimitive

/**
 * HTML Renderer for NanoIR
 *
 * Renders NanoIR components to static HTML.
 * Useful for server-side rendering and testing.
 *
 * Implements NanoRenderer<String> with component-specific methods.
 */
class HtmlRenderer(
    private val context: RenderContext = RenderContext()
) : NanoRenderer<String> {

    override fun render(ir: NanoIR): String {
        return buildString {
            append("<!DOCTYPE html>\n<html>\n<head>\n")
            append("<meta charset=\"UTF-8\">\n")
            append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            append("<style>\n")
            append(generateCss())
            append("</style>\n")
            append("</head>\n<body>\n")
            append(renderNode(ir))
            append("</body>\n</html>")
        }
    }

    override fun renderNode(ir: NanoIR): String {
        return when (ir.type) {
            "Component" -> renderComponent(ir)
            "VStack" -> renderVStack(ir)
            "HStack" -> renderHStack(ir)
            "Card" -> renderCard(ir)
            "Form" -> renderForm(ir)
            "Text" -> renderText(ir)
            "Image" -> renderImage(ir)
            "Badge" -> renderBadge(ir)
            "Divider" -> renderDivider(ir)
            "Button" -> renderButton(ir)
            "Input" -> renderInput(ir)
            "Checkbox" -> renderCheckbox(ir)
            "TextArea" -> renderTextArea(ir)
            "Select" -> renderSelect(ir)
            "Conditional" -> renderConditional(ir)
            "ForLoop" -> renderForLoop(ir)
            else -> renderUnknown(ir)
        }
    }

    // ============================================================================
    // Layout Components
    // ============================================================================

    override fun renderVStack(ir: NanoIR): String {
        val spacing = ir.props["spacing"]?.jsonPrimitive?.content ?: "md"
        val align = ir.props["align"]?.jsonPrimitive?.content ?: "stretch"
        return buildString {
            append("<div class=\"nano-vstack spacing-$spacing align-$align\">\n")
            ir.children?.forEach { append(renderNode(it)) }
            append("</div>\n")
        }
    }

    override fun renderHStack(ir: NanoIR): String {
        val spacing = ir.props["spacing"]?.jsonPrimitive?.content ?: "md"
        val align = ir.props["align"]?.jsonPrimitive?.content ?: "center"
        val justify = ir.props["justify"]?.jsonPrimitive?.content ?: "start"
        return buildString {
            append("<div class=\"nano-hstack spacing-$spacing align-$align justify-$justify\">\n")
            ir.children?.forEach { append(renderNode(it)) }
            append("</div>\n")
        }
    }

    // ============================================================================
    // Container Components
    // ============================================================================

    override fun renderCard(ir: NanoIR): String {
        val padding = ir.props["padding"]?.jsonPrimitive?.content ?: "md"
        val shadow = ir.props["shadow"]?.jsonPrimitive?.content ?: "sm"
        return buildString {
            append("<div class=\"nano-card padding-$padding shadow-$shadow\">\n")
            ir.children?.forEach { append(renderNode(it)) }
            append("</div>\n")
        }
    }

    override fun renderForm(ir: NanoIR): String {
        val onSubmit = ir.props["onSubmit"]?.jsonPrimitive?.content
        return buildString {
            append("<form class=\"nano-form\"")
            if (onSubmit != null) append(" data-action=\"$onSubmit\"")
            append(">\n")
            ir.children?.forEach { append(renderNode(it)) }
            append("</form>\n")
        }
    }

    // ============================================================================
    // Content Components
    // ============================================================================

    override fun renderText(ir: NanoIR): String {
        val content = ir.props["content"]?.jsonPrimitive?.content ?: ""
        val style = ir.props["style"]?.jsonPrimitive?.content ?: "body"
        val tag = when (style) {
            "h1" -> "h1"
            "h2" -> "h2"
            "h3" -> "h3"
            "h4" -> "h4"
            "caption" -> "small"
            else -> "p"
        }
        return "<$tag class=\"nano-text style-$style\">$content</$tag>\n"
    }

    override fun renderImage(ir: NanoIR): String {
        val src = ir.props["src"]?.jsonPrimitive?.content ?: ""
        val aspect = ir.props["aspect"]?.jsonPrimitive?.content
        val radius = ir.props["radius"]?.jsonPrimitive?.content ?: "none"
        val aspectClass = aspect?.replace("/", "-") ?: "auto"
        val alt = ir.props["alt"]?.jsonPrimitive?.content ?: "Image"
        return "<img src=\"$src\" class=\"nano-image aspect-$aspectClass radius-$radius\" alt=\"$alt\">\n"
    }

    override fun renderBadge(ir: NanoIR): String {
        val text = ir.props["text"]?.jsonPrimitive?.content ?: ""
        val color = ir.props["color"]?.jsonPrimitive?.content ?: "default"
        return "<span class=\"nano-badge color-$color\">$text</span>\n"
    }

    override fun renderDivider(ir: NanoIR): String {
        return "<hr class=\"nano-divider\">\n"
    }

    // ============================================================================
    // Input Components
    // ============================================================================

    override fun renderButton(ir: NanoIR): String {
        val label = ir.props["label"]?.jsonPrimitive?.content ?: ""
        val intent = ir.props["intent"]?.jsonPrimitive?.content ?: "default"
        val icon = ir.props["icon"]?.jsonPrimitive?.content
        return buildString {
            append("<button class=\"nano-button intent-$intent\">")
            if (icon != null) append("<span class=\"icon\">$icon</span> ")
            append(label)
            append("</button>\n")
        }
    }

    override fun renderInput(ir: NanoIR): String {
        val placeholder = ir.props["placeholder"]?.jsonPrimitive?.content ?: ""
        val type = ir.props["type"]?.jsonPrimitive?.content ?: "text"
        return "<input type=\"$type\" class=\"nano-input\" placeholder=\"$placeholder\">\n"
    }

    override fun renderCheckbox(ir: NanoIR): String {
        val label = ir.props["label"]?.jsonPrimitive?.content
        return if (label != null) {
            "<label class=\"nano-checkbox-wrapper\"><input type=\"checkbox\" class=\"nano-checkbox\"><span>$label</span></label>\n"
        } else {
            "<input type=\"checkbox\" class=\"nano-checkbox\">\n"
        }
    }

    override fun renderTextArea(ir: NanoIR): String {
        val placeholder = ir.props["placeholder"]?.jsonPrimitive?.content ?: ""
        val rows = ir.props["rows"]?.jsonPrimitive?.content ?: "4"
        return "<textarea class=\"nano-textarea\" placeholder=\"$placeholder\" rows=\"$rows\"></textarea>\n"
    }

    override fun renderSelect(ir: NanoIR): String {
        val placeholder = ir.props["placeholder"]?.jsonPrimitive?.content
        val options = ir.props["options"]?.jsonPrimitive?.content
        return buildString {
            append("<select class=\"nano-select\">\n")
            if (placeholder != null) {
                append("  <option value=\"\" disabled selected>$placeholder</option>\n")
            }
            // Options would be populated dynamically
            if (options != null) {
                append("  <!-- options: $options -->\n")
            }
            append("</select>\n")
        }
    }

    // ============================================================================
    // Control Flow Components
    // ============================================================================

    override fun renderConditional(ir: NanoIR): String {
        // In static HTML, we render the then branch
        // Dynamic evaluation would happen on client-side
        return buildString {
            append("<!-- if: ${ir.condition} -->\n")
            ir.children?.forEach { append(renderNode(it)) }
            append("<!-- endif -->\n")
        }
    }

    override fun renderForLoop(ir: NanoIR): String {
        val loop = ir.loop
        return buildString {
            append("<!-- for ${loop?.variable} in ${loop?.iterable} -->\n")
            ir.children?.forEach { append(renderNode(it)) }
            append("<!-- endfor -->\n")
        }
    }

    // ============================================================================
    // Meta Components
    // ============================================================================

    override fun renderComponent(ir: NanoIR): String {
        val name = ir.props["name"]?.jsonPrimitive?.content ?: "Component"
        return buildString {
            append("<div class=\"nano-component\" data-name=\"$name\">\n")
            ir.children?.forEach { append(renderNode(it)) }
            append("</div>\n")
        }
    }

    override fun renderUnknown(ir: NanoIR): String {
        return "<!-- Unknown component: ${ir.type} -->\n"
    }

    private fun generateCss(): String = """
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }

        .nano-component { width: 100%; }

        .nano-vstack { display: flex; flex-direction: column; }
        .nano-hstack { display: flex; flex-direction: row; align-items: center; }

        .spacing-xs { gap: 4px; }
        .spacing-sm { gap: 8px; }
        .spacing-md { gap: 16px; }
        .spacing-lg { gap: 24px; }
        .spacing-xl { gap: 32px; }

        .align-start { align-items: flex-start; }
        .align-center { align-items: center; }
        .align-end { align-items: flex-end; }
        .align-stretch { align-items: stretch; }

        .justify-start { justify-content: flex-start; }
        .justify-center { justify-content: center; }
        .justify-end { justify-content: flex-end; }
        .justify-between { justify-content: space-between; }

        .nano-card {
            background: white;
            border-radius: 8px;
            overflow: hidden;
        }
        .padding-xs { padding: 4px; }
        .padding-sm { padding: 8px; }
        .padding-md { padding: 16px; }
        .padding-lg { padding: 24px; }

        .shadow-none { box-shadow: none; }
        .shadow-sm { box-shadow: 0 1px 2px rgba(0,0,0,0.1); }
        .shadow-md { box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
        .shadow-lg { box-shadow: 0 10px 15px rgba(0,0,0,0.1); }

        .nano-text { margin: 0; }
        .style-h1 { font-size: 2rem; font-weight: bold; }
        .style-h2 { font-size: 1.5rem; font-weight: bold; }
        .style-h3 { font-size: 1.25rem; font-weight: bold; }
        .style-h4 { font-size: 1rem; font-weight: bold; }
        .style-body { font-size: 1rem; }
        .style-caption { font-size: 0.875rem; color: #666; }

        .nano-button {
            padding: 8px 16px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 1rem;
        }
        .intent-primary { background: #6200EE; color: white; }
        .intent-secondary { background: #03DAC6; color: black; }
        .intent-default { background: #E0E0E0; color: black; }
        .intent-error, .intent-danger { background: #B00020; color: white; }

        .nano-image { max-width: 100%; height: auto; display: block; }
        .radius-sm { border-radius: 4px; }
        .radius-md { border-radius: 8px; }
        .radius-lg { border-radius: 16px; }
        .radius-full { border-radius: 9999px; }

        .nano-badge {
            display: inline-block;
            padding: 2px 8px;
            border-radius: 12px;
            font-size: 0.75rem;
            font-weight: 500;
        }
        .color-green { background: #C8E6C9; color: #2E7D32; }
        .color-red { background: #FFCDD2; color: #C62828; }
        .color-blue { background: #BBDEFB; color: #1565C0; }
        .color-default { background: #E0E0E0; color: #424242; }

        .nano-input {
            padding: 8px 12px;
            border: 1px solid #E0E0E0;
            border-radius: 4px;
            font-size: 1rem;
            width: 100%;
        }

        .nano-textarea {
            padding: 8px 12px;
            border: 1px solid #E0E0E0;
            border-radius: 4px;
            font-size: 1rem;
            width: 100%;
            resize: vertical;
            font-family: inherit;
        }

        .nano-select {
            padding: 8px 12px;
            border: 1px solid #E0E0E0;
            border-radius: 4px;
            font-size: 1rem;
            width: 100%;
            background: white;
        }

        .nano-form {
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .nano-checkbox-wrapper {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            cursor: pointer;
        }

        .nano-checkbox {
            width: 16px;
            height: 16px;
            accent-color: #1976D2;
            cursor: pointer;
        }

        .nano-divider { border: none; border-top: 1px solid #E0E0E0; margin: 16px 0; }
    """.trimIndent()
}

