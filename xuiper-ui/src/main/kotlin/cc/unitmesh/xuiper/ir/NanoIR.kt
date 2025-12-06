package cc.unitmesh.xuiper.ir

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * NanoIR - JSON Intermediate Representation
 * 
 * Platform-agnostic representation of NanoDSL components.
 * This is the format sent to renderers (Compose, React, Flutter, etc.)
 * 
 * Example output:
 * ```json
 * {
 *   "type": "Card",
 *   "props": { "padding": "md", "shadow": "sm" },
 *   "children": [
 *     { "type": "Text", "props": { "content": "Hello" } }
 *   ]
 * }
 * ```
 */
@Serializable
data class NanoIR(
    /** Component type (VStack, Card, Text, Button, etc.) */
    val type: String,
    
    /** Component properties as JSON elements */
    val props: Map<String, JsonElement> = emptyMap(),
    
    /** Child components */
    val children: List<NanoIR>? = null,
    
    /** State definitions (only for Component type) */
    val state: NanoStateIR? = null,
    
    /** Actions associated with this component */
    val actions: Map<String, NanoActionIR>? = null,
    
    /** Binding information */
    val bindings: Map<String, NanoBindingIR>? = null,
    
    /** Condition for conditional rendering */
    val condition: String? = null,
    
    /** Loop information for list rendering */
    val loop: NanoLoopIR? = null
) {
    companion object {
        fun text(content: String, style: String? = null): NanoIR {
            val props = mutableMapOf<String, JsonElement>(
                "content" to JsonPrimitive(content)
            )
            style?.let { props["style"] = JsonPrimitive(it) }
            return NanoIR(type = "Text", props = props)
        }

        fun vstack(spacing: String? = null, children: List<NanoIR>): NanoIR {
            val props = mutableMapOf<String, JsonElement>()
            spacing?.let { props["spacing"] = JsonPrimitive(it) }
            return NanoIR(type = "VStack", props = props, children = children)
        }

        fun hstack(
            spacing: String? = null,
            align: String? = null,
            justify: String? = null,
            children: List<NanoIR>
        ): NanoIR {
            val props = mutableMapOf<String, JsonElement>()
            spacing?.let { props["spacing"] = JsonPrimitive(it) }
            align?.let { props["align"] = JsonPrimitive(it) }
            justify?.let { props["justify"] = JsonPrimitive(it) }
            return NanoIR(type = "HStack", props = props, children = children)
        }

        fun card(padding: String? = null, shadow: String? = null, children: List<NanoIR>): NanoIR {
            val props = mutableMapOf<String, JsonElement>()
            padding?.let { props["padding"] = JsonPrimitive(it) }
            shadow?.let { props["shadow"] = JsonPrimitive(it) }
            return NanoIR(type = "Card", props = props, children = children)
        }

        fun button(label: String, intent: String? = null): NanoIR {
            val props = mutableMapOf<String, JsonElement>(
                "label" to JsonPrimitive(label)
            )
            intent?.let { props["intent"] = JsonPrimitive(it) }
            return NanoIR(type = "Button", props = props)
        }
    }
}

/**
 * State definition in IR format
 */
@Serializable
data class NanoStateIR(
    val variables: Map<String, NanoStateVarIR>
)

/**
 * Single state variable
 */
@Serializable
data class NanoStateVarIR(
    val type: String,
    val defaultValue: JsonElement? = null
)

/**
 * Action definition in IR format
 */
@Serializable
data class NanoActionIR(
    val type: String,
    val payload: Map<String, JsonElement>? = null
)

/**
 * Binding definition in IR format
 */
@Serializable
data class NanoBindingIR(
    /** "subscribe" for << or "twoWay" for := */
    val mode: String,
    val expression: String
)

/**
 * Loop definition for list rendering
 */
@Serializable
data class NanoLoopIR(
    val variable: String,
    val iterable: String
)

