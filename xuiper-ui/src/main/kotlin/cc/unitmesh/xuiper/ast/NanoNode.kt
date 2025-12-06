package cc.unitmesh.xuiper.ast

import cc.unitmesh.xuiper.action.NanoAction

/**
 * NanoDSL AST Node - Abstract Syntax Tree representation
 * 
 * Represents all possible nodes in a NanoDSL component tree.
 * This sealed class hierarchy enables exhaustive pattern matching.
 */
sealed class NanoNode {
    /**
     * Component definition - the root of a NanoDSL component
     * Example: `component ProductCard(item: Product):`
     */
    data class Component(
        val name: String,
        val params: List<ComponentParam> = emptyList(),
        val state: StateBlock? = null,
        val children: List<NanoNode> = emptyList()
    ) : NanoNode()

    /**
     * Component parameter definition
     */
    data class ComponentParam(
        val name: String,
        val type: String? = null
    )

    /**
     * State block containing state variable definitions
     * Example: `state: count: int = 0`
     */
    data class StateBlock(
        val variables: List<StateVariable> = emptyList()
    )

    /**
     * State variable definition
     */
    data class StateVariable(
        val name: String,
        val type: String,
        val defaultValue: String? = null
    )

    // ============ Layout Components ============

    /**
     * Vertical stack layout
     * Example: `VStack(spacing="md"):`
     */
    data class VStack(
        val spacing: String? = null,
        val align: String? = null,
        val children: List<NanoNode> = emptyList()
    ) : NanoNode()

    /**
     * Horizontal stack layout
     * Example: `HStack(align="center", justify="between"):`
     */
    data class HStack(
        val spacing: String? = null,
        val align: String? = null,
        val justify: String? = null,
        val children: List<NanoNode> = emptyList()
    ) : NanoNode()

    // ============ Container Components ============

    /**
     * Card container
     * Example: `Card: padding: "md"`
     */
    data class Card(
        val padding: String? = null,
        val shadow: String? = null,
        val children: List<NanoNode> = emptyList()
    ) : NanoNode()

    // ============ Content Components ============

    /**
     * Text display
     * Example: `Text("Hello", style="h2")`
     */
    data class Text(
        val content: String,
        val style: String? = null,
        val binding: Binding? = null
    ) : NanoNode()

    /**
     * Image display
     * Example: `Image(src=item.image, aspect=16/9)`
     */
    data class Image(
        val src: String,
        val aspect: String? = null,
        val radius: String? = null,
        val width: Int? = null
    ) : NanoNode()

    /**
     * Badge component
     * Example: `Badge("New", color="green")`
     */
    data class Badge(
        val text: String,
        val color: String? = null
    ) : NanoNode()

    /**
     * Divider component
     */
    object Divider : NanoNode()

    // ============ Input Components ============

    /**
     * Button component
     * Example: `Button("Add to Cart", intent="primary"): on_click: ...`
     */
    data class Button(
        val label: String,
        val intent: String? = null,
        val icon: String? = null,
        val onClick: NanoAction? = null
    ) : NanoNode()

    /**
     * Input field
     * Example: `Input(value := state.email, placeholder="Enter email")`
     */
    data class Input(
        val value: Binding? = null,
        val placeholder: String? = null,
        val type: String? = null
    ) : NanoNode()

    /**
     * Checkbox component
     * Example: `Checkbox(checked := task.done)`
     */
    data class Checkbox(
        val checked: Binding? = null
    ) : NanoNode()

    // ============ Control Flow ============

    /**
     * Conditional rendering
     * Example: `if item.is_new: Badge("New")`
     */
    data class Conditional(
        val condition: String,
        val thenBranch: List<NanoNode>,
        val elseBranch: List<NanoNode>? = null
    ) : NanoNode()

    /**
     * List iteration
     * Example: `for task in state.tasks: ...`
     */
    data class ForLoop(
        val variable: String,
        val iterable: String,
        val body: List<NanoNode>
    ) : NanoNode()
}

