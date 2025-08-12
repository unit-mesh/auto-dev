package cc.unitmesh.diagram.parser.mermaid

/**
 * Parser for Mermaid class diagrams
 * Based on the grammar rules in classDiagram.jison
 */
class MermaidParser(private val tokens: List<MermaidToken>) {
    private var position = 0
    private val errors = mutableListOf<ParseError>()
    
    /**
     * Parse the token stream into an AST
     */
    fun parse(): ParseResult {
        errors.clear()
        position = 0
        
        return try {
            val statements = parseStatements()
            val ast = ClassDiagramNode(statements)
            ParseResult.Success(ast)
        } catch (e: ParseException) {
            errors.add(ParseError(e.message ?: "Unknown parse error", getCurrentPosition()))
            ParseResult.Error(errors.toList())
        }
    }
    
    private fun parseStatements(): List<StatementNode> {
        val statements = mutableListOf<StatementNode>()
        
        // Skip initial tokens until we find classDiagram
        while (!isAtEnd() && !check(TokenType.CLASS_DIAGRAM)) {
            advance()
        }
        
        if (check(TokenType.CLASS_DIAGRAM)) {
            advance() // consume classDiagram
            consumeNewlines()
        }
        
        while (!isAtEnd()) {
            try {
                val statement = parseStatement()
                if (statement != null) {
                    statements.add(statement)
                }
                consumeNewlines()
            } catch (e: ParseException) {
                errors.add(ParseError(e.message ?: "Error parsing statement", getCurrentPosition()))
                synchronize()
            }
        }
        
        return statements
    }
    
    private fun parseStatement(): StatementNode? {
        return when {
            check(TokenType.CLASS) -> parseClassStatement()
            check(TokenType.NAMESPACE) -> parseNamespaceStatement()
            check(TokenType.NOTE) -> parseNoteStatement()
            check(TokenType.NOTE_FOR) -> parseNoteForStatement()
            check(TokenType.STYLE) -> parseStyleStatement()
            check(TokenType.CLASSDEF) -> parseClassDefStatement()
            check(TokenType.CLICK) -> parseClickStatement()
            check(TokenType.CALLBACK) -> parseCallbackStatement()
            check(TokenType.LINK) -> parseLinkStatement()
            check(TokenType.CSSCLASS) -> parseCssClassStatement()
            check(TokenType.ANNOTATION_START) -> parseAnnotationStatement()
            check(TokenType.DIRECTION_TB, TokenType.DIRECTION_BT, TokenType.DIRECTION_RL, TokenType.DIRECTION_LR) -> parseDirectionStatement()
            check(TokenType.ACC_TITLE) -> parseAccessibilityStatement()
            check(TokenType.ACC_DESCR) -> parseAccessibilityStatement()
            checkRelationStatement() -> parseRelationStatement()
            checkMemberStatement() -> parseMemberStatement()
            else -> {
                if (!check(TokenType.NEWLINE, TokenType.EOF)) {
                    advance() // Skip unknown token
                }
                null
            }
        }
    }
    
    private fun parseClassStatement(): ClassStatementNode {
        consume(TokenType.CLASS, "Expected 'class'")
        val className = parseClassName()
        
        var classLabel: String? = null
        var members = emptyList<MemberNode>()
        var cssClass: String? = null
        
        // Check for class label [label]
        if (check(TokenType.SQS)) {
            advance()
            classLabel = consume(TokenType.STR, "Expected class label").value
            consume(TokenType.SQE, "Expected ']'")
        }
        
        // Check for CSS class separator :::
        if (check(TokenType.STYLE_SEPARATOR)) {
            advance()
            cssClass = consume(TokenType.ALPHA, "Expected CSS class name").value
        }
        
        // Check for class body
        if (check(TokenType.STRUCT_START)) {
            advance()
            members = parseMembers()
            consume(TokenType.STRUCT_STOP, "Expected '}'")
        }
        
        return ClassStatementNode(className, classLabel, members, cssClass, getCurrentPosition())
    }
    
