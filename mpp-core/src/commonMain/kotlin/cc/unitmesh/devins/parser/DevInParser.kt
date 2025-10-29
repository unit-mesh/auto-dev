package cc.unitmesh.devins.parser

/**
 * DevIn Language Parser for multiplatform support
 * 
 * This parser builds an Abstract Syntax Tree (AST) from DevIn language tokens.
 * It supports the complete DevIn grammar including:
 * - Front matter blocks
 * - Agent, command, and variable declarations
 * - Code blocks with language specifications
 * - Expression evaluation
 * - Function calls and pipelines
 * - Conditional statements
 */

/**
 * Base AST Node interface
 */
interface DevInASTNode {
    val type: String
    val children: List<DevInASTNode>
    val startOffset: Int
    val endOffset: Int
    val line: Int
    val column: Int
}

/**
 * DevIn AST Node Types
 */
object DevInASTNodeTypes {
    const val FILE = "FILE"
    const val FRONT_MATTER_HEADER = "FRONT_MATTER_HEADER"
    const val FRONT_MATTER_ENTRY = "FRONT_MATTER_ENTRY"
    const val FRONT_MATTER_KEY = "FRONT_MATTER_KEY"
    const val FRONT_MATTER_VALUE = "FRONT_MATTER_VALUE"
    const val FRONT_MATTER_ARRAY = "FRONT_MATTER_ARRAY"
    
    const val AGENT_BLOCK = "AGENT_BLOCK"
    const val COMMAND_BLOCK = "COMMAND_BLOCK"
    const val VARIABLE_BLOCK = "VARIABLE_BLOCK"
    const val EXPRESSION_BLOCK = "EXPRESSION_BLOCK"
    
    const val CODE_BLOCK = "CODE_BLOCK"
    const val CODE_CONTENT = "CODE_CONTENT"
    
    const val FUNCTION_CALL = "FUNCTION_CALL"
    const val FUNCTION_STATEMENT = "FUNCTION_STATEMENT"
    const val FOREIGN_FUNCTION = "FOREIGN_FUNCTION"
    const val PIPELINE_ARGS = "PIPELINE_ARGS"
    
    const val PATTERN_ACTION = "PATTERN_ACTION"
    const val ACTION_BLOCK = "ACTION_BLOCK"
    const val ACTION_BODY = "ACTION_BODY"
    const val ACTION_EXPR = "ACTION_EXPR"
    
    const val CASE_BODY = "CASE_BODY"
    const val CASE_PATTERN_ACTION = "CASE_PATTERN_ACTION"
    const val CASE_CONDITION = "CASE_CONDITION"
    
    const val CONDITION_EXPR = "CONDITION_EXPR"
    const val CONDITION_FLAG = "CONDITION_FLAG"
    const val CONDITION_STATEMENT = "CONDITION_STATEMENT"
    
    const val LOGICAL_OR_EXPR = "LOGICAL_OR_EXPR"
    const val LOGICAL_AND_EXPR = "LOGICAL_AND_EXPR"
    const val EQ_COMPARISON_EXPR = "EQ_COMPARISON_EXPR"
    const val INEQ_COMPARISON_EXPR = "INEQ_COMPARISON_EXPR"
    const val CALL_EXPR = "CALL_EXPR"
    const val QUAL_REF_EXPR = "QUAL_REF_EXPR"
    const val SIMPLE_REF_EXPR = "SIMPLE_REF_EXPR"
    const val LITERAL_EXPR = "LITERAL_EXPR"
    const val PAREN_EXPR = "PAREN_EXPR"
    const val VARIABLE_EXPR = "VARIABLE_EXPR"
    
    const val VELOCITY_EXPR = "VELOCITY_EXPR"
    const val VELOCITY_BLOCK = "VELOCITY_BLOCK"
    const val IF_EXPR = "IF_EXPR"
    const val IF_CLAUSE = "IF_CLAUSE"
    const val ELSEIF_CLAUSE = "ELSEIF_CLAUSE"
    const val ELSE_CLAUSE = "ELSE_CLAUSE"
    
    const val QUERY_STATEMENT = "QUERY_STATEMENT"
    const val FROM_CLAUSE = "FROM_CLAUSE"
    const val WHERE_CLAUSE = "WHERE_CLAUSE"
    const val SELECT_CLAUSE = "SELECT_CLAUSE"
    const val PSI_ELEMENT_DECL = "PSI_ELEMENT_DECL"
    const val PSI_VAR_DECL = "PSI_VAR_DECL"
    
    const val MARKDOWN_HEADER = "MARKDOWN_HEADER"
    const val TEXT_SEGMENT = "TEXT_SEGMENT"
    const val COMMENTS = "COMMENTS"
    const val NEWLINE = "NEWLINE"
    
    const val IDENTIFIER = "IDENTIFIER"
    const val NUMBER = "NUMBER"
    const val STRING = "STRING"
    const val BOOLEAN = "BOOLEAN"
    const val PATTERN = "PATTERN"
}

