package cc.unitmesh.xuiper.spec.v1

import cc.unitmesh.xuiper.spec.*

/**
 * NanoDSL Specification Version 1.0
 * 
 * The initial specification optimized for current LLM capabilities.
 * Key design decisions:
 * - Python-style indentation (familiar to LLMs)
 * - Minimal syntax (reduce token usage)
 * - Explicit state bindings (<< and :=)
 */
object NanoSpecV1 : NanoSpec {
    override val version = "1.0"
    override val name = "NanoDSL-V1"

    // ==================== Component Definitions ====================
    
    private val SPACING_VALUES = listOf("xs", "sm", "md", "lg", "xl")
    private val STYLE_VALUES = listOf("h1", "h2", "h3", "h4", "body", "caption")
    private val INTENT_VALUES = listOf("primary", "secondary", "danger", "default")
    private val SHADOW_VALUES = listOf("none", "sm", "md", "lg")
    private val RADIUS_VALUES = listOf("none", "sm", "md", "lg", "full")
    private val ALIGN_VALUES = listOf("start", "center", "end", "stretch")
    private val JUSTIFY_VALUES = listOf("start", "center", "end", "between", "around")
    
    override val components: Map<String, ComponentSpec> = mapOf(
        // Layout
        "VStack" to ComponentSpec(
            name = "VStack",
            category = ComponentCategory.LAYOUT,
            optionalProps = listOf(
                PropSpec("spacing", PropType.ENUM, "md", allowedValues = SPACING_VALUES),
                PropSpec("align", PropType.ENUM, "stretch", allowedValues = ALIGN_VALUES)
            ),
            allowsChildren = true,
            description = "Vertical stack layout"
        ),
        "HStack" to ComponentSpec(
            name = "HStack",
            category = ComponentCategory.LAYOUT,
            optionalProps = listOf(
                PropSpec("spacing", PropType.ENUM, "md", allowedValues = SPACING_VALUES),
                PropSpec("align", PropType.ENUM, "center", allowedValues = ALIGN_VALUES),
                PropSpec("justify", PropType.ENUM, "start", allowedValues = JUSTIFY_VALUES)
            ),
            allowsChildren = true,
            description = "Horizontal stack layout"
        ),
        // Container
        "Card" to ComponentSpec(
            name = "Card",
            category = ComponentCategory.CONTAINER,
            optionalProps = listOf(
                PropSpec("padding", PropType.ENUM, "md", allowedValues = SPACING_VALUES),
                PropSpec("shadow", PropType.ENUM, "sm", allowedValues = SHADOW_VALUES)
            ),
            allowsChildren = true,
            description = "Card container with shadow"
        ),
        // Content
        "Text" to ComponentSpec(
            name = "Text",
            category = ComponentCategory.CONTENT,
            requiredProps = listOf(PropSpec("content", PropType.STRING)),
            optionalProps = listOf(
                PropSpec("style", PropType.ENUM, "body", allowedValues = STYLE_VALUES)
            ),
            description = "Text display component"
        ),
        "Image" to ComponentSpec(
            name = "Image",
            category = ComponentCategory.CONTENT,
            requiredProps = listOf(PropSpec("src", PropType.STRING)),
            optionalProps = listOf(
                PropSpec("aspect", PropType.STRING),
                PropSpec("radius", PropType.ENUM, "none", allowedValues = RADIUS_VALUES),
                PropSpec("width", PropType.INT)
            ),
            description = "Image display component"
        ),
        "Badge" to ComponentSpec(
            name = "Badge",
            category = ComponentCategory.CONTENT,
            requiredProps = listOf(PropSpec("text", PropType.STRING)),
            optionalProps = listOf(PropSpec("color", PropType.STRING)),
            description = "Badge/tag component"
        ),
        "Divider" to ComponentSpec(
            name = "Divider",
            category = ComponentCategory.CONTENT,
            description = "Horizontal divider line"
        ),
        // Input
        "Button" to ComponentSpec(
            name = "Button",
            category = ComponentCategory.INPUT,
            requiredProps = listOf(PropSpec("label", PropType.STRING)),
            optionalProps = listOf(
                PropSpec("intent", PropType.ENUM, "default", allowedValues = INTENT_VALUES),
                PropSpec("icon", PropType.STRING)
            ),
            allowsActions = true,
            description = "Clickable button"
        ),
        "Input" to ComponentSpec(
            name = "Input",
            category = ComponentCategory.INPUT,
            optionalProps = listOf(
                PropSpec("value", PropType.BINDING),
                PropSpec("placeholder", PropType.STRING),
                PropSpec("type", PropType.STRING, "text")
            ),
            description = "Text input field"
        ),
        "Checkbox" to ComponentSpec(
            name = "Checkbox",
            category = ComponentCategory.INPUT,
            optionalProps = listOf(PropSpec("checked", PropType.BINDING)),
            description = "Checkbox input"
        ),
        "TextArea" to ComponentSpec(
            name = "TextArea",
            category = ComponentCategory.INPUT,
            optionalProps = listOf(
                PropSpec("value", PropType.BINDING),
                PropSpec("placeholder", PropType.STRING),
                PropSpec("rows", PropType.INT, "4")
            ),
            description = "Multi-line text input"
        ),
        "Select" to ComponentSpec(
            name = "Select",
            category = ComponentCategory.INPUT,
            optionalProps = listOf(
                PropSpec("value", PropType.BINDING),
                PropSpec("options", PropType.STRING),
                PropSpec("placeholder", PropType.STRING)
            ),
            description = "Dropdown select input"
        ),
        // Form
        "Form" to ComponentSpec(
            name = "Form",
            category = ComponentCategory.CONTAINER,
            optionalProps = listOf(
                PropSpec("onSubmit", PropType.STRING)
            ),
            allowsChildren = true,
            allowsActions = true,
            description = "Form container with submit handling"
        )
    )

    override val layoutComponents = setOf("VStack", "HStack")
    override val containerComponents = setOf("Card", "Form")
    override val contentComponents = setOf("Text", "Image", "Badge", "Divider")
    override val inputComponents = setOf("Button", "Input", "Checkbox", "TextArea", "Select")
    override val controlFlowKeywords = setOf("if", "for", "state", "component", "request")
    override val actionTypes = setOf("Navigate", "Fetch", "ShowToast", "StateMutation")

    override val bindingOperators = listOf(
        BindingOperatorSpec("<<", "Subscribe", "One-way binding (read-only)", isOneWay = true),
        BindingOperatorSpec(":=", "TwoWay", "Two-way binding (read-write)", isOneWay = false)
    )

    override fun getComponent(name: String) = components[name]
    override fun isValidComponent(name: String) = name in components
    override fun isReservedKeyword(keyword: String) = keyword in controlFlowKeywords
}