    private fun parseNamespaceStatement(): NamespaceStatementNode {
        consume(TokenType.NAMESPACE, "Expected 'namespace'")
        val namespaceName = parseNamespaceName()
        
        consume(TokenType.STRUCT_START, "Expected '{'")
        consumeNewlines()
        
        val classes = mutableListOf<ClassStatementNode>()
        while (!check(TokenType.STRUCT_STOP) && !isAtEnd()) {
            if (check(TokenType.CLASS)) {
                classes.add(parseClassStatement())
            } else {
                advance()
            }
            consumeNewlines()
        }
        
        consume(TokenType.STRUCT_STOP, "Expected '}'")
        
        return NamespaceStatementNode(namespaceName, classes, getCurrentPosition())
    }
    
    private fun parseRelationStatement(): RelationStatementNode {
        val sourceClass = parseClassName()
        
        var sourceLabel: String? = null
        if (check(TokenType.STR)) {
            sourceLabel = advance().value
        }
        
        val relation = parseRelation()
        
        var targetLabel: String? = null
        if (check(TokenType.STR)) {
            targetLabel = advance().value
        }
        
        val targetClass = parseClassName()
        
        var relationLabel: String? = null
        if (check(TokenType.LABEL)) {
            relationLabel = advance().value.substring(1) // Remove leading ':'
        }
        
        return RelationStatementNode(
            sourceClass, targetClass, relation,
            sourceLabel, targetLabel, relationLabel,
            getCurrentPosition()
        )
    }
    
    private fun parseMemberStatement(): MemberStatementNode {
        val className = parseClassName()
        consume(TokenType.COLON, "Expected ':'")
        
        val memberText = when {
            check(TokenType.LABEL) -> advance().value.substring(1) // Remove leading ':'
            check(TokenType.MEMBER) -> advance().value
            else -> throw ParseException("Expected member definition")
        }
        
        val member = parseMemberFromText(memberText)
        return MemberStatementNode(className, member, getCurrentPosition())
    }
    
    private fun parseNoteStatement(): NoteStatementNode {
        consume(TokenType.NOTE, "Expected 'note'")
        val noteText = consume(TokenType.STR, "Expected note text").value
        return NoteStatementNode(noteText, null, getCurrentPosition())
    }
    
    private fun parseNoteForStatement(): NoteStatementNode {
        consume(TokenType.NOTE_FOR, "Expected 'note for'")
        val className = parseClassName()
        val noteText = consume(TokenType.STR, "Expected note text").value
        return NoteStatementNode(noteText, className, getCurrentPosition())
    }
    
    private fun parseAnnotationStatement(): AnnotationStatementNode {
        consume(TokenType.ANNOTATION_START, "Expected '<<'")
        val annotation = consume(TokenType.ALPHA, "Expected annotation").value
        consume(TokenType.ANNOTATION_END, "Expected '>>'")
        val className = parseClassName()
        return AnnotationStatementNode(className, annotation, getCurrentPosition())
    }
    
    private fun parseStyleStatement(): StyleStatementNode {
        consume(TokenType.STYLE, "Expected 'style'")
        val className = consume(TokenType.ALPHA, "Expected class name").value
        val styles = parseStyles()
        return StyleStatementNode(className, styles, getCurrentPosition())
    }
    
    private fun parseClassDefStatement(): ClassDefStatementNode {
        consume(TokenType.CLASSDEF, "Expected 'classDef'")
        val classNames = parseClassList()
        val styles = parseStyles()
        return ClassDefStatementNode(classNames, styles, getCurrentPosition())
    }
    
