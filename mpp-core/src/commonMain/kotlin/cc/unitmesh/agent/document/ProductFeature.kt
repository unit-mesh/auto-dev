package cc.unitmesh.agent.document

import kotlinx.serialization.Serializable
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Product Feature - represents a node in the product feature tree
 */
@Serializable
data class ProductFeature(
    val id: String = generateFeatureId(),
    val name: String,
    val description: String,
    val level: FeatureLevel,
    val confidence: Float = 1.0f,
    val children: MutableList<ProductFeature> = mutableListOf(),
    val codeRefs: MutableList<CodeRef> = mutableListOf(),
    val status: FeatureStatus = FeatureStatus.PENDING
) {
    companion object {
        @OptIn(ExperimentalAtomicApi::class)
        private val counter = AtomicInt(0)

        @OptIn(ExperimentalAtomicApi::class)
        fun generateFeatureId(): String = "feature-${counter.addAndFetch(1)}"
    }
}

/**
 * Feature Level - represents the hierarchy level in the feature tree
 */
@Serializable
enum class FeatureLevel {
    PRODUCT,   // Product level (root node)
    MODULE,    // Module level (e.g., "Payment System")
    FEATURE,   // Feature level (e.g., "Order Payment")
    ATOMIC     // Atomic function level (e.g., "Validate Payment Amount")
}

/**
 * Feature Status - represents the analysis status of a feature
 */
@Serializable
enum class FeatureStatus {
    PENDING,       // Not yet analyzed
    ANALYZING,     // Currently being analyzed
    CONFIRMED,     // Analysis complete, confirmed
    NEEDS_REVIEW   // Needs human review (low confidence)
}

/**
 * Code Reference - links a feature to its source code
 */
@Serializable
data class CodeRef(
    val filePath: String,
    val className: String? = null,
    val methods: List<String> = emptyList(),
    val inferredFrom: InferenceSource = InferenceSource.FILENAME
)

/**
 * Inference Source - indicates how a feature was inferred
 */
@Serializable
enum class InferenceSource {
    FILENAME,          // Inferred from file name
    CLASS_DEFINITION,  // Inferred from class definition/comments
    METHOD_SIGNATURE,  // Inferred from method signatures
    CODE_ANALYSIS,     // Inferred from code content analysis
    DIRECTORY_STRUCTURE // Inferred from directory structure
}

/**
 * Output format for the feature tree
 */
enum class FeatureTreeOutputFormat {
    MERMAID_MINDMAP,   // Mermaid mindmap format
    PLANTUML_MINDMAP,  // PlantUML mindmap format
    DOT_GRAPH,         // Graphviz DOT format
    MARKDOWN_LIST,     // Markdown nested list
    JSON               // JSON tree structure
}

/**
 * Renders feature tree to various output formats
 */
object FeatureTreeRenderer {
    
    fun render(root: ProductFeature, format: FeatureTreeOutputFormat): String {
        return when (format) {
            FeatureTreeOutputFormat.MERMAID_MINDMAP -> renderMermaidMindmap(root)
            FeatureTreeOutputFormat.PLANTUML_MINDMAP -> renderPlantUmlMindmap(root)
            FeatureTreeOutputFormat.DOT_GRAPH -> renderDotGraph(root)
            FeatureTreeOutputFormat.MARKDOWN_LIST -> renderMarkdownList(root)
            FeatureTreeOutputFormat.JSON -> renderJson(root)
        }
    }
    
    private fun renderMermaidMindmap(root: ProductFeature): String {
        return buildString {
            appendLine("```mermaid")
            appendLine("mindmap")
            appendLine("  root((${root.name}))")
            renderMermaidChildren(root.children, "    ", this)
            appendLine("```")
        }
    }
    
    private fun renderMermaidChildren(children: List<ProductFeature>, indent: String, sb: StringBuilder) {
        for (child in children) {
            val marker = if (child.confidence < 0.7) "?" else ""
            sb.appendLine("$indent${child.name}$marker")
            renderMermaidChildren(child.children, "$indent  ", sb)
        }
    }
    
    private fun renderPlantUmlMindmap(root: ProductFeature): String {
        return buildString {
            appendLine("@startmindmap")
            appendLine("* ${root.name}")
            renderPlantUmlChildren(root.children, 2, this)
            appendLine("@endmindmap")
        }
    }
    
    private fun renderPlantUmlChildren(children: List<ProductFeature>, level: Int, sb: StringBuilder) {
        val marker = "*".repeat(level)
        for (child in children) {
            val confidence = if (child.confidence < 0.7) " [?]" else ""
            sb.appendLine("$marker ${child.name}$confidence")
            renderPlantUmlChildren(child.children, level + 1, sb)
        }
    }
    
    private fun renderDotGraph(root: ProductFeature): String {
        return buildString {
            appendLine("digraph FeatureTree {")
            appendLine("  rankdir=TB;")
            appendLine("  node [shape=box];")
            appendLine("  \"${root.id}\" [label=\"${root.name}\", style=filled, fillcolor=lightblue];")
            renderDotChildren(root, this)
            appendLine("}")
        }
    }
    
    private fun renderDotChildren(parent: ProductFeature, sb: StringBuilder) {
        for (child in parent.children) {
            val color = if (child.confidence < 0.7) "lightyellow" else "white"
            sb.appendLine("  \"${child.id}\" [label=\"${child.name}\", style=filled, fillcolor=$color];")
            sb.appendLine("  \"${parent.id}\" -> \"${child.id}\";")
            renderDotChildren(child, sb)
        }
    }
    
    private fun renderMarkdownList(root: ProductFeature): String {
        return buildString {
            appendLine("# ${root.name}")
            appendLine()
            appendLine(root.description)
            appendLine()
            renderMarkdownChildren(root.children, 0, this)
        }
    }
    
    private fun renderMarkdownChildren(children: List<ProductFeature>, level: Int, sb: StringBuilder) {
        val indent = "  ".repeat(level)
        for (child in children) {
            val marker = if (child.confidence < 0.7) " ⚠️" else ""
            sb.appendLine("$indent- **${child.name}**$marker: ${child.description}")
            if (child.codeRefs.isNotEmpty()) {
                sb.appendLine("$indent  - 📁 ${child.codeRefs.joinToString(", ") { it.filePath }}")
            }
            renderMarkdownChildren(child.children, level + 1, sb)
        }
    }
    
    private fun renderJson(root: ProductFeature): String {
        return kotlinx.serialization.json.Json { prettyPrint = true }.encodeToString(
            ProductFeature.serializer(), root
        )
    }
}

