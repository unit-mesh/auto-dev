package cc.unitmesh.xuiper.action

/**
 * NanoDSL Action System
 * 
 * Defines all possible actions that can be triggered by user interactions.
 * Actions are limited to a predefined set for security (prevents arbitrary code execution).
 * 
 * Reference: DivKit Action Protocol
 */
sealed class NanoAction {
    /**
     * State mutation action
     * Example: `on_click: state.count += 1`
     */
    data class StateMutation(
        val path: String,
        val operation: MutationOp,
        val value: String
    ) : NanoAction()

    /**
     * Navigation action with enhanced routing support
     *
     * Examples:
     * ```nanodsl
     * # Simple navigation
     * Navigate(to="/cart")
     *
     * # Navigation with route params
     * Navigate(to="/user/{id}", params={"id": state.userId})
     *
     * # Navigation with query string
     * Navigate(to="/search", query={"q": state.query, "page": "1"})
     *
     * # Replace current history entry (no back navigation)
     * Navigate(to="/login", replace=true)
     *
     * # Full form
     * Navigate(
     *     to="/product/{id}",
     *     params={"id": "123"},
     *     query={"ref": "home"},
     *     replace=false
     * )
     * ```
     */
    data class Navigate(
        /** Target path (required) - can include path params like /user/{id} */
        val to: String,

        /** Route parameters to substitute in path - e.g., {"id": "123"} for /user/{id} */
        val params: Map<String, String>? = null,

        /** Query parameters to append to URL - e.g., {"q": "search"} -> ?q=search */
        val query: Map<String, String>? = null,

        /** Replace current history entry instead of pushing new one */
        val replace: Boolean = false
    ) : NanoAction() {
        /**
         * Build the final URL with params and query string substituted
         */
        fun buildUrl(): String {
            var url = to

            // Substitute path params
            params?.forEach { (key, value) ->
                url = url.replace("{$key}", value)
                url = url.replace(":$key", value)
            }

            // Append query string
            if (!query.isNullOrEmpty()) {
                val queryString = query.entries.joinToString("&") { (k, v) ->
                    "${encodeURIComponent(k)}=${encodeURIComponent(v)}"
                }
                url = if (url.contains("?")) "$url&$queryString" else "$url?$queryString"
            }

            return url
        }

        private fun encodeURIComponent(value: String): String {
            return java.net.URLEncoder.encode(value, "UTF-8")
                .replace("+", "%20")
        }
    }

    /**
     * Network fetch action with comprehensive HTTP request support
     *
     * Examples:
     * ```nanodsl
     * # Simple GET
     * Fetch(url="/api/users")
     *
     * # POST with body
     * Fetch(url="/api/login", method="POST", body={"email": state.email})
     *
     * # Full form with callbacks
     * Fetch(
     *     url="/api/users",
     *     method="POST",
     *     body={"name": state.name, "email": state.email},
     *     headers={"Authorization": "Bearer token"},
     *     on_success: Navigate(to="/success"),
     *     on_error: ShowToast("Request failed")
     * )
     * ```
     */
    data class Fetch(
        /** Request URL (required) */
        val url: String,

        /** HTTP method: GET, POST, PUT, PATCH, DELETE */
        val method: HttpMethod = HttpMethod.GET,

        /** Request body - supports state bindings */
        val body: Map<String, BodyField>? = null,

        /** Request headers */
        val headers: Map<String, String>? = null,

        /** Query parameters */
        val params: Map<String, String>? = null,

        /** Content type for the request body */
        val contentType: ContentType = ContentType.JSON,

        /** Action to execute on successful response */
        val onSuccess: NanoAction? = null,

        /** Action to execute on error */
        val onError: NanoAction? = null,

        /** State path to bind loading state */
        val loadingState: String? = null,

        /** State path to bind response data */
        val responseBinding: String? = null,

        /** State path to bind error message */
        val errorBinding: String? = null
    ) : NanoAction() {
        // Legacy constructor for backward compatibility
        constructor(url: String, method: String, body: Map<String, String>?) : this(
            url = url,
            method = HttpMethod.valueOf(method.uppercase()),
            body = body?.mapValues { BodyField.Literal(it.value) }
        )
    }

    /**
     * Toast notification
     * Example: `on_click: ShowToast("Added to cart")`
     */
    data class ShowToast(
        val message: String
    ) : NanoAction()

    /**
     * Compound action (multiple actions in sequence)
     */
    data class Sequence(
        val actions: List<NanoAction>
    ) : NanoAction()
}

/**
 * State mutation operations
 */
enum class MutationOp {
    SET,        // state.x = value
    ADD,        // state.x += value
    SUBTRACT,   // state.x -= value
    APPEND,     // state.list.append(value)
    REMOVE      // state.list.remove(value)
}

/**
 * HTTP methods supported by Fetch action
 */
enum class HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS
}

/**
 * Content types for HTTP request body
 */
enum class ContentType(val mimeType: String) {
    JSON("application/json"),
    FORM_URLENCODED("application/x-www-form-urlencoded"),
    FORM_DATA("multipart/form-data"),
    TEXT("text/plain"),
    XML("application/xml")
}

/**
 * Body field value - can be literal string or state binding
 *
 * Examples:
 * - Literal: `"email": "test@example.com"`
 * - Binding: `"email": state.email`
 */
sealed class BodyField {
    /**
     * Literal string value
     */
    data class Literal(val value: String) : BodyField()

    /**
     * State binding - value comes from state path
     * Example: `state.email` -> StateBinding("state.email")
     */
    data class StateBinding(val path: String) : BodyField()

    companion object {
        /**
         * Parse a body field from DSL string
         */
        fun parse(value: String): BodyField {
            return if (value.startsWith("state.")) {
                StateBinding(value)
            } else {
                Literal(value.trim('"'))
            }
        }
    }
}
