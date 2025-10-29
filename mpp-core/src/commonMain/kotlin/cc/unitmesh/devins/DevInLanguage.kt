package cc.unitmesh.devins

import cc.unitmesh.devins.parser.DevInLexer
import cc.unitmesh.devins.parser.DevInLexerImpl
import cc.unitmesh.devins.parser.DevInParser
import cc.unitmesh.devins.parser.DevInParserImpl
import cc.unitmesh.devins.parser.DevInParseResult
import cc.unitmesh.devins.parser.DevInParseError
import cc.unitmesh.devins.parser.DevInToken
import cc.unitmesh.devins.parser.DevInASTNodeTypes

/**
 * DevIn Language facade for multiplatform support
 * 
 * This class provides a high-level interface for working with DevIn language
 * across different platforms. It combines lexical analysis and parsing
 * into a convenient API.
 */
class DevInLanguage {
    private val lexer: DevInLexer = DevInLexerImpl()
    private val parser: DevInParser = DevInParserImpl()
    
    /**
     * Parse DevIn source code and return the AST
     */
    fun parse(source: String): DevInParseResult {
        return parser.parse(source)
    }
    
    /**
     * Tokenize DevIn source code
     */
    fun tokenize(source: String): List<DevInToken> {
        return lexer.tokenize(source)
    }
    
    /**
     * Parse from a token stream
     */
    fun parseTokens(tokens: List<DevInToken>): DevInParseResult {
        return parser.parseTokens(tokens)
    }
    
    /**
     * Parse a specific grammar rule (useful for testing)
     */
    fun parseRule(ruleName: String, input: String): DevInParseResult {
        return parser.parseRule(ruleName, input)
    }

    /**
     * Validate DevIn source code syntax
     */
    fun validate(source: String): List<DevInParseError> {
        val result = parse(source)
        return result.errors
    }

    /**
     * Check if DevIn source code is syntactically valid
     */
    fun isValid(source: String): Boolean {
        return validate(source).isEmpty()
    }

    companion object {
        /**
         * Create a new DevIn language instance
         */
        fun create(): DevInLanguage {
            return DevInLanguage()
        }

        /**
         * Parse DevIn source code (static method)
         */
        fun parse(source: String): DevInParseResult {
            return create().parse(source)
        }

        /**
         * Tokenize DevIn source code (static method)
         */
        fun tokenize(source: String): List<DevInToken> {
            return create().tokenize(source)
        }
    }
}

/**
 * DevIn Language Configuration
 */
data class DevInConfig(
    val enableFrontMatter: Boolean = true,
    val enableCodeBlocks: Boolean = true,
    val enableVariables: Boolean = true,
    val enableAgents: Boolean = true,
    val enableCommands: Boolean = true,
    val strictMode: Boolean = false
)

/**
 * DevIn Language Processor
 * 
 * Provides higher-level processing capabilities for DevIn documents
 */
class DevInProcessor(private val config: DevInConfig = DevInConfig()) {
    private val language = DevInLanguage.create()
    
    /**
     * Process a DevIn document and extract structured information
     */
    fun process(input: String): DevInProcessResult {
        val parseResult = language.parse(input)
        
        return DevInProcessResult(
            ast = parseResult.ast,
            errors = parseResult.errors,
            frontMatter = if (config.enableFrontMatter) extractFrontMatter(parseResult) else null,
            codeBlocks = if (config.enableCodeBlocks) extractCodeBlocks(parseResult) else emptyList(),
            variables = if (config.enableVariables) extractVariables(parseResult) else emptyList(),
            agents = if (config.enableAgents) extractAgents(parseResult) else emptyList(),
            commands = if (config.enableCommands) extractCommands(parseResult) else emptyList()
        )
    }
    
    private fun extractFrontMatter(parseResult: DevInParseResult): Map<String, Any>? {
        val ast = parseResult.ast ?: return null

        // Find front matter nodes in the AST
        val frontMatterNodes = findNodesByType(ast, DevInASTNodeTypes.FRONT_MATTER_HEADER)
        if (frontMatterNodes.isEmpty()) return null

        // For now, return a simple map indicating front matter was found
        return mapOf("found" to true)
    }
    
    private fun extractCodeBlocks(parseResult: DevInParseResult): List<DevInCodeBlock> {
        val ast = parseResult.ast ?: return emptyList()

        // Find code block nodes in the AST
        val codeBlockNodes = findNodesByType(ast, DevInASTNodeTypes.CODE_BLOCK)

        return codeBlockNodes.map { node ->
            DevInCodeBlock(
                language = "unknown", // Would need to extract from AST
                content = "code content", // Would need to extract from AST
                line = node.line,
                column = node.column
            )
        }
    }
    
    private fun extractVariables(parseResult: DevInParseResult): List<DevInVariable> {
        val ast = parseResult.ast ?: return emptyList()

        // Find variable block nodes in the AST
        val variableNodes = findNodesByType(ast, DevInASTNodeTypes.VARIABLE_BLOCK)

        return variableNodes.map { node ->
            DevInVariable(
                name = "variable", // Would need to extract from AST
                value = null,
                line = node.line,
                column = node.column
            )
        }
    }
    
    private fun extractAgents(parseResult: DevInParseResult): List<DevInAgent> {
        val ast = parseResult.ast ?: return emptyList()

        // Find agent block nodes in the AST
        val agentNodes = findNodesByType(ast, DevInASTNodeTypes.AGENT_BLOCK)

        return agentNodes.map { node ->
            DevInAgent(
                name = "agent", // Would need to extract from AST
                line = node.line,
                column = node.column
            )
        }
    }
    
    private fun extractCommands(parseResult: DevInParseResult): List<DevInCommand> {
        val ast = parseResult.ast ?: return emptyList()

        // Find command block nodes in the AST
        val commandNodes = findNodesByType(ast, DevInASTNodeTypes.COMMAND_BLOCK)

        return commandNodes.map { node ->
            DevInCommand(
                name = "command", // Would need to extract from AST
                properties = null,
                lineInfo = null,
                line = node.line,
                column = node.column
            )
        }
    }

    private fun findNodesByType(node: cc.unitmesh.devins.parser.DevInASTNode, targetType: String): List<cc.unitmesh.devins.parser.DevInASTNode> {
        val result = mutableListOf<cc.unitmesh.devins.parser.DevInASTNode>()

        if (node.type == targetType) {
            result.add(node)
        }

        node.children.forEach { child ->
            result.addAll(findNodesByType(child, targetType))
        }

        return result
    }
}

/**
 * DevIn Processing Result
 */
data class DevInProcessResult(
    val ast: cc.unitmesh.devins.parser.DevInASTNode?,
    val errors: List<cc.unitmesh.devins.parser.DevInParseError>,
    val frontMatter: Map<String, Any>?,
    val codeBlocks: List<DevInCodeBlock>,
    val variables: List<DevInVariable>,
    val agents: List<DevInAgent>,
    val commands: List<DevInCommand>
)

/**
 * Extracted DevIn elements
 */
data class DevInCodeBlock(
    val language: String?,
    val content: String,
    val line: Int,
    val column: Int
)

data class DevInVariable(
    val name: String,
    val value: String?,
    val line: Int,
    val column: Int
)

data class DevInAgent(
    val name: String,
    val line: Int,
    val column: Int
)

data class DevInCommand(
    val name: String,
    val properties: String?,
    val lineInfo: String?,
    val line: Int,
    val column: Int
)