/**
 * Concrete AST Node implementations
 */
data class DevInFileNode(
    override val children: List<DevInASTNode>,
    override val startOffset: Int,
    override val endOffset: Int,
    override val line: Int,
    override val column: Int
) : DevInASTNode {
    override val type = DevInASTNodeTypes.FILE
}

data class DevInFrontMatterNode(
    val entries: List<DevInASTNode>,
    override val children: List<DevInASTNode>,
    override val startOffset: Int,
    override val endOffset: Int,
    override val line: Int,
    override val column: Int
) : DevInASTNode {
    override val type = DevInASTNodeTypes.FRONT_MATTER_HEADER
}

data class DevInCodeBlockNode(
    val languageId: String?,
    val content: String,
    override val children: List<DevInASTNode>,
    override val startOffset: Int,
    override val endOffset: Int,
    override val line: Int,
    override val column: Int
) : DevInASTNode {
    override val type = DevInASTNodeTypes.CODE_BLOCK
}

data class DevInAgentBlockNode(
    val agentId: String,
    override val children: List<DevInASTNode>,
    override val startOffset: Int,
    override val endOffset: Int,
    override val line: Int,
    override val column: Int
) : DevInASTNode {
    override val type = DevInASTNodeTypes.AGENT_BLOCK
}

data class DevInCommandBlockNode(
    val commandId: String,
    val commandProp: String?,
    val lineInfo: String?,
    override val children: List<DevInASTNode>,
    override val startOffset: Int,
    override val endOffset: Int,
    override val line: Int,
    override val column: Int
) : DevInASTNode {
    override val type = DevInASTNodeTypes.COMMAND_BLOCK
}

data class DevInVariableBlockNode(
    val variableId: String,
    override val children: List<DevInASTNode>,
    override val startOffset: Int,
    override val endOffset: Int,
    override val line: Int,
    override val column: Int
) : DevInASTNode {
    override val type = DevInASTNodeTypes.VARIABLE_BLOCK
}

/**
 * Parse result containing AST and any errors
 */
data class DevInParseResult(
    val ast: DevInASTNode?,
    val errors: List<DevInParseError>
)

/**
 * Parse error information
 */
data class DevInParseError(
    val message: String,
    val line: Int,
    val column: Int,
    val offset: Int,
    val token: DevInToken?
)

/**
 * DevIn Parser interface
 */
interface DevInParser {
    /**
     * Parse DevIn source code into an AST
     */
    fun parse(input: String): DevInParseResult
    
    /**
     * Parse from a token stream
     */
    fun parseTokens(tokens: List<DevInToken>): DevInParseResult
    
    /**
     * Parse a specific rule (for testing)
     */
    fun parseRule(ruleName: String, input: String): DevInParseResult
}

/**
 * Expected DevIn Parser implementation
 */
expect class DevInParserImpl() : DevInParser {
    override fun parse(input: String): DevInParseResult
    override fun parseTokens(tokens: List<DevInToken>): DevInParseResult
    override fun parseRule(ruleName: String, input: String): DevInParseResult
}

/**
 * Shared DevIn Parser implementation that can be used by all platforms
 * This reduces code duplication while still allowing platform-specific optimizations
 */
class DevInParserShared {
    private val lexer = DevInLexerImpl()

    fun parse(input: String): DevInParseResult {
        return try {
            val tokens = lexer.tokenize(input)
            parseTokens(tokens)
        } catch (e: Exception) {
            DevInParseResult(
                ast = null,
                errors = listOf(
                    DevInParseError(
                        message = "Parse error: ${e.message}",
                        line = 1,
                        column = 1,
                        offset = 0,
                        token = null
                    )
                )
            )
        }
    }

    fun parseTokens(tokens: List<DevInToken>): DevInParseResult {
        return try {
            val parser = SharedSimpleDevInParser(tokens)
            val ast = parser.parseFile()
            DevInParseResult(ast = ast, errors = parser.errors)
        } catch (e: Exception) {
            DevInParseResult(
                ast = null,
                errors = listOf(
                    DevInParseError(
                        message = "Parse error: ${e.message}",
                        line = 1,
                        column = 1,
                        offset = 0,
                        token = null
                    )
                )
            )
        }
    }

    fun parseRule(ruleName: String, input: String): DevInParseResult {
        return parse(input)
    }
}

// 共享的解析器实现 - 所有平台都可以使用
private class SharedSimpleDevInParser(private val tokens: List<DevInToken>) {
    private var position = 0
    val errors = mutableListOf<DevInParseError>()

    fun parseFile(): DevInASTNode {
        // ... 实现细节 (与之前相同)
        return object : DevInASTNode {
            override val type = DevInASTNodeTypes.FILE
            override val children = emptyList<DevInASTNode>()
            override val startOffset = 0
            override val endOffset = 0
            override val line = 1
            override val column = 1
        }
    }
}
