package cc.unitmesh.xuiper.parser

import cc.unitmesh.xuiper.action.MutationOp
import cc.unitmesh.xuiper.action.NanoAction
import cc.unitmesh.xuiper.ast.Binding
import cc.unitmesh.xuiper.ast.NanoNode
import cc.unitmesh.xuiper.spec.NanoSpec
import cc.unitmesh.xuiper.spec.v1.NanoSpecV1

/**
 * IndentParser - Python-style indentation-based parser
 *
 * This is the default parser implementation for NanoDSL.
 * Uses indentation to determine hierarchy (like Python).
 *
 * Example input:
 * ```
 * component GreetingCard:
 *     Card:
 *         padding: "md"
 *         content:
 *             VStack(spacing="sm"):
 *                 Text("Hello!", style="h2")
 * ```
 *
 * To modify parsing behavior for new LLM capabilities:
 * 1. Override methods in this class
 * 2. Or create a new parser implementing NanoParser interface
 */
class IndentParser(
    override val spec: NanoSpec = NanoSpecV1
) : NanoParser {
    
    companion object {
        private val COMPONENT_REGEX = Regex("""^component\s+(\w+)(?:\((.*?)\))?:\s*$""")
        private val STATE_BLOCK_REGEX = Regex("""^\s*state:\s*$""")
        private val STATE_VAR_REGEX = Regex("""^\s*(\w+):\s*(\w+)\s*=\s*(.+)$""")
        private val COMPONENT_CALL_REGEX = Regex("""^(\w+)(?:\((.*?)\))?:\s*$""")
        private val COMPONENT_INLINE_REGEX = Regex("""^(\w+)(?:\((.*?)\))$""")
        private val PROP_REGEX = Regex("""^(\w+):\s*(.+)$""")
        private val IF_REGEX = Regex("""^if\s+(.+?):\s*$""")
        private val FOR_REGEX = Regex("""^for\s+(\w+)\s+in\s+(.+?):\s*$""")
        private val ON_CLICK_REGEX = Regex("""^on_click:\s*(.+)$""")
    }

    /**
     * Parse NanoDSL source code into AST (NanoParser interface)
     */
    override fun parse(source: String): ParseResult {
        return try {
            val lines = source.lines()
            val (component, _) = parseComponent(lines, 0)
            ParseResult.Success(component)
        } catch (e: Exception) {
            ParseResult.Failure(listOf(ParseError(e.message ?: "Unknown error", 0)))
        }
    }

    /**
     * Validate source without full parsing
     */
    override fun validate(source: String): ValidationResult {
        val errors = mutableListOf<ParseError>()
        val warnings = mutableListOf<ParseWarning>()

        val lines = source.lines()

        // Check basic structure
        if (lines.isEmpty() || lines.all { it.isBlank() }) {
            errors.add(ParseError("Empty source", 0))
            return ValidationResult(false, errors, warnings)
        }

        // Check component definition
        val firstNonBlank = lines.indexOfFirst { it.isNotBlank() }
        if (firstNonBlank >= 0) {
            val firstLine = lines[firstNonBlank].trim()
            if (!COMPONENT_REGEX.matches(firstLine)) {
                // Check if it's a bare component without 'component' keyword
                if (COMPONENT_CALL_REGEX.matches(firstLine)) {
                    warnings.add(ParseWarning(
                        "Missing 'component' keyword",
                        firstNonBlank + 1,
                        "Add 'component ComponentName:' at the start"
                    ))
                } else {
                    errors.add(ParseError("Invalid component definition", firstNonBlank + 1))
                }
            }
        }

        // Check indentation consistency
        var prevIndent = 0
        lines.forEachIndexed { index, line ->
            if (line.isNotBlank()) {
                val indent = line.takeWhile { it == ' ' }.length
                if (indent % 4 != 0) {
                    warnings.add(ParseWarning(
                        "Inconsistent indentation (expected multiple of 4)",
                        index + 1,
                        "Use 4 spaces per indentation level"
                    ))
                }
                prevIndent = indent
            }
        }

        return ValidationResult(errors.isEmpty(), errors, warnings)
    }

    /**
     * Legacy parse method for backward compatibility
     */
    fun parseToAst(source: String): NanoNode.Component {
        val lines = source.lines()
        val (component, _) = parseComponent(lines, 0)
        return component
    }

    private fun parseComponent(lines: List<String>, startIndex: Int): Pair<NanoNode.Component, Int> {
        val firstLine = lines[startIndex].trim()
        val match = COMPONENT_REGEX.matchEntire(firstLine)
            ?: throw ParseException("Invalid component definition at line ${startIndex + 1}: $firstLine")

        val name = match.groupValues[1]
        val paramsStr = match.groupValues[2]
        val params = parseComponentParams(paramsStr)

        val baseIndent = getIndent(lines[startIndex])
        var index = startIndex + 1
        var state: NanoNode.StateBlock? = null
        val children = mutableListOf<NanoNode>()

        while (index < lines.size) {
            val line = lines[index]
            if (line.isBlank()) {
                index++
                continue
            }

            val currentIndent = getIndent(line)
            if (currentIndent <= baseIndent) break

            val trimmed = line.trim()

            // Check for state block
            if (STATE_BLOCK_REGEX.matches(trimmed)) {
                val (stateBlock, newIndex) = parseStateBlock(lines, index)
                state = stateBlock
                index = newIndex
                continue
            }

            // Parse child nodes
            val (node, newIndex) = parseNode(lines, index, currentIndent)
            if (node != null) {
                children.add(node)
            }
            index = newIndex
        }

        return NanoNode.Component(name, params, state, children) to index
    }

    private fun parseComponentParams(paramsStr: String): List<NanoNode.ComponentParam> {
        if (paramsStr.isBlank()) return emptyList()
        return paramsStr.split(",").map { param ->
            val parts = param.trim().split(":")
            if (parts.size == 2) {
                NanoNode.ComponentParam(parts[0].trim(), parts[1].trim())
            } else {
                NanoNode.ComponentParam(parts[0].trim())
            }
        }
    }

    private fun parseStateBlock(lines: List<String>, startIndex: Int): Pair<NanoNode.StateBlock, Int> {
        val baseIndent = getIndent(lines[startIndex])
        var index = startIndex + 1
        val variables = mutableListOf<NanoNode.StateVariable>()

        while (index < lines.size) {
            val line = lines[index]
            if (line.isBlank()) {
                index++
                continue
            }

            val currentIndent = getIndent(line)
            if (currentIndent <= baseIndent) break

            val match = STATE_VAR_REGEX.matchEntire(line)
            if (match != null) {
                variables.add(
                    NanoNode.StateVariable(
                        name = match.groupValues[1],
                        type = match.groupValues[2],
                        defaultValue = match.groupValues[3].trim()
                    )
                )
            }
            index++
        }

        return NanoNode.StateBlock(variables) to index
    }

    private fun parseNode(lines: List<String>, startIndex: Int, expectedIndent: Int): Pair<NanoNode?, Int> {
        if (startIndex >= lines.size) return null to startIndex

        val line = lines[startIndex]
        if (line.isBlank()) return null to startIndex + 1

        val trimmed = line.trim()

        // Handle if statement
        IF_REGEX.matchEntire(trimmed)?.let { match ->
            return parseConditional(lines, startIndex, match.groupValues[1])
        }

        // Handle for loop
        FOR_REGEX.matchEntire(trimmed)?.let { match ->
            return parseForLoop(lines, startIndex, match.groupValues[1], match.groupValues[2])
        }

        // Handle Divider
        if (trimmed == "Divider") {
            return NanoNode.Divider to startIndex + 1
        }

        // Handle component with block
        COMPONENT_CALL_REGEX.matchEntire(trimmed)?.let { match ->
            return parseComponentCall(lines, startIndex, match.groupValues[1], match.groupValues[2])
        }

        // Handle inline component
        COMPONENT_INLINE_REGEX.matchEntire(trimmed)?.let { match ->
            val node = createNode(match.groupValues[1], match.groupValues[2], emptyList())
            return node to startIndex + 1
        }

        return null to startIndex + 1
    }

    private fun parseComponentCall(
        lines: List<String>,
        startIndex: Int,
        componentName: String,
        argsStr: String
    ): Pair<NanoNode?, Int> {
        val baseIndent = getIndent(lines[startIndex])
        var index = startIndex + 1
        val children = mutableListOf<NanoNode>()
        val props = mutableMapOf<String, String>()
        var onClick: NanoAction? = null

        while (index < lines.size) {
            val line = lines[index]
            if (line.isBlank()) {
                index++
                continue
            }

            val currentIndent = getIndent(line)
            if (currentIndent <= baseIndent) break

            val trimmed = line.trim()

            // Check for on_click action
            ON_CLICK_REGEX.matchEntire(trimmed)?.let { match ->
                onClick = parseAction(match.groupValues[1])
                index++
                continue
            }

            // Check for property
            PROP_REGEX.matchEntire(trimmed)?.let { match ->
                val propName = match.groupValues[1]
                val propValue = match.groupValues[2].trim()

                // "content:" is a special case for nested children
                if (propName == "content") {
                    val (contentChildren, newIndex) = parseChildren(lines, index)
                    children.addAll(contentChildren)
                    index = newIndex
                    continue
                }

                // Strip quotes from property value
                props[propName] = propValue.removeSurrounding("\"")
                index++
                continue
            }

            // Parse as child node
            val (node, newIndex) = parseNode(lines, index, currentIndent)
            if (node != null) {
                children.add(node)
            }
            index = newIndex
        }

        val node = createNode(componentName, argsStr, children, props, onClick)
        return node to index
    }

    private fun parseChildren(lines: List<String>, startIndex: Int): Pair<List<NanoNode>, Int> {
        val baseIndent = getIndent(lines[startIndex])
        var index = startIndex + 1
        val children = mutableListOf<NanoNode>()

        while (index < lines.size) {
            val line = lines[index]
            if (line.isBlank()) {
                index++
                continue
            }

            val currentIndent = getIndent(line)
            if (currentIndent <= baseIndent) break

            val (node, newIndex) = parseNode(lines, index, currentIndent)
            if (node != null) {
                children.add(node)
            }
            index = newIndex
        }

        return children to index
    }

    private fun parseConditional(
        lines: List<String>,
        startIndex: Int,
        condition: String
    ): Pair<NanoNode.Conditional, Int> {
        val (thenBranch, index) = parseChildren(lines, startIndex)
        return NanoNode.Conditional(condition, thenBranch) to index
    }

    private fun parseForLoop(
        lines: List<String>,
        startIndex: Int,
        variable: String,
        iterable: String
    ): Pair<NanoNode.ForLoop, Int> {
        val (body, index) = parseChildren(lines, startIndex)
        return NanoNode.ForLoop(variable, iterable, body) to index
    }

    private fun parseAction(actionStr: String): NanoAction {
        val trimmed = actionStr.trim()

        // Navigate action
        if (trimmed.startsWith("Navigate(")) {
            val toMatch = Regex("""Navigate\(to="(.+?)"\)""").find(trimmed)
            if (toMatch != null) {
                return NanoAction.Navigate(toMatch.groupValues[1])
            }
        }

        // ShowToast action
        if (trimmed.startsWith("ShowToast(")) {
            val msgMatch = Regex("""ShowToast\("(.+?)"\)""").find(trimmed)
            if (msgMatch != null) {
                return NanoAction.ShowToast(msgMatch.groupValues[1])
            }
        }

        // State mutation
        val mutationMatch = Regex("""state\.(\w+)\s*([+\-]?)=\s*(.+)""").find(trimmed)
        if (mutationMatch != null) {
            val path = mutationMatch.groupValues[1]
            val op = when (mutationMatch.groupValues[2]) {
                "+" -> MutationOp.ADD
                "-" -> MutationOp.SUBTRACT
                else -> MutationOp.SET
            }
            val value = mutationMatch.groupValues[3]
            return NanoAction.StateMutation(path, op, value)
        }

        return NanoAction.StateMutation("unknown", MutationOp.SET, trimmed)
    }

    private fun createNode(
        name: String,
        argsStr: String,
        children: List<NanoNode>,
        props: Map<String, String> = emptyMap(),
        onClick: NanoAction? = null
    ): NanoNode? {
        val args = parseArgs(argsStr)

        return when (name) {
            "VStack" -> NanoNode.VStack(
                spacing = args["spacing"] ?: props["spacing"],
                align = args["align"] ?: props["align"],
                children = children
            )
            "HStack" -> NanoNode.HStack(
                spacing = args["spacing"] ?: props["spacing"],
                align = args["align"] ?: props["align"],
                justify = args["justify"] ?: props["justify"],
                children = children
            )
            "Card" -> NanoNode.Card(
                padding = args["padding"] ?: props["padding"],
                shadow = args["shadow"] ?: props["shadow"],
                children = children
            )
            "Text" -> {
                val content = extractFirstArg(argsStr) ?: ""
                NanoNode.Text(
                    content = content,
                    style = args["style"] ?: props["style"]
                )
            }
            "Button" -> {
                val label = extractFirstArg(argsStr) ?: ""
                NanoNode.Button(
                    label = label,
                    intent = args["intent"] ?: props["intent"],
                    icon = args["icon"] ?: props["icon"],
                    onClick = onClick
                )
            }
            "Image" -> NanoNode.Image(
                src = args["src"] ?: "",
                aspect = args["aspect"],
                radius = args["radius"],
                width = args["width"]?.toIntOrNull()
            )
            "Badge" -> {
                val text = extractFirstArg(argsStr) ?: ""
                NanoNode.Badge(text = text, color = args["color"])
            }
            "Input" -> {
                val valueArg = args["value"]
                NanoNode.Input(
                    value = valueArg?.let { Binding.parse(it) },
                    placeholder = args["placeholder"],
                    type = args["type"]
                )
            }
            "Checkbox" -> {
                val checkedArg = args["checked"]
                NanoNode.Checkbox(
                    checked = checkedArg?.let { Binding.parse(it) }
                )
            }
            else -> null
        }
    }

    private fun parseArgs(argsStr: String): Map<String, String> {
        if (argsStr.isBlank()) return emptyMap()

        val result = mutableMapOf<String, String>()
        val argRegex = Regex("""(\w+)\s*[:=]\s*(?:"([^"]*)"|([\w.]+))""")
        argRegex.findAll(argsStr).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2].ifEmpty { match.groupValues[3] }
            result[key] = value
        }
        return result
    }

    private fun extractFirstArg(argsStr: String): String? {
        if (argsStr.isBlank()) return null
        val match = Regex("""^"([^"]*)".*$""").find(argsStr.trim())
        return match?.groupValues?.get(1)
    }

    private fun getIndent(line: String): Int {
        return line.takeWhile { it == ' ' || it == '\t' }.length
    }
}
