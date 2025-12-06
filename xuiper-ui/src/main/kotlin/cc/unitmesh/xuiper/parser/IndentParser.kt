package cc.unitmesh.xuiper.parser

import cc.unitmesh.xuiper.action.BodyField
import cc.unitmesh.xuiper.action.HttpMethod
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
        private val PROP_REGEX = Regex("""^(\w+):\s*(.*)$""")
        private val IF_REGEX = Regex("""^if\s+(.+?):\s*$""")
        private val FOR_REGEX = Regex("""^for\s+(\w+)\s+in\s+(.+?):\s*$""")
        private val ON_CLICK_REGEX = Regex("""^on_click:\s*(.+)$""")
        private val ON_CLICK_BLOCK_REGEX = Regex("""^on_click:\s*$""")
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
                val rawValue = match.groupValues[3].trim()
                // Strip quotes from string values (e.g., "" -> empty string, "hello" -> hello)
                val defaultValue = if (rawValue.startsWith("\"") && rawValue.endsWith("\"")) {
                    rawValue.substring(1, rawValue.length - 1)
                } else {
                    rawValue
                }
                variables.add(
                    NanoNode.StateVariable(
                        name = match.groupValues[1],
                        type = match.groupValues[2],
                        defaultValue = defaultValue
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

            // Check for on_click block (multi-line)
            if (ON_CLICK_BLOCK_REGEX.matches(trimmed)) {
                val (action, newIndex) = parseActionBlock(lines, index)
                onClick = action
                index = newIndex
                continue
            }

            // Check for on_click action (single-line)
            ON_CLICK_REGEX.matchEntire(trimmed)?.let { match ->
                onClick = parseAction(match.groupValues[1])
                index++
                continue
            }

            // Check if this is a component call (e.g., "VStack:" or "Button(...):") BEFORE checking property
            // This prevents "VStack:" from being matched as an empty property
            if (COMPONENT_CALL_REGEX.matches(trimmed) || COMPONENT_INLINE_REGEX.matches(trimmed)) {
                val (node, newIndex) = parseNode(lines, index, currentIndent)
                if (node != null) {
                    children.add(node)
                }
                index = newIndex
                continue
            }

            // Check for property
            PROP_REGEX.matchEntire(trimmed)?.let { match ->
                val propName = match.groupValues[1]
                val propValue = match.groupValues[2].trim()

                // "content:" is a special case for nested children
                // This handles the pattern:
                //   Card:
                //       padding: "lg"
                //       content:
                //           VStack(...):
                if (propName == "content") {
                    // If content is empty or just whitespace, parse children from content block
                    if (propValue.isEmpty() || propValue.isBlank()) {
                        val (contentChildren, newIndex) = parseContentBlock(lines, index)
                        children.addAll(contentChildren)
                        index = newIndex
                    } else {
                        // Content has an inline value, treat as property
                        props[propName] = propValue.removeSurrounding("\"")
                        index++
                    }
                    continue
                }

                // Skip empty property values
                if (propValue.isEmpty()) {
                    index++
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

    /**
     * Parse content block specifically for the "content:" property.
     * This handles the pattern:
     * ```
     * Card:
     *     padding: "lg"
     *     content:
     *         VStack(...):
     *             ...
     * ```
     * The baseIndent is the "content:" line, and children are indented below it.
     */
    private fun parseContentBlock(lines: List<String>, startIndex: Int): Pair<List<NanoNode>, Int> {
        val contentLineIndent = getIndent(lines[startIndex])
        var index = startIndex + 1
        val children = mutableListOf<NanoNode>()

        while (index < lines.size) {
            val line = lines[index]
            if (line.isBlank()) {
                index++
                continue
            }

            val currentIndent = getIndent(line)
            // Content block ends when we encounter a line with same or less indent than "content:" line
            if (currentIndent <= contentLineIndent) break

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

    /**
     * Parse a multi-line action block (on_click: with indented content)
     * Supports:
     * - State mutations
     * - Multi-line Fetch with on_success/on_error callbacks
     * - Sequence of actions
     */
    private fun parseActionBlock(lines: List<String>, startIndex: Int): Pair<NanoAction, Int> {
        val baseIndent = getIndent(lines[startIndex])
        var index = startIndex + 1
        val actions = mutableListOf<NanoAction>()
        val fetchBuilder = FetchActionBuilder()
        var inFetchBlock = false

        while (index < lines.size) {
            val line = lines[index]
            if (line.isBlank()) {
                index++
                continue
            }

            val currentIndent = getIndent(line)
            if (currentIndent <= baseIndent) break

            val trimmed = line.trim()

            // Handle Fetch block start (multi-line)
            if (trimmed.startsWith("Fetch(") && !trimmed.endsWith(")")) {
                inFetchBlock = true
                parseFetchStart(trimmed, fetchBuilder)
                index++
                continue
            }

            // Inside Fetch block
            if (inFetchBlock) {
                if (trimmed == ")") {
                    // End of Fetch block
                    actions.add(fetchBuilder.build())
                    inFetchBlock = false
                    fetchBuilder.reset()
                } else {
                    parseFetchLine(trimmed, fetchBuilder, lines, index)
                }
                index++
                continue
            }

            // Single-line Fetch
            if (trimmed.startsWith("Fetch(") && trimmed.endsWith(")")) {
                actions.add(parseFetchAction(trimmed))
                index++
                continue
            }

            // Other actions (state mutation, Navigate, ShowToast)
            actions.add(parseAction(trimmed))
            index++
        }

        val resultAction = when (actions.size) {
            0 -> NanoAction.StateMutation("unknown", MutationOp.SET, "")
            1 -> actions[0]
            else -> NanoAction.Sequence(actions)
        }

        return resultAction to index
    }

    /**
     * Helper class to build Fetch action from multi-line input
     */
    private class FetchActionBuilder {
        var url: String = ""
        var method: HttpMethod = HttpMethod.GET
        var body: MutableMap<String, BodyField>? = null
        var headers: MutableMap<String, String>? = null
        var params: MutableMap<String, String>? = null
        var onSuccess: NanoAction? = null
        var onError: NanoAction? = null
        var loadingState: String? = null

        fun build(): NanoAction.Fetch = NanoAction.Fetch(
            url = url,
            method = method,
            body = body,
            headers = headers,
            params = params,
            onSuccess = onSuccess,
            onError = onError,
            loadingState = loadingState
        )

        fun reset() {
            url = ""
            method = HttpMethod.GET
            body = null
            headers = null
            params = null
            onSuccess = null
            onError = null
            loadingState = null
        }
    }

    private fun parseFetchStart(line: String, builder: FetchActionBuilder) {
        val urlMatch = Regex("""url\s*=\s*"([^"]+)"""").find(line)
        builder.url = urlMatch?.groupValues?.get(1) ?: ""

        val methodMatch = Regex("""method\s*=\s*"([^"]+)"""").find(line)
        methodMatch?.groupValues?.get(1)?.let {
            builder.method = try { HttpMethod.valueOf(it.uppercase()) } catch (e: Exception) { HttpMethod.GET }
        }
    }

    private fun parseFetchLine(line: String, builder: FetchActionBuilder, lines: List<String>, currentIndex: Int) {
        val trimmed = line.trim().removeSuffix(",")

        // Parse url
        Regex("""url\s*=\s*"([^"]+)"""").find(trimmed)?.let {
            builder.url = it.groupValues[1]
        }

        // Parse method
        Regex("""method\s*=\s*"([^"]+)"""").find(trimmed)?.let {
            builder.method = try { HttpMethod.valueOf(it.groupValues[1].uppercase()) } catch (e: Exception) { HttpMethod.GET }
        }

        // Parse body
        if (trimmed.startsWith("body=") || trimmed.startsWith("body =")) {
            val bodyContent = trimmed.substringAfter("body").removePrefix("=").trim()
            if (bodyContent.startsWith("{") && bodyContent.endsWith("}")) {
                builder.body = parseFetchBodyContent(bodyContent.removeSurrounding("{", "}"))
            }
        }

        // Parse headers
        if (trimmed.startsWith("headers=") || trimmed.startsWith("headers =")) {
            val headersContent = trimmed.substringAfter("headers").removePrefix("=").trim()
            if (headersContent.startsWith("{") && headersContent.endsWith("}")) {
                builder.headers = parseFetchHeadersContent(headersContent.removeSurrounding("{", "}"))
            }
        }

        // Parse on_success callback
        if (trimmed.startsWith("on_success:")) {
            val actionStr = trimmed.removePrefix("on_success:").trim()
            if (actionStr.isNotEmpty()) {
                builder.onSuccess = parseAction(actionStr)
            }
        }

        // Parse on_error callback
        if (trimmed.startsWith("on_error:")) {
            val actionStr = trimmed.removePrefix("on_error:").trim()
            if (actionStr.isNotEmpty()) {
                builder.onError = parseAction(actionStr)
            }
        }
    }

    private fun parseFetchBodyContent(content: String): MutableMap<String, BodyField> {
        val result = mutableMapOf<String, BodyField>()
        val fieldRegex = Regex(""""(\w+)"\s*:\s*(state\.[\w.]+|"[^"]*")""")
        fieldRegex.findAll(content).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            result[key] = BodyField.parse(value)
        }
        return result
    }

    private fun parseFetchHeadersContent(content: String): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        val headerRegex = Regex(""""([^"]+)"\s*:\s*"([^"]+)"""")
        headerRegex.findAll(content).forEach { match ->
            result[match.groupValues[1]] = match.groupValues[2]
        }
        return result
    }

    private fun parseAction(actionStr: String): NanoAction {
        val trimmed = actionStr.trim()

        // Navigate action (enhanced with params, query, replace)
        if (trimmed.startsWith("Navigate(")) {
            return parseNavigateAction(trimmed)
        }

        // ShowToast action
        if (trimmed.startsWith("ShowToast(")) {
            val msgMatch = Regex("""ShowToast\("(.+?)"\)""").find(trimmed)
            if (msgMatch != null) {
                return NanoAction.ShowToast(msgMatch.groupValues[1])
            }
        }

        // Fetch action - simple pattern
        if (trimmed.startsWith("Fetch(")) {
            return parseFetchAction(trimmed)
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

    /**
     * Parse Navigate action with enhanced routing support
     * Examples:
     * - `Navigate(to="/home")`
     * - `Navigate(to="/user/{id}", params={"id": state.userId})`
     * - `Navigate(to="/search", query={"q": state.query})`
     * - `Navigate(to="/login", replace=true)`
     */
    private fun parseNavigateAction(actionStr: String): NanoAction.Navigate {
        val paramsStr = actionStr.removePrefix("Navigate(").removeSuffix(")")

        // Parse 'to' (required)
        val toMatch = Regex("""to\s*=\s*"([^"]+)"""").find(paramsStr)
        val to = toMatch?.groupValues?.get(1) ?: "/"

        // Parse 'replace' boolean
        val replaceMatch = Regex("""replace\s*=\s*(true|false)""").find(paramsStr)
        val replace = replaceMatch?.groupValues?.get(1)?.toBoolean() ?: false

        // Parse 'params' map: params={"id": state.userId} or params={"id": "123"}
        val routeParams = parseNavigateMap(paramsStr, "params")

        // Parse 'query' map: query={"q": state.query}
        val queryParams = parseNavigateMap(paramsStr, "query")

        return NanoAction.Navigate(
            to = to,
            params = routeParams,
            query = queryParams,
            replace = replace
        )
    }

    /**
     * Parse a map parameter from Navigate action
     * Supports: {"key": "value"} or {"key": state.path}
     */
    private fun parseNavigateMap(paramsStr: String, mapName: String): Map<String, String>? {
        val mapMatch = Regex("""$mapName\s*=\s*\{([^}]*)\}""").find(paramsStr) ?: return null
        val mapContent = mapMatch.groupValues[1]

        val result = mutableMapOf<String, String>()
        // Match patterns like "id": "123" or "id": state.userId
        val fieldRegex = Regex(""""(\w+)"\s*:\s*("([^"]*)"|state\.[\w.]+)""")
        fieldRegex.findAll(mapContent).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            // Remove quotes if it's a literal string
            result[key] = if (value.startsWith("\"")) {
                value.trim('"')
            } else {
                value // Keep state.xxx as-is for runtime binding
            }
        }

        return result.takeIf { it.isNotEmpty() }
    }

    /**
     * Parse Fetch action with parameters
     * Examples:
     * - `Fetch(url="/api/users")`
     * - `Fetch(url="/api/login", method="POST")`
     * - `Fetch(url="/api/login", method="POST", body={"email": state.email})`
     */
    private fun parseFetchAction(actionStr: String): NanoAction.Fetch {
        // Extract parameters from Fetch(...)
        val paramsStr = actionStr.removePrefix("Fetch(").removeSuffix(")")

        // Parse URL
        val urlMatch = Regex("""url\s*=\s*"([^"]+)"""").find(paramsStr)
        val url = urlMatch?.groupValues?.get(1) ?: "/api/unknown"

        // Parse method
        val methodMatch = Regex("""method\s*=\s*"([^"]+)"""").find(paramsStr)
        val method = methodMatch?.groupValues?.get(1)?.uppercase()?.let {
            try { HttpMethod.valueOf(it) } catch (e: Exception) { HttpMethod.GET }
        } ?: HttpMethod.GET

        // Parse body (simplified: extract key-value pairs)
        val body = parseFetchBody(paramsStr)

        // Parse headers (simplified)
        val headers = parseFetchHeaders(paramsStr)

        return NanoAction.Fetch(
            url = url,
            method = method,
            body = body,
            headers = headers
        )
    }

    /**
     * Parse body from Fetch params: body={"key": value, ...}
     */
    private fun parseFetchBody(paramsStr: String): Map<String, BodyField>? {
        val bodyMatch = Regex("""body\s*=\s*\{([^}]*)\}""").find(paramsStr) ?: return null
        val bodyContent = bodyMatch.groupValues[1]

        val result = mutableMapOf<String, BodyField>()
        // Match patterns like "email": state.email or "name": "literal"
        val fieldRegex = Regex(""""(\w+)"\s*:\s*(state\.[\w.]+|"[^"]*")""")
        fieldRegex.findAll(bodyContent).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            result[key] = BodyField.parse(value)
        }

        return result.takeIf { it.isNotEmpty() }
    }

    /**
     * Parse headers from Fetch params: headers={"Key": "value", ...}
     */
    private fun parseFetchHeaders(paramsStr: String): Map<String, String>? {
        val headersMatch = Regex("""headers\s*=\s*\{([^}]*)\}""").find(paramsStr) ?: return null
        val headersContent = headersMatch.groupValues[1]

        val result = mutableMapOf<String, String>()
        val headerRegex = Regex(""""([^"]+)"\s*:\s*"([^"]+)"""")
        headerRegex.findAll(headersContent).forEach { match ->
            result[match.groupValues[1]] = match.groupValues[2]
        }

        return result.takeIf { it.isNotEmpty() }
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
                val contentArg = args["content"]
                val content = if (contentArg != null) {
                    // If content is a binding, extract the expression
                    if (contentArg.startsWith("<<") || contentArg.startsWith(":=")) {
                        "" // Content will come from binding
                    } else {
                        contentArg
                    }
                } else {
                    extractFirstArg(argsStr) ?: ""
                }
                val binding = contentArg?.let { Binding.parse(it) }?.takeIf { it !is Binding.Static }
                NanoNode.Text(
                    content = content,
                    style = args["style"] ?: props["style"],
                    binding = binding
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
            "TextArea" -> {
                val valueArg = args["value"]
                NanoNode.TextArea(
                    value = valueArg?.let { Binding.parse(it) },
                    placeholder = args["placeholder"],
                    rows = args["rows"]?.toIntOrNull()
                )
            }
            "Select" -> {
                val valueArg = args["value"]
                NanoNode.Select(
                    value = valueArg?.let { Binding.parse(it) },
                    options = args["options"],
                    placeholder = args["placeholder"]
                )
            }
            "Form" -> {
                NanoNode.Form(
                    onSubmit = args["onSubmit"],
                    children = children
                )
            }
            else -> null
        }
    }

    private fun parseArgs(argsStr: String): Map<String, String> {
        if (argsStr.isBlank()) return emptyMap()

        val result = mutableMapOf<String, String>()

        // First, handle << (subscribe binding) pattern: content << state.count
        val subscribeRegex = Regex("""(\w+)\s*<<\s*([\w.]+)""")
        subscribeRegex.findAll(argsStr).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            result[key] = "<< $value"
        }

        // Then handle := (two-way binding) and = (assignment) patterns
        // Match patterns like: name="value", name=value, name := state.path, name: "value"
        val argRegex = Regex("""(\w+)\s*(?::=|=|:)\s*(?:"([^"]*)"|([\w.]+))""")
        argRegex.findAll(argsStr).forEach { match ->
            val key = match.groupValues[1]
            // Skip if already handled by subscribe binding
            if (key in result) return@forEach

            val rawValue = match.groupValues[2].ifEmpty { match.groupValues[3] }

            // Preserve the binding operator for value parsing
            val operatorMatch = Regex("""(\w+)\s*(:=)""").find(argsStr)
            val hasTwoWayBinding = operatorMatch?.groupValues?.get(1) == key

            result[key] = if (hasTwoWayBinding) ":= $rawValue" else rawValue
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