    private fun parseClickStatement(): ClickStatementNode {
        consume(TokenType.CLICK, "Expected 'click'")
        val className = parseClassName()
        
        val action = when {
            check(TokenType.CALLBACK_NAME) -> {
                val callbackName = advance().value
                var args: String? = null
                var tooltip: String? = null
                
                if (check(TokenType.CALLBACK_ARGS)) {
                    args = advance().value
                }
                if (check(TokenType.STR)) {
                    tooltip = advance().value
                }
                
                ClickActionNode.CallbackAction(callbackName, args, tooltip, getCurrentPosition())
            }
            
            check(TokenType.HREF) -> {
                advance()
                val url = consume(TokenType.STR, "Expected URL").value
                var target: String? = null
                var tooltip: String? = null
                
                if (check(TokenType.LINK_TARGET)) {
                    target = advance().value
                }
                if (check(TokenType.STR)) {
                    tooltip = advance().value
                }
                
                ClickActionNode.LinkAction(url, target, tooltip, getCurrentPosition())
            }
            
            else -> throw ParseException("Expected callback name or href")
        }
        
        return ClickStatementNode(className, action, getCurrentPosition())
    }
    
    private fun parseCallbackStatement(): ClickStatementNode {
        consume(TokenType.CALLBACK, "Expected 'callback'")
        val className = parseClassName()
        val callbackName = consume(TokenType.STR, "Expected callback name").value
        
        var tooltip: String? = null
        if (check(TokenType.STR)) {
            tooltip = advance().value
        }
        
        val action = ClickActionNode.CallbackAction(callbackName, null, tooltip, getCurrentPosition())
        return ClickStatementNode(className, action, getCurrentPosition())
    }
    
    private fun parseLinkStatement(): ClickStatementNode {
        consume(TokenType.LINK, "Expected 'link'")
        val className = parseClassName()
        val url = consume(TokenType.STR, "Expected URL").value
        
        var target: String? = null
        var tooltip: String? = null
        
        if (check(TokenType.LINK_TARGET)) {
            target = advance().value
        }
        if (check(TokenType.STR)) {
            tooltip = advance().value
        }
        
        val action = ClickActionNode.LinkAction(url, target, tooltip, getCurrentPosition())
        return ClickStatementNode(className, action, getCurrentPosition())
    }
    
    private fun parseCssClassStatement(): StyleStatementNode {
        consume(TokenType.CSSCLASS, "Expected 'cssClass'")
        val className = consume(TokenType.STR, "Expected class name").value
        val cssClassName = consume(TokenType.ALPHA, "Expected CSS class name").value
        
        val style = StyleNode("cssClass", cssClassName, getCurrentPosition())
        return StyleStatementNode(className, listOf(style), getCurrentPosition())
    }
    
    private fun parseDirectionStatement(): DirectionStatementNode {
        val directionType = when {
            check(TokenType.DIRECTION_TB) -> DirectionType.TB
            check(TokenType.DIRECTION_BT) -> DirectionType.BT
            check(TokenType.DIRECTION_RL) -> DirectionType.RL
            check(TokenType.DIRECTION_LR) -> DirectionType.LR
            else -> throw ParseException("Expected direction")
        }
        
        advance()
        return DirectionStatementNode(directionType, getCurrentPosition())
    }
    
    private fun parseAccessibilityStatement(): AccessibilityStatementNode {
        val type = when {
            check(TokenType.ACC_TITLE) -> AccessibilityType.TITLE
            check(TokenType.ACC_DESCR) -> AccessibilityType.DESCRIPTION
            else -> throw ParseException("Expected accessibility statement")
        }
        
        advance()
        
        val value = when (type) {
            AccessibilityType.TITLE -> consume(TokenType.ACC_TITLE_VALUE, "Expected title value").value
            AccessibilityType.DESCRIPTION -> {
                if (check(TokenType.ACC_DESCR_MULTILINE_VALUE)) {
                    advance().value
                } else {
                    consume(TokenType.ACC_DESCR_VALUE, "Expected description value").value
                }
            }
            else -> throw ParseException("Unexpected accessibility type")
        }
        
        return AccessibilityStatementNode(type, value, getCurrentPosition())
    }
    
