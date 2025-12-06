package cc.unitmesh.xuiper.render

import cc.unitmesh.xuiper.ir.NanoIR

/**
 * Platform-agnostic NanoUI Renderer interface
 * 
 * This interface defines the contract for rendering NanoIR components.
 * Platform-specific implementations (Compose, React, Flutter) should implement this interface.
 * 
 * The render result type [T] varies by platform:
 * - Compose: @Composable functions (Unit)
 * - React: ReactElement
 * - Flutter: Widget
 * - HTML: String
 */
interface NanoRenderer<T> {
    /**
     * Render a NanoIR tree to platform-specific output
     */
    fun render(ir: NanoIR): T

    /**
     * Render a specific component type
     */
    fun renderComponent(ir: NanoIR): T

    /**
     * Check if this renderer supports the given component type
     */
    fun supports(type: String): Boolean
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

