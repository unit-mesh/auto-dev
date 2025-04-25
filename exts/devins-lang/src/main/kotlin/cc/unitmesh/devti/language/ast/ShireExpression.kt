package cc.unitmesh.devti.language.ast

import cc.unitmesh.devti.language.ast.action.PatternActionFunc
import com.intellij.openapi.diagnostic.logger
import java.util.regex.Pattern

/**
 * Represents the base class for all statements.
 */
abstract class Statement {
    abstract fun evaluate(variables: Map<String, String>): Any

    fun display(): String {
        return when (this) {
            is Operator -> this.type.display
            is StringOperatorStatement -> this.type.display
            is Comparison -> "${this.left.display()} ${this.operator.display()} ${this.right.display()}"
            is StringComparison -> "${this.variable} ${this.operator.display()} ${this.value}"
            is LogicalExpression -> "${this.left.display()} ${this.operator.display} ${this.right.display()}"
            is NotExpression -> "!${this.operand.display()}"
            is MethodCall -> {
                val parameters = this.arguments?.joinToString(", ") {
                    when (it) {
                        is FrontMatterType -> it.display()
                        else -> it.toString()
                    }
                }
                val formattedParameters = if (parameters == null) "" else "($parameters)"

                val dotWithTarget = if (this.methodName is FrontMatterType.EMPTY) {
                    ""
                } else if (this.methodName is FrontMatterType.IDENTIFIER) {
                    if (this.methodName.value == "") {
                        ""
                    } else {
                        ".${this.methodName.display()}"
                    }
                } else {
                    ".${this.methodName.display()}"
                }

                "${this.objectName.display()}${dotWithTarget}$formattedParameters"
            }

            is Value -> this.value.display()
            is Processor -> this.processors.joinToString(" | ") { it.toString() }
            else -> throw IllegalArgumentException("Unsupported statement type: $this")
        }
    }
}

/**
 * This function `evaluate` is used to evaluate the `value` based on its type.
 *
 * @param variables A map of variables where the key is a `String` and the value is also a `String`.
 *
 * @return Returns the value of the `FrontMatterType` if it is one of the supported types (STRING, NUMBER, DATE, BOOLEAN).
 *
 * @throws IllegalArgumentException If the `FrontMatterType` is not one of the supported types,
 * an `IllegalArgumentException` is thrown with a message indicating the unsupported value type.
 *
 * @since 1.0
 */
data class Value(val value: FrontMatterType) : Statement() {
    override fun evaluate(variables: Map<String, String>): Any {
        return when (value) {
            is FrontMatterType.STRING -> value.value
            is FrontMatterType.NUMBER -> value.value
            is FrontMatterType.DATE -> value.value
            is FrontMatterType.BOOLEAN -> value.value
            else -> throw IllegalArgumentException("Unsupported value type: $value")
        }
    }
}

/**
 * Enumeration of operator types used in logical and comparison expressions.
 *
 * @property display The string representation of the operator.
 */
sealed class OperatorType(val display: String) {
    /** Logical OR operator (||). */
    object Or : OperatorType("||")

    /** Logical AND operator (&&). */
    object And : OperatorType("&&")

    /** Logical NOT operator (!). */
    object Not : OperatorType("!")

    /** Equality operator (==). */
    object Equal : OperatorType("==")

    /** Inequality operator (!=). */
    object NotEqual : OperatorType("!=")

    /** Less than operator (<). */
    object LessThan : OperatorType("<")

    /** Greater than operator (>). */
    object GreaterThan : OperatorType(">")

    /** Less than or equal operator (<=). */
    object LessEqual : OperatorType("<=")

    /** Greater than or equal operator (>=). */
    object GreaterEqual : OperatorType(">=")

    companion object {
        fun fromString(operator: String): OperatorType {
            return when (operator) {
                "||" -> Or
                "&&" -> And
                "!" -> Not
                "==" -> Equal
                "!=" -> NotEqual
                "<" -> LessThan
                ">" -> GreaterThan
                "<=" -> LessEqual
                ">=" -> GreaterEqual
                else -> throw IllegalArgumentException("Invalid operator: $operator")
            }
        }
    }
}

