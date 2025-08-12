package cc.unitmesh.diagram.graphviz.parser.mermaid

/**
 * Abstract Syntax Tree nodes for Mermaid class diagrams
 * Based on the grammar rules in classDiagram.jison
 */

/**
 * Base interface for all AST nodes
 */
interface AstNode {
    val position: TokenPosition?
}

/**
 * Root node representing the entire class diagram
 */
data class ClassDiagramNode(
    val statements: List<StatementNode>,
    override val position: TokenPosition? = null
) : AstNode

/**
 * Base interface for all statement nodes
 */
interface StatementNode : AstNode

/**
 * Class definition statement
 */
data class ClassStatementNode(
    val className: String,
    val classLabel: String? = null,
    val members: List<MemberNode> = emptyList(),
    val cssClass: String? = null,
    override val position: TokenPosition? = null
) : StatementNode

/**
 * Namespace definition statement
 */
data class NamespaceStatementNode(
    val namespaceName: String,
    val classes: List<ClassStatementNode>,
    override val position: TokenPosition? = null
) : StatementNode

/**
 * Relationship statement between classes
 */
data class RelationStatementNode(
    val sourceClass: String,
    val targetClass: String,
    val relation: RelationNode,
    val sourceLabel: String? = null,
    val targetLabel: String? = null,
    val relationLabel: String? = null,
    override val position: TokenPosition? = null
) : StatementNode

/**
 * Member statement (field or method)
 */
data class MemberStatementNode(
    val className: String,
    val member: MemberNode,
    override val position: TokenPosition? = null
) : StatementNode

/**
 * Annotation statement
 */
data class AnnotationStatementNode(
    val className: String,
    val annotation: String,
    override val position: TokenPosition? = null
) : StatementNode

/**
 * Note statement
 */
data class NoteStatementNode(
    val noteText: String,
    val forClass: String? = null,
    override val position: TokenPosition? = null
) : StatementNode

/**
 * Style statement
 */
data class StyleStatementNode(
    val className: String,
    val styles: List<StyleNode>,
    override val position: TokenPosition? = null
) : StatementNode

/**
 * Class definition statement
 */
data class ClassDefStatementNode(
    val classNames: List<String>,
    val styles: List<StyleNode>,
    override val position: TokenPosition? = null
) : StatementNode

/**
 * Click statement for interactivity
 */
data class ClickStatementNode(
    val className: String,
    val action: ClickActionNode,
    override val position: TokenPosition? = null
) : StatementNode

/**
 * Direction statement
 */
data class DirectionStatementNode(
    val direction: DirectionType,
    override val position: TokenPosition? = null
) : StatementNode

/**
 * Accessibility statements
 */
data class AccessibilityStatementNode(
    val type: AccessibilityType,
    val value: String,
    override val position: TokenPosition? = null
) : StatementNode

/**
 * Class member (field or method)
 */
data class MemberNode(
    val name: String,
    val type: String? = null,
    val visibility: VisibilityType = VisibilityType.PUBLIC,
    val isMethod: Boolean = false,
    val parameters: List<ParameterNode> = emptyList(),
    val returnType: String? = null,
    override val position: TokenPosition? = null
) : AstNode

/**
 * Method parameter
 */
data class ParameterNode(
    val name: String,
    val type: String? = null,
    override val position: TokenPosition? = null
) : AstNode

/**
 * Relationship definition
 */
data class RelationNode(
    val type1: RelationType,
    val type2: RelationType,
    val lineType: LineType,
    override val position: TokenPosition? = null
) : AstNode

/**
 * Style definition
 */
data class StyleNode(
    val property: String,
    val value: String,
    override val position: TokenPosition? = null
) : AstNode

/**
 * Click action
 */
sealed class ClickActionNode : AstNode {
    data class CallbackAction(
        val callbackName: String,
        val args: String? = null,
        val tooltip: String? = null,
        override val position: TokenPosition? = null
    ) : ClickActionNode()
    
    data class LinkAction(
        val url: String,
        val target: String? = null,
        val tooltip: String? = null,
        override val position: TokenPosition? = null
    ) : ClickActionNode()
}

/**
 * Enums for various types
 */
enum class VisibilityType(val symbol: String) {
    PUBLIC("+"),
    PRIVATE("-"),
    PROTECTED("#"),
    PACKAGE("~")
}

enum class RelationType {
    NONE,
    AGGREGATION,
    EXTENSION,
    COMPOSITION,
    DEPENDENCY,
    LOLLIPOP
}

enum class LineType {
    LINE,
    DOTTED_LINE
}

enum class DirectionType {
    TB, BT, RL, LR
}

enum class AccessibilityType {
    TITLE,
    DESCRIPTION,
    DESCRIPTION_MULTILINE
}
