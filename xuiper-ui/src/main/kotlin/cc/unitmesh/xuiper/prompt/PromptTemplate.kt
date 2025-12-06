package cc.unitmesh.xuiper.prompt

import kotlinx.serialization.Serializable

@Serializable
data class PromptTemplate(
    val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val userPromptTemplate: String,
    val examples: List<PromptExample> = emptyList(),
    val outputFormat: String = "nanodsl"
) {
    fun render(variables: Map<String, String>): RenderedPrompt {
        var system = systemPrompt
        var user = userPromptTemplate
        
        variables.forEach { (key, value) ->
            system = system.replace("{{$key}}", value)
            user = user.replace("{{$key}}", value)
        }
        
        if (examples.isNotEmpty()) {
            val examplesText = examples.joinToString("\n\n") { example ->
                """
                |Example - ${example.name}:
                |User: ${example.userPrompt}
                |
                |```nanodsl
                |${example.expectedOutput}
                |```
                """.trimMargin()
            }
            system = "$system\n\n## Examples\n\n$examplesText"
        }
        
        return RenderedPrompt(system, user)
    }
}

@Serializable
data class PromptExample(
    val name: String,
    val userPrompt: String,
    val expectedOutput: String
)

data class RenderedPrompt(
    val systemPrompt: String,
    val userPrompt: String
)

object PromptTemplateRegistry {
    private val templates = mutableMapOf<String, PromptTemplate>()

    init {
        loadFromResources()
    }

    private fun loadFromResources() {
        val standardPrompt = loadPromptResource("prompts/standard.txt")
        register(PromptTemplate(
            id = "default",
            name = "Standard NanoDSL",
            description = "Balanced prompt for general NanoDSL generation",
            systemPrompt = standardPrompt,
            userPromptTemplate = "{{user_prompt}}",
            examples = listOf(
                PromptExample(
                    name = "Simple Card",
                    userPrompt = "Create a simple greeting card with a title and message",
                    expectedOutput = SIMPLE_CARD_EXAMPLE
                )
            )
        ))

        val minimalPrompt = loadPromptResource("prompts/minimal.txt")
        register(PromptTemplate(
            id = "minimal",
            name = "Minimal NanoDSL",
            description = "Minimal prompt for token efficiency testing",
            systemPrompt = minimalPrompt,
            userPromptTemplate = "{{user_prompt}}"
        ))

        val detailedPrompt = loadPromptResource("prompts/detailed.txt")
        val productCardExample = loadPromptResource("prompts/examples/product-card.nanodsl")
        val counterExample = loadPromptResource("prompts/examples/counter.nanodsl")
        register(PromptTemplate(
            id = "detailed",
            name = "Detailed NanoDSL",
            description = "Comprehensive prompt with full syntax reference",
            systemPrompt = detailedPrompt,
            userPromptTemplate = "{{user_prompt}}",
            examples = listOf(
                PromptExample(
                    name = "Product Card",
                    userPrompt = "Create a product card showing image, title, price, and add to cart button",
                    expectedOutput = productCardExample
                ),
                PromptExample(
                    name = "Counter",
                    userPrompt = "Create a counter with increment and decrement buttons",
                    expectedOutput = counterExample
                )
            )
        ))
    }

    private fun loadPromptResource(path: String): String {
        return PromptTemplateRegistry::class.java.classLoader
            ?.getResourceAsStream(path)
            ?.bufferedReader()
            ?.readText()
            ?: throw IllegalStateException("Cannot load prompt resource: $path")
    }

    fun register(template: PromptTemplate) {
        templates[template.id] = template
    }

    fun get(id: String): PromptTemplate = templates[id]
        ?: throw IllegalArgumentException("Unknown prompt template: $id")

    fun list(): List<PromptTemplate> = templates.values.toList()

    fun reload() {
        templates.clear()
        loadFromResources()
    }
}

private const val SIMPLE_CARD_EXAMPLE = """component GreetingCard:
    Card:
        padding: "md"
        content:
            VStack(spacing="sm"):
                Text("Hello!", style="h2")
                Text("Welcome to NanoDSL", style="body")"""
