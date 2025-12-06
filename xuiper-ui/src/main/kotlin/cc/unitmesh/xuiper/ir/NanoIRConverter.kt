package cc.unitmesh.xuiper.ir

import cc.unitmesh.xuiper.action.NanoAction
import cc.unitmesh.xuiper.dsl.Binding
import cc.unitmesh.xuiper.dsl.NanoNode
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Converts NanoNode AST to NanoIR (JSON Intermediate Representation)
 */
object NanoIRConverter {

    /**
     * Convert a NanoNode to NanoIR
     */
    fun convert(node: NanoNode): NanoIR {
        return when (node) {
            is NanoNode.Component -> convertComponent(node)
            is NanoNode.VStack -> convertVStack(node)
            is NanoNode.HStack -> convertHStack(node)
            is NanoNode.Card -> convertCard(node)
            is NanoNode.Text -> convertText(node)
            is NanoNode.Image -> convertImage(node)
            is NanoNode.Badge -> convertBadge(node)
            is NanoNode.Button -> convertButton(node)
            is NanoNode.Input -> convertInput(node)
            is NanoNode.Checkbox -> convertCheckbox(node)
            is NanoNode.Conditional -> convertConditional(node)
            is NanoNode.ForLoop -> convertForLoop(node)
            NanoNode.Divider -> NanoIR(type = "Divider")
        }
    }

    private fun convertComponent(node: NanoNode.Component): NanoIR {
        val props = mutableMapOf<String, JsonElement>(
            "name" to JsonPrimitive(node.name)
        )

        val state = node.state?.let { stateBlock ->
            val vars = stateBlock.variables.associate { v ->
                v.name to NanoStateVarIR(
                    type = v.type,
                    defaultValue = v.defaultValue?.let { JsonPrimitive(it) }
                )
            }
            NanoStateIR(vars)
        }

        return NanoIR(
            type = "Component",
            props = props,
            children = node.children.map { convert(it) },
            state = state
        )
    }

    private fun convertVStack(node: NanoNode.VStack): NanoIR {
        val props = mutableMapOf<String, JsonElement>()
        node.spacing?.let { props["spacing"] = JsonPrimitive(it) }
        node.align?.let { props["align"] = JsonPrimitive(it) }

        return NanoIR(
            type = "VStack",
            props = props,
            children = node.children.map { convert(it) }
        )
    }

    private fun convertHStack(node: NanoNode.HStack): NanoIR {
        val props = mutableMapOf<String, JsonElement>()
        node.spacing?.let { props["spacing"] = JsonPrimitive(it) }
        node.align?.let { props["align"] = JsonPrimitive(it) }
        node.justify?.let { props["justify"] = JsonPrimitive(it) }

        return NanoIR(
            type = "HStack",
            props = props,
            children = node.children.map { convert(it) }
        )
    }

    private fun convertCard(node: NanoNode.Card): NanoIR {
        val props = mutableMapOf<String, JsonElement>()
        node.padding?.let { props["padding"] = JsonPrimitive(it) }
        node.shadow?.let { props["shadow"] = JsonPrimitive(it) }

        return NanoIR(
            type = "Card",
            props = props,
            children = node.children.map { convert(it) }
        )
    }

    private fun convertText(node: NanoNode.Text): NanoIR {
        val props = mutableMapOf<String, JsonElement>(
            "content" to JsonPrimitive(node.content)
        )
        node.style?.let { props["style"] = JsonPrimitive(it) }

        val bindings = node.binding?.let {
            mapOf("content" to convertBinding(it))
        }

        return NanoIR(type = "Text", props = props, bindings = bindings)
    }

    private fun convertImage(node: NanoNode.Image): NanoIR {
        val props = mutableMapOf<String, JsonElement>(
            "src" to JsonPrimitive(node.src)
        )
        node.aspect?.let { props["aspect"] = JsonPrimitive(it) }
        node.radius?.let { props["radius"] = JsonPrimitive(it) }
        node.width?.let { props["width"] = JsonPrimitive(it) }

        return NanoIR(type = "Image", props = props)
    }

    private fun convertBadge(node: NanoNode.Badge): NanoIR {
        val props = mutableMapOf<String, JsonElement>(
            "text" to JsonPrimitive(node.text)
        )
        node.color?.let { props["color"] = JsonPrimitive(it) }

        return NanoIR(type = "Badge", props = props)
    }

    private fun convertButton(node: NanoNode.Button): NanoIR {
        val props = mutableMapOf<String, JsonElement>(
            "label" to JsonPrimitive(node.label)
        )
        node.intent?.let { props["intent"] = JsonPrimitive(it) }
        node.icon?.let { props["icon"] = JsonPrimitive(it) }

        val actions = node.onClick?.let {
            mapOf("onClick" to convertAction(it))
        }

        return NanoIR(type = "Button", props = props, actions = actions)
    }

    private fun convertInput(node: NanoNode.Input): NanoIR {
        val props = mutableMapOf<String, JsonElement>()
        node.placeholder?.let { props["placeholder"] = JsonPrimitive(it) }
        node.type?.let { props["type"] = JsonPrimitive(it) }

        val bindings = node.value?.let {
            mapOf("value" to convertBinding(it))
        }

        return NanoIR(type = "Input", props = props, bindings = bindings)
    }

    private fun convertCheckbox(node: NanoNode.Checkbox): NanoIR {
        val bindings = node.checked?.let {
            mapOf("checked" to convertBinding(it))
        }

        return NanoIR(type = "Checkbox", bindings = bindings)
    }

    private fun convertConditional(node: NanoNode.Conditional): NanoIR {
        val children = node.thenBranch.map { convert(it) }

        return NanoIR(
            type = "Conditional",
            condition = node.condition,
            children = children
        )
    }

    private fun convertForLoop(node: NanoNode.ForLoop): NanoIR {
        val children = node.body.map { convert(it) }

        return NanoIR(
            type = "ForLoop",
            loop = NanoLoopIR(
                variable = node.variable,
                iterable = node.iterable
            ),
            children = children
        )
    }

    private fun convertBinding(binding: Binding): NanoBindingIR {
        return when (binding) {
            is Binding.Subscribe -> NanoBindingIR(
                mode = "subscribe",
                expression = binding.expression
            )
            is Binding.TwoWay -> NanoBindingIR(
                mode = "twoWay",
                expression = binding.path
            )
            is Binding.Static -> NanoBindingIR(
                mode = "static",
                expression = binding.value
            )
        }
    }

    private fun convertAction(action: NanoAction): NanoActionIR {
        return when (action) {
            is NanoAction.StateMutation -> NanoActionIR(
                type = "stateMutation",
                payload = mapOf(
                    "path" to JsonPrimitive(action.path),
                    "operation" to JsonPrimitive(action.operation.name),
                    "value" to JsonPrimitive(action.value)
                )
            )
            is NanoAction.Navigate -> NanoActionIR(
                type = "navigate",
                payload = mapOf("to" to JsonPrimitive(action.to))
            )
            is NanoAction.Fetch -> NanoActionIR(
                type = "fetch",
                payload = mapOf(
                    "url" to JsonPrimitive(action.url),
                    "method" to JsonPrimitive(action.method)
                )
            )
            is NanoAction.ShowToast -> NanoActionIR(
                type = "showToast",
                payload = mapOf("message" to JsonPrimitive(action.message))
            )
            is NanoAction.Sequence -> NanoActionIR(
                type = "sequence"
            )
        }
    }
}