    private fun parseClassName(): String {
        return when {
            check(TokenType.ALPHA) -> advance().value
            check(TokenType.BQUOTE_STR) -> advance().value
            else -> throw ParseException("Expected class name")
        }
    }
    
    private fun parseNamespaceName(): String {
        val parts = mutableListOf<String>()
        
        parts.add(consume(TokenType.ALPHA, "Expected namespace name").value)
        
        while (check(TokenType.DOT)) {
            advance()
            parts.add(consume(TokenType.ALPHA, "Expected namespace name part").value)
        }
        
        return parts.joinToString(".")
    }
    
    private fun parseRelation(): RelationNode {
        var type1 = RelationType.NONE
        var type2 = RelationType.NONE
        var lineType = LineType.LINE
        
        // Parse first relation type
        if (checkRelationType()) {
            type1 = parseRelationType()
        }
        
        // Parse line type
        if (check(TokenType.LINE)) {
            lineType = LineType.LINE
            advance()
        } else if (check(TokenType.DOTTED_LINE)) {
            lineType = LineType.DOTTED_LINE
            advance()
        }
        
        // Parse second relation type
        if (checkRelationType()) {
            type2 = parseRelationType()
        }
        
        return RelationNode(type1, type2, lineType, getCurrentPosition())
    }
    
    private fun parseRelationType(): RelationType {
        return when {
            check(TokenType.EXTENSION) -> {
                advance()
                RelationType.EXTENSION
            }
            check(TokenType.DEPENDENCY) -> {
                advance()
                RelationType.DEPENDENCY
            }
            check(TokenType.COMPOSITION) -> {
                advance()
                RelationType.COMPOSITION
            }
            check(TokenType.AGGREGATION) -> {
                advance()
                RelationType.AGGREGATION
            }
            check(TokenType.LOLLIPOP) -> {
                advance()
                RelationType.LOLLIPOP
            }
            else -> throw ParseException("Expected relation type")
        }
    }
    
    private fun parseMembers(): List<MemberNode> {
        val members = mutableListOf<MemberNode>()
        
        while (!check(TokenType.STRUCT_STOP) && !isAtEnd()) {
            if (check(TokenType.MEMBER)) {
                val memberText = advance().value
                val member = parseMemberFromText(memberText)
                members.add(member)
            } else if (check(TokenType.NEWLINE)) {
                advance()
            } else {
                advance() // Skip unknown tokens
            }
        }
        
        return members
    }
    
    private fun parseMemberFromText(text: String): MemberNode {
        val trimmed = text.trim()
        
        // Parse visibility
        val visibility = when {
            trimmed.startsWith("+") -> VisibilityType.PUBLIC
            trimmed.startsWith("-") -> VisibilityType.PRIVATE
            trimmed.startsWith("#") -> VisibilityType.PROTECTED
            trimmed.startsWith("~") -> VisibilityType.PACKAGE
            else -> VisibilityType.PUBLIC
        }
        
        val withoutVisibility = if (trimmed.startsWithAny("+", "-", "#", "~")) {
            trimmed.substring(1).trim()
        } else {
            trimmed
        }
        
        // Check if it's a method
        val isMethod = withoutVisibility.contains("(")
        
        return if (isMethod) {
            parseMemberMethod(withoutVisibility, visibility)
        } else {
            parseMemberField(withoutVisibility, visibility)
        }
    }
    
    private fun parseMemberField(text: String, visibility: VisibilityType): MemberNode {
        val parts = text.split("\\s+".toRegex(), 2)
        
        return if (parts.size >= 2) {
            MemberNode(
                name = parts[1],
                type = parts[0],
                visibility = visibility,
                isMethod = false,
                position = getCurrentPosition()
            )
        } else {
            MemberNode(
                name = parts[0],
                type = null,
                visibility = visibility,
                isMethod = false,
                position = getCurrentPosition()
            )
        }
    }
    