/**
 * Enumeration of string operator types used in string comparison expressions.
 *
 * @property display The string representation of the string operator.
 */
sealed class StringOperator(val display: String) {
    /** Contains operator (contains). */
    object Contains : StringOperator("contains")

    /** Starts with operator (startsWith). */
    object StartsWith : StringOperator("startsWith")

    /** Ends with operator (endsWith). */
    object EndsWith : StringOperator("endsWith")

    /** Matches regex operator (matches). */
    object Matches : StringOperator("matches")
}

/**
 * Represents an operator used in a comparison expression.
 *
 * @property type The type of operator.
 */
data class Operator(val type: OperatorType) : Statement() {
    override fun evaluate(variables: Map<String, String>) = type.display
}

/**
 * Represents a string operator used in a string comparison expression.
 *
 * @property type The type of string operator.
 */
data class StringOperatorStatement(val type: StringOperator) : Statement() {
    override fun evaluate(variables: Map<String, String>) = type.display
}

/**
 * Represents a comparison expression, including a variable, an operator, and a value.
 *
 * @property left The name of the variable being compared.
 * @property operator The operator used for comparison.
 * @property right The value being compared against.
 */
data class Comparison(
    val left: FrontMatterType,
    val operator: Operator,
    val right: FrontMatterType,
) : Statement() {
    override fun evaluate(variables: Map<String, String>): Boolean {
        val variableValue = when (left.value) {
            is MethodCall -> left.value.evaluate(variables)
            is FrontMatterType.STRING -> left.value.value
            else -> {
                logger<Comparison>().error("Variable not found: ${left.value}, will use: ${variables[left.value]}")
                variables[left.value]
            }
        }

        val value = right.value

        return when (operator.type) {
            OperatorType.Equal -> variableValue == value
            OperatorType.NotEqual -> variableValue != value
            OperatorType.LessThan -> (variableValue as Comparable<Any>) < value
            OperatorType.GreaterThan -> (variableValue as Comparable<Any>) > value
            OperatorType.LessEqual -> (variableValue as Comparable<Any>) <= value
            OperatorType.GreaterEqual -> (variableValue as Comparable<Any>) >= value
            else -> throw IllegalArgumentException("Invalid comparison operator: ${operator.type}")
        }
    }
}

/**
 * Represents a string comparison expression, including a variable, a string operator, and a value.
 *
 * @property variable The name of the variable being compared.
 * @property operator The string operator used for comparison.
 * @property value The string value being compared against.
 */
data class StringComparison(
    val variable: String,
    val operator: StringOperatorStatement,
    val value: String,
) : Statement() {
    override fun evaluate(variables: Map<String, String>): Boolean {
        return when (operator.type) {
            StringOperator.Contains -> variable.contains(value)
            StringOperator.StartsWith -> variable.startsWith(value)
            StringOperator.EndsWith -> variable.endsWith(value)
            StringOperator.Matches -> variable.matches(Pattern.compile(value).toRegex())
        }
    }
}

/**
 * Represents a logical expression, including left and right operands and an operator.
 *
 * @property left The left operand of the logical expression.
 * @property operator The logical operator used in the expression.
 * @property right The right operand of the logical expression.
 */
data class LogicalExpression(
    val left: Statement,
    val operator: OperatorType,
    val right: Statement,
) : Statement() {
    override fun evaluate(variables: Map<String, String>): Boolean {
        val leftValue = left.evaluate(variables) as Boolean
        val rightValue = right.evaluate(variables) as Boolean

        return when (operator) {
            OperatorType.And -> leftValue && rightValue
            OperatorType.Or -> leftValue || rightValue
            else -> throw IllegalArgumentException("Invalid logical operator: $operator")
        }
    }
}

/**
 * Represents a negation expression, including an operand.
 *
 * @property operand The operand to be negated.
 */
data class NotExpression(val operand: Statement) : Statement() {
    override fun evaluate(variables: Map<String, String>): Boolean {
        return !(operand.evaluate(variables) as Boolean)
    }
}

