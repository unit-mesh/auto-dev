package cc.unitmesh.xuiper.ast

import cc.unitmesh.xuiper.action.BodyField
import cc.unitmesh.xuiper.action.ContentType
import cc.unitmesh.xuiper.action.HttpMethod
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

    // ============ HTTP Request Components ============

    /**
     * Declarative HTTP request definition
     *
     * Example:
     * ```nanodsl
     * request login:
     *     url: "/api/login"
     *     method: "POST"
     *     body:
     *         email << state.email
     *         password << state.password
     *     headers:
     *         Content-Type: "application/json"
     *     on_success:
     *         Navigate(to="/dashboard")
     *     on_error:
     *         ShowToast("Login failed")
     * ```
     */
    data class HttpRequest(
        /** Request name (used to invoke: request.login()) */
        val name: String,

        /** Request URL */
        val url: String,

        /** HTTP method */
        val method: HttpMethod = HttpMethod.GET,

        /** Request body fields with bindings */
        val body: List<RequestBodyField> = emptyList(),

        /** Request headers */
        val headers: Map<String, String> = emptyMap(),

        /** Query parameters */
        val params: Map<String, String> = emptyMap(),

        /** Content type */
        val contentType: ContentType = ContentType.JSON,

        /** Actions on loading start */
        val onLoading: NanoAction? = null,

        /** Actions on success */
        val onSuccess: NanoAction? = null,

        /** Actions on error */
        val onError: NanoAction? = null,

        /** State path to bind loading status */
        val loadingBinding: Binding? = null,

        /** State path to bind response data */
        val responseBinding: Binding? = null,

        /** State path to bind error message */
        val errorBinding: Binding? = null
    ) : NanoNode()

    /**
     * Request body field with optional binding
     */
    data class RequestBodyField(
        /** Field name in the request body */
        val name: String,

        /** Field value - either literal or state binding */
        val value: BodyField
    )

    // ============ Form Components ============

    /**
     * Form container that groups inputs and provides submit handling
     *
     * Example:
     * ```nanodsl
     * Form(onSubmit=request.login):
     *     Input(name="email", value := state.email)
     *     Input(name="password", type="password", value := state.password)
     *     Button("Submit", type="submit")
     * ```
     */
    data class Form(
        /** Request to invoke on submit */
        val onSubmit: String? = null,

        /** Direct Fetch action on submit */
        val onSubmitAction: NanoAction? = null,

        /** Child components (inputs, buttons) */
        val children: List<NanoNode> = emptyList()
    ) : NanoNode()

    /**
     * Select/Dropdown component
     * Example: `Select(value := state.country, options=countries)`
     */
    data class Select(
        /** Two-way binding to state */
        val value: Binding? = null,

        /** Options list - can be literal or state binding */
        val options: String? = null,

        /** Placeholder text */
        val placeholder: String? = null
    ) : NanoNode()

    /**
     * TextArea component for multi-line input
     * Example: `TextArea(value := state.bio, rows=4)`
     */
    data class TextArea(
        /** Two-way binding to state */
        val value: Binding? = null,

        /** Number of rows */
        val rows: Int? = null,

        /** Placeholder text */
        val placeholder: String? = null
    ) : NanoNode()
}
