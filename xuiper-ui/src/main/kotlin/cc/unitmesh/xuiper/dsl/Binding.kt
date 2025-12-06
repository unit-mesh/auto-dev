package cc.unitmesh.xuiper.dsl

/**
 * Represents a data binding in NanoDSL
 * 
 * NanoDSL supports two binding operators:
 * - `<<` (Subscribe): One-way data flow, content updates when source changes
 * - `:=` (Two-way): Bidirectional binding for input components
 */
sealed class Binding {
    /**
     * Subscribe binding using `<<` operator
     * Example: `Text(content << f"Total: ${state.count * state.price}")`
     */
    data class Subscribe(
        val expression: String
    ) : Binding()

    /**
     * Two-way binding using `:=` operator
     * Example: `Input(value := state.email)`
     */
    data class TwoWay(
        val path: String
    ) : Binding()

    /**
     * Static value (no binding)
     */
    data class Static(
        val value: String
    ) : Binding()

    companion object {
        /**
         * Parse a binding expression from DSL text
         */
        fun parse(text: String): Binding {
            val trimmed = text.trim()
            return when {
                trimmed.contains("<<") -> {
                    val expr = trimmed.substringAfter("<<").trim()
                    Subscribe(expr)
                }
                trimmed.contains(":=") -> {
                    val path = trimmed.substringAfter(":=").trim()
                    TwoWay(path)
                }
                else -> Static(trimmed)
            }
        }
    }
}

