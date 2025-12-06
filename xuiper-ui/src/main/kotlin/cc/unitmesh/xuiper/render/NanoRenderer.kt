package cc.unitmesh.xuiper.render

import cc.unitmesh.xuiper.ir.NanoIR

/**
 * Platform-agnostic NanoUI Renderer interface
 *
 * This interface defines the contract for rendering NanoIR components.
 * Platform-specific implementations (Compose, React, Flutter) should implement this interface.
 *
 * Each component type has its own render method, ensuring compile-time
 * checking when new components are added. This follows the pattern used
 * in CodingAgentRenderer.
 *
 * The render result type [T] varies by platform:
 * - Compose: @Composable functions (Unit)
 * - React: ReactElement
 * - Flutter: Widget
 * - HTML: String
 *
 * @see cc.unitmesh.agent.render.CodingAgentRenderer for the pattern reference
 */
interface NanoRenderer<T> {
    // ============================================================================
    // Full Document Rendering
    // ============================================================================

    /**
     * Render a complete NanoIR tree to platform-specific output.
     * This is the entry point for rendering a full document.
     */
    fun render(ir: NanoIR): T

    /**
     * Dispatch rendering based on component type.
     * This method routes to the appropriate component-specific render method.
     */
    fun renderNode(ir: NanoIR): T

    // ============================================================================
    // Layout Components
    // ============================================================================

    /**
     * Render a vertical stack layout.
     * Props: spacing (xs|sm|md|lg|xl), align (start|center|end|stretch)
     */
    fun renderVStack(ir: NanoIR): T

    /**
     * Render a horizontal stack layout.
     * Props: spacing, align, justify (start|center|end|between|around)
     */
    fun renderHStack(ir: NanoIR): T

    // ============================================================================
    // Container Components
    // ============================================================================

    /**
     * Render a card container with shadow.
     * Props: padding (xs|sm|md|lg|xl), shadow (none|sm|md|lg)
     */
    fun renderCard(ir: NanoIR): T

    /**
     * Render a form container.
     * Props: onSubmit (action reference)
     */
    fun renderForm(ir: NanoIR): T

    // ============================================================================
    // Content Components
    // ============================================================================

    /**
     * Render text content.
     * Props: content (string), style (h1|h2|h3|h4|body|caption)
     */
    fun renderText(ir: NanoIR): T

    /**
     * Render an image.
     * Props: src (url), aspect (ratio), radius (none|sm|md|lg|full), width
     */
    fun renderImage(ir: NanoIR): T

    /**
     * Render a badge/tag.
     * Props: text (string), color (string)
     */
    fun renderBadge(ir: NanoIR): T

    /**
     * Render a horizontal divider line.
     */
    fun renderDivider(ir: NanoIR): T

    // ============================================================================
    // Input Components
    // ============================================================================

    /**
     * Render a clickable button.
     * Props: label (string), intent (primary|secondary|default|danger), icon
     */
    fun renderButton(ir: NanoIR): T

    /**
     * Render a text input field.
     * Props: placeholder, type, value (binding)
     */
    fun renderInput(ir: NanoIR): T

    /**
     * Render a checkbox input.
     * Props: checked (binding)
     */
    fun renderCheckbox(ir: NanoIR): T

    /**
     * Render a multi-line text area.
     * Props: placeholder, rows, value (binding)
     */
    fun renderTextArea(ir: NanoIR): T

    /**
     * Render a dropdown select.
     * Props: options, placeholder, value (binding)
     */
    fun renderSelect(ir: NanoIR): T

    // ============================================================================
    // Control Flow Components
    // ============================================================================

    /**
     * Render conditional content (if block).
     * Uses ir.condition for the condition expression.
     */
    fun renderConditional(ir: NanoIR): T

    /**
     * Render a loop (for block).
     * Uses ir.loop for variable and iterable.
     */
    fun renderForLoop(ir: NanoIR): T

    // ============================================================================
    // Meta Components
    // ============================================================================

    /**
     * Render a component wrapper.
     * Props: name (component name)
     */
    fun renderComponent(ir: NanoIR): T

    /**
     * Render an unknown/unsupported component.
     * Called when ir.type doesn't match any known component.
     */
    fun renderUnknown(ir: NanoIR): T
}

/**
 * Render context for stateful rendering
 */
data class RenderContext(
    /** Current state values */
    val state: Map<String, Any> = emptyMap(),
    
    /** Action dispatcher */
    val dispatch: ((NanoRenderAction) -> Unit)? = null,
    
    /** Theme configuration */
    val theme: NanoTheme = NanoTheme.Default
)

/**
 * Action dispatched during rendering
 */
sealed class NanoRenderAction {
    data class StateMutation(val path: String, val value: Any) : NanoRenderAction()
    data class Navigate(val to: String) : NanoRenderAction()
    data class Fetch(val url: String, val method: String = "GET") : NanoRenderAction()
    data class ShowToast(val message: String) : NanoRenderAction()
}

/**
 * Theme configuration for NanoUI
 */
data class NanoTheme(
    val spacing: SpacingScale = SpacingScale.Default,
    val colors: ColorScheme = ColorScheme.Default
) {
    companion object {
        val Default = NanoTheme()
    }
}

/**
 * Spacing scale following design tokens
 */
data class SpacingScale(
    val xs: Int = 4,
    val sm: Int = 8,
    val md: Int = 16,
    val lg: Int = 24,
    val xl: Int = 32
) {
    companion object {
        val Default = SpacingScale()
    }

    fun resolve(value: String?): Int {
        return when (value) {
            "xs" -> xs
            "sm" -> sm
            "md" -> md
            "lg" -> lg
            "xl" -> xl
            else -> value?.toIntOrNull() ?: md
        }
    }
}

/**
 * Color scheme for NanoUI theming
 */
data class ColorScheme(
    val primary: String = "#6200EE",
    val secondary: String = "#03DAC6",
    val background: String = "#FFFFFF",
    val surface: String = "#FFFFFF",
    val error: String = "#B00020",
    val onPrimary: String = "#FFFFFF",
    val onSecondary: String = "#000000",
    val onBackground: String = "#000000",
    val onSurface: String = "#000000",
    val onError: String = "#FFFFFF"
) {
    companion object {
        val Default = ColorScheme()
        
        val Dark = ColorScheme(
            primary = "#BB86FC",
            secondary = "#03DAC6",
            background = "#121212",
            surface = "#1E1E1E",
            error = "#CF6679",
            onPrimary = "#000000",
            onSecondary = "#000000",
            onBackground = "#FFFFFF",
            onSurface = "#FFFFFF",
            onError = "#000000"
        )
    }

    fun resolveIntent(intent: String?): String {
        return when (intent) {
            "primary" -> primary
            "secondary" -> secondary
            "error", "danger" -> error
            else -> primary
        }
    }
}