    private fun parseMemberMethod(text: String, visibility: VisibilityType): MemberNode {
        val parenIndex = text.indexOf('(')
        val methodName = text.substring(0, parenIndex).trim()
        
        // For now, we'll keep it simple and not parse parameters
        return MemberNode(
            name = methodName,
            type = "method",
            visibility = visibility,
            isMethod = true,
            position = getCurrentPosition()
        )
    }
    
    private fun parseClassList(): List<String> {
        val classes = mutableListOf<String>()
        classes.add(consume(TokenType.ALPHA, "Expected class name").value)
        
        while (check(TokenType.COMMA)) {
            advance()
            classes.add(consume(TokenType.ALPHA, "Expected class name").value)
        }
        
        return classes
    }
    
    private fun parseStyles(): List<StyleNode> {
        // For now, return empty list - style parsing can be implemented later
        return emptyList()
    }
    
    private fun checkRelationStatement(): Boolean {
        // Look ahead to see if this looks like a relation statement
        val saved = position
        try {
            if (check(TokenType.ALPHA, TokenType.BQUOTE_STR)) {
                advance()
                
                // Skip optional string label
                if (check(TokenType.STR)) {
                    advance()
                }
                
                // Check for relation operators
                val hasRelation = checkRelationType() || check(TokenType.LINE, TokenType.DOTTED_LINE)
                position = saved
                return hasRelation
            }
        } catch (e: Exception) {
            position = saved
        }
        
        return false
    }
    
    private fun checkMemberStatement(): Boolean {
        // Look ahead to see if this looks like a member statement (ClassName : member)
        val saved = position
        try {
            if (check(TokenType.ALPHA, TokenType.BQUOTE_STR)) {
                advance()
                val hasColon = check(TokenType.COLON)
                position = saved
                return hasColon
            }
        } catch (e: Exception) {
            position = saved
        }
        
        return false
    }
    
    private fun checkRelationType(): Boolean {
        return check(TokenType.EXTENSION, TokenType.DEPENDENCY, TokenType.COMPOSITION, 
                    TokenType.AGGREGATION, TokenType.LOLLIPOP)
    }
    
    private fun check(vararg types: TokenType): Boolean {
        if (isAtEnd()) return false
        return currentToken()?.type in types
    }
    
    private fun advance(): MermaidToken {
        if (!isAtEnd()) position++
        return previous()
    }
    
    private fun isAtEnd(): Boolean = position >= tokens.size || currentToken()?.type == TokenType.EOF
    
    private fun currentToken(): MermaidToken? = if (position < tokens.size) tokens[position] else null
    
    private fun previous(): MermaidToken = tokens[position - 1]
    
    private fun consume(type: TokenType, message: String): MermaidToken {
        if (check(type)) return advance()
        throw ParseException("$message. Got ${currentToken()?.type}")
    }
    
    private fun consumeNewlines() {
        while (check(TokenType.NEWLINE)) {
            advance()
        }
    }
    
    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == TokenType.NEWLINE) return

            when (currentToken()?.type) {
                TokenType.CLASS, TokenType.NAMESPACE, TokenType.NOTE, TokenType.STYLE -> return
                else -> advance()
            }
        }
    }

    private fun getCurrentPosition(): TokenPosition? {
        return currentToken()?.let {
            TokenPosition(it.line, it.column, position)
        }
    }

    private fun String.startsWithAny(vararg chars: String): Boolean {
        return chars.any { this.startsWith(it) }
    }
}

/**
 * Parse result
 */
sealed class ParseResult {
    data class Success(val ast: ClassDiagramNode) : ParseResult()
    data class Error(val errors: List<ParseError>) : ParseResult()
}

/**
 * Parse error
 */
data class ParseError(
    val message: String,
    val position: TokenPosition?
)

/**
 * Parse exception
 */
class ParseException(message: String) : Exception(message)