/**
 * Represents a method call expression, including the object and method being called.
 *
 * @property objectName The name of the object on which the method is called.
 * @property methodName The name of the method being called.
 * @property arguments The arguments passed to the method.
 */
data class MethodCall(
    val objectName: FrontMatterType,
    val methodName: FrontMatterType,
    val arguments: List<Any>?,
) : Statement() {
    override fun evaluate(variables: Map<String, String>): Any {
        val value = when (objectName) {
            is FrontMatterType.STRING -> variables[objectName.value]
            is FrontMatterType.VARIABLE -> variables[objectName.value]
            else -> null
        } ?: throw IllegalArgumentException("Variable not found: ${objectName.value}")

        val parameters: List<Any>? = parameters()

        return evaluateExpression(methodName, parameters, value)
    }

    fun parameters() = this.arguments?.map {
        when (it) {
            is FrontMatterType.STRING -> it.display().removeSurrounding("\"")
            is FrontMatterType.NUMBER -> it.value
            is FrontMatterType.DATE -> it.value
            is FrontMatterType -> it.display()
            else -> it.toString()
        }
    }

    fun evaluateExpression(methodNode: FrontMatterType, parameters: List<Any>?, value: String): Comparable<*> {
        val method = ExpressionBuiltInMethod.fromString(methodNode.value.toString())
        return when (method) {
            ExpressionBuiltInMethod.LENGTH -> value.length
            ExpressionBuiltInMethod.TRIM -> value.trim()
            ExpressionBuiltInMethod.CONTAINS -> {
                if (parameters != null) {
                    value.contains(parameters[0] as String)
                } else {
                    throw IllegalArgumentException("Missing parameter for method: $methodNode")
                }
            }

            ExpressionBuiltInMethod.STARTS_WITH -> {
                if (parameters != null) {
                    value.startsWith(parameters[0] as String)
                } else {
                    throw IllegalArgumentException("Missing parameter for method: $methodNode")
                }
            }

            ExpressionBuiltInMethod.ENDS_WITH -> {
                if (parameters != null) {
                    value.endsWith(parameters[0] as String)
                } else {
                    throw IllegalArgumentException("Missing parameter for method: $methodNode")
                }
            }

            ExpressionBuiltInMethod.LOWERCASE -> value.lowercase()
            ExpressionBuiltInMethod.UPPERCASE -> value.uppercase()
            ExpressionBuiltInMethod.IS_EMPTY -> value.isEmpty()
            ExpressionBuiltInMethod.IS_NOT_EMPTY -> value.isNotEmpty()
            ExpressionBuiltInMethod.FIRST -> value.first().toString()
            ExpressionBuiltInMethod.LAST -> value.last().toString()
            ExpressionBuiltInMethod.MATCHES -> {
                if (parameters != null) {
                    value.matches((parameters[0] as String).toRegex())
                } else {
                    throw IllegalArgumentException("Missing parameter for method: $methodNode")
                }
            }

            else -> throw IllegalArgumentException("Unsupported method: $methodNode")
        }
    }
}

data class Processor(
    val processors: List<PatternActionFunc>,
) : Statement() {
    override fun evaluate(variables: Map<String, String>): List<PatternActionFunc> {
        return processors
    }
}

data class CaseKeyValue(
    val key: FrontMatterType,
    val value: FrontMatterType.EXPRESSION,
) : Statement() {
    override fun evaluate(variables: Map<String, String>): Any {
        return key.display() to value
    }
}

data class ForeignFunctionStmt(
    val funcName: String,
    val funcPath: String,
    val accessFuncName: String,
    val inputTypes: List<String>,
    val returnVars: Map<String, Any>,
) : Statement() {
    override fun evaluate(variables: Map<String, String>): ForeignFunction {
        return ForeignFunction(funcName, funcPath, accessFuncName, inputTypes, returnVars)
    }
}

/**
 * Switch case
 */
data class ConditionCase(
    val conditions: List<FrontMatterType>,
    val cases: List<FrontMatterType>,
) : Statement() {
    override fun evaluate(variables: Map<String, String>): Any {
        val condition = conditions.map { it.display() }
        val case = cases.map { it.display() }

        return condition to case
    }
}