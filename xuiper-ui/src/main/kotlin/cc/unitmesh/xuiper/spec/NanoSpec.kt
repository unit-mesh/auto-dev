package cc.unitmesh.xuiper.spec

/**
 * NanoSpec - DSL Language Specification
 * 
 * Defines the grammar and component rules for NanoDSL.
 * Versioned to support evolution as LLM capabilities change.
 * 
 * When LLM capabilities improve, you can:
 * 1. Create a new version (v2, v3, ...)
 * 2. Add new components or syntax rules
 * 3. Deprecate old patterns
 * 4. Keep backward compatibility via version selection
 */
interface NanoSpec {
    /** Specification version */
    val version: String
    
    /** Specification name */
    val name: String
    
    /** All component specifications */
    val components: Map<String, ComponentSpec>
    
    /** Layout components (VStack, HStack, etc.) */
    val layoutComponents: Set<String>
    
    /** Container components (Card, etc.) */
    val containerComponents: Set<String>
    
    /** Content components (Text, Image, Badge, etc.) */
    val contentComponents: Set<String>
    
    /** Input components (Button, Input, Checkbox, etc.) */
    val inputComponents: Set<String>
    
    /** Control flow keywords */
    val controlFlowKeywords: Set<String>
    
    /** State binding operators */
    val bindingOperators: List<BindingOperatorSpec>
    
    /** Action types supported */
    val actionTypes: Set<String>
    
    /** Get component spec by name */
    fun getComponent(name: String): ComponentSpec?
    
    /** Check if a component name is valid */
    fun isValidComponent(name: String): Boolean
    
    /** Check if a keyword is reserved */
    fun isReservedKeyword(keyword: String): Boolean
}

/**
 * Component specification - defines a single component's structure
 */
data class ComponentSpec(
    /** Component name */
    val name: String,
    
    /** Component category */
    val category: ComponentCategory,
    
    /** Required properties */
    val requiredProps: List<PropSpec> = emptyList(),
    
    /** Optional properties */
    val optionalProps: List<PropSpec> = emptyList(),
    
    /** Whether this component can have children */
    val allowsChildren: Boolean = false,
    
    /** Whether this component supports actions (on_click, etc.) */
    val allowsActions: Boolean = false,
    
    /** Description for documentation/prompts */
    val description: String = ""
)

/**
 * Component categories
 */
enum class ComponentCategory {
    LAYOUT,
    CONTAINER,
    CONTENT,
    INPUT,
    CONTROL_FLOW
}

/**
 * Property specification
 */
data class PropSpec(
    val name: String,
    val type: PropType,
    val defaultValue: String? = null,
    val description: String = "",
    /** Valid values for enum-like props */
    val allowedValues: List<String>? = null
)

/**
 * Property types
 */
enum class PropType {
    STRING,
    INT,
    FLOAT,
    BOOLEAN,
    BINDING,        // State binding (<<, :=)
    EXPRESSION,     // Expression like f"..."
    ENUM            // Fixed set of values (sm, md, lg)
}

/**
 * Binding operator specification
 */
data class BindingOperatorSpec(
    val symbol: String,
    val name: String,
    val description: String,
    val isOneWay: Boolean
)

