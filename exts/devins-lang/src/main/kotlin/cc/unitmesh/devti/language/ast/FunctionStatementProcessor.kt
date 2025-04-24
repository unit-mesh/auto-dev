package cc.unitmesh.devti.language.ast

import cc.unitmesh.devti.language.ast.action.PatternActionFunc
import cc.unitmesh.devti.language.ast.action.PatternFuncProcessor
import cc.unitmesh.devti.language.ast.shireql.ShireQLSchema
import cc.unitmesh.devti.language.ast.shireql.ShireQLVariableBuilder
import cc.unitmesh.devti.language.ast.shireql.VariableContainerManager
import cc.unitmesh.devti.language.ast.shireql.variable.vcs.ShireGitCommit
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jayway.jsonpath.JsonPath
import cc.unitmesh.devti.language.ast.shireql.ShireDateSchema
import kotlinx.coroutines.runBlocking

/**
 * The `FunctionStatementProcessor` class is responsible for processing function statements within a project context.
 * It extends the `PatternFuncProcessor` class, which is part of a larger framework likely dealing with pattern matching and processing within a domain-specific language or a scripting environment.
 *
 * This class operates on statements that can be comparisons, processor invocations, or method calls, and it manages a variable table to keep track of variable values during execution.
 *
 * @property myProject The project in which the processing occurs, extending the functionality of the base class `PatternFuncProcessor`.
 * @property hole The hobbit hole, which seems to be a metaphorical representation of a context or a scope, also extending the base class functionality.
 *
 * ### Methods:
 *
 * This class uses the Kotlin `runBlocking` coroutine scope to handle asynchronous operations and may throw an `IllegalArgumentException` for unknown types during evaluation.
 */
open class FunctionStatementProcessor(override val myProject: Project, override val hole: HobbitHole) :
    PatternFuncProcessor(myProject, hole) {
    open fun execute(transform: VariableTransform): String {
        val fromStmt = transform.patternActionFuncs.find { it is PatternActionFunc.From } as PatternActionFunc.From
        val selectStmt =
            transform.patternActionFuncs.find { it is PatternActionFunc.Select } as PatternActionFunc.Select
        val whereStmt = transform.patternActionFuncs.find { it is PatternActionFunc.Where } as PatternActionFunc.Where

        val variableElementsMap: Map<String, List<Any>> = runReadAction {
            ShireQLVariableBuilder(myProject, hole).buildVariables(fromStmt)
        }

        val valuedType = evalValueByElementStmt(variableElementsMap, whereStmt.statement)

        val handledElements = processStatement(whereStmt.statement, variableElementsMap, valuedType)
        val selectElements = processSelect(selectStmt, handledElements)

        return selectElements.joinToString("\n")
    }

    fun processSelect(selectStmt: PatternActionFunc.Select, handledElements: List<Any>): List<String> {
        return selectStmt.statements.flatMap {
            processSelectStatement(it, handledElements)
        }
    }

    private fun processSelectStatement(statement: Statement, handledElements: List<Any>): List<String> {
        val result = mutableListOf<String>()
        handledElements.forEach { element ->
            when (element) {
                is PsiElement -> {
                    when (statement) {
                        is Value -> {
                            result.add(statement.display())
                        }

                        is MethodCall -> {
                            invokeMethodOrField(statement, element)?.let {
                                result.add(it.toString())
                            }
                        }
                    }
                }

                is ShireGitCommit -> {
                    when (statement) {
                        is Value -> {
                            result.add(statement.display())
                        }

                        is MethodCall -> {
                            invokeMethodOrField(statement, element)?.let {
                                result.add(it.toString())
                            }
                        }
                    }
                }

                is ShireDateSchema -> {
                    when (statement) {
                        is Value -> {
                            invokeMethod(element, null)?.let {
                                result.add(it.toString())
                            }
                        }

                        is MethodCall -> {
                            val methodName = statement.methodName.display()
                            val args = statement.arguments
                            invokeMethod(element, methodName, args)?.let {
                                result.add(it.toString())
                            }
                        }
                    }
                }

                else -> {
                    logger<FunctionStatementProcessor>().error("unknown element: $element")
                }
            }
        }

        return result
    }

    fun execute(statement: Statement, variableTable: MutableMap<String, Any?>): Any? = runBlocking {
        return@runBlocking when (statement) {
            is Comparison -> {
                executeComparison(statement, variableTable)
            }

            is Processor -> {
                execute(statement.processors, variableTable)
            }

            is MethodCall -> {
                invokeLocalMethodCall(statement, variableTable)
            }

            is Value -> {
                statement.value
            }

            else -> {
                logger<FunctionStatementProcessor>().error("unknown stmt: $statement expr: ${statement.display()}")
                null
            }
        }
    }

    private fun invokeLocalMethodCall(statement: MethodCall, variableTable: MutableMap<String, Any?>): Any? {
        val objName = statement.objectName.display()
        val methodName = statement.methodName.display()
        val methodArgs = statement.arguments

        if (methodName == "") {
            val firstArg = methodArgs?.get(0)
            when (objName) {
                "jsonpath" -> {
                    val output = (variableTable["output"] ?: "").toString()
                    val arg: String = when (firstArg) {
                        is FrontMatterType.STRING -> (methodArgs[0] as FrontMatterType.STRING).value.toString()
                        else -> firstArg.toString()
                    }
                    val string: String = try {
                        JsonPath.parse(output)?.read<Any>(arg).toString()
                    } catch (e: Exception) {
                        logger<FunctionStatementProcessor>().warn("jsonpath error: $e")
                        return null
                    }

                    return string
                }

                "print" -> {
                    println(firstArg)
                }

                else -> {
                    logger<FunctionStatementProcessor>().warn("unknown method: $objName")
                }
            }
        }

        return null
    }

    suspend fun execute(processors: List<PatternActionFunc>, variableTable: MutableMap<String, Any?>): Any? {
        val input: Any = variableTable["output"] ?: ""
        var result: Any = variableTable["output"] ?: ""

        var lastOutput: Any? = result

        processors.forEach { action ->
            result = patternFunctionExecute(action, result, input, variableTable)

            if (action.funcName == "execute") {
                if (lastOutput != null) {
                    result = lastOutput as Any
                }
            }

            lastOutput = result
            variableTable["output"] = result
        }

        return result.toString()
    }

    private fun FunctionStatementProcessor.executeComparison(
        statement: Comparison,
        value: Any,
    ): Boolean {
        val operator = statement.operator
        val left = evaluate(statement.left, value)
        val right = evaluate(statement.right, value)

        return when (operator.type) {
            OperatorType.Equal -> left == right
            OperatorType.And -> left == right
            OperatorType.NotEqual -> left != right
            OperatorType.Or -> left == true || right == true

            OperatorType.GreaterEqual -> {
                if (left == null || right == null) {
                    false
                } else {
                    left as Comparable<Any> >= right as Comparable<Any>
                }
            }

            OperatorType.GreaterThan -> {
                if (left == null || right == null) {
                    false
                } else {
                    left as Comparable<Any> > right as Comparable<Any>
                }
            }

            OperatorType.LessEqual -> {
                if (left == null || right == null) {
                    false
                } else {
                    left as Comparable<Any> <= right as Comparable<Any>
                }
            }

            OperatorType.LessThan -> {
                if (left == null || right == null) {
                    false
                } else {
                    left as Comparable<Any> < right as Comparable<Any>
                }
            }

            else -> {
                logger<FunctionStatementProcessor>().warn("unknown operator: $operator")
                false
            }
        }
    }

    /// todo: fix this for multiple variables
    private fun processStatement(
        statement: Statement,
        variableElementsMap: Map<String, List<Any>>,
        valuedType: VariableContainerManager,
    ): List<Any> {
        val result = mutableListOf<Any>()
        variableElementsMap.forEach { (variableName, elements) ->
            elements.forEach { element ->
                when (statement) {
                    is Comparison -> {
                        val operator = statement.operator
                        /// for commit.authorDate <= date.now()
                        /// if we use element (Date) for commit.authorDate, will be null, should use element (ShireGitCommit)
                        val left = valuedType.getValue(element, statement.left) ?: evaluate(statement.right, element)
                        val right = valuedType.getValue(element, statement.right) ?: evaluate(statement.right, element)

                        if (left == null) {
                            logger<FunctionStatementProcessor>().warn("left is null: ${statement.left.display()}")
                            return@forEach
                        }

                        if (right == null) {
                            logger<FunctionStatementProcessor>().warn("right is null: ${statement.right.display()}")
                            return@forEach
                        }

                        when (operator.type) {
                            OperatorType.Equal -> if (left == right) result.add(element)
                            OperatorType.And -> if (left == right) result.add(element)
                            OperatorType.NotEqual -> if (left != right) result.add(element)
                            OperatorType.Or -> if (left == true || right == true) result.add(element)
                            OperatorType.GreaterEqual -> {
                                if (left as Comparable<Any> >= right as Comparable<Any>) {
                                    result.add(element)
                                }
                            }

                            OperatorType.GreaterThan -> {
                                if (left as Comparable<Any> > right as Comparable<Any>) {
                                    result.add(element)
                                }
                            }

                            OperatorType.LessEqual -> {
                                if (left as Comparable<Any> <= right as Comparable<Any>) {
                                    result.add(element)
                                }
                            }

                            OperatorType.LessThan -> {
                                if (left as Comparable<Any> < right as Comparable<Any>) {
                                    result.add(element)
                                }
                            }

                            else -> {
                                logger<FunctionStatementProcessor>().warn("unknown operator: $operator")
                            }
                        }
                    }

                    is MethodCall -> {
                        when (val output = invokeMethodOrField(statement, element)) {
                            is Collection<*> -> {
                                output.forEach {
                                    if (it is Any) {
                                        result.add(it)
                                    }
                                }
                            }

                            is Any -> {
                                result.add(output)
                            }
                        }
                    }

                    is LogicalExpression -> {
                        val left = processStatement(statement.left, variableElementsMap, valuedType)
                        val right = processStatement(statement.right, variableElementsMap, valuedType)

                        when (statement.operator) {
                            OperatorType.And -> {
                                if (left.isNotEmpty() && right.isNotEmpty()) {
                                    result.add(element)
                                }
                            }

                            OperatorType.Or -> {
                                if (left.isNotEmpty() || right.isNotEmpty()) {
                                    result.add(element)
                                }
                            }

                            else -> {
                                logger<FunctionStatementProcessor>().warn("unknown operator: ${statement.operator}")
                            }
                        }
                    }

                    else -> {
                        logger<FunctionStatementProcessor>().warn("unknown statement: ${statement.display()}")
                    }
                }
            }
        }

        return result
    }

    /// a dirty implementation for multiple variables
    private fun evalValueByElementStmt(
        variableElementsMap: Map<String, List<Any>>,
        statement: Statement,
    ): VariableContainerManager {
        val typeValued = VariableContainerManager()
        variableElementsMap.forEach { (variableName, elements) ->
            elements.forEach { element ->
                when (statement) {
                    is Comparison -> {
                        evaluateComparison(element, typeValued, statement.left, statement.right)
                    }

                    is LogicalExpression -> {
                        val left = evalValueByElementStmt(variableElementsMap, statement.left)
                        val right = evalValueByElementStmt(variableElementsMap, statement.right)

                        when (statement.operator) {
                            OperatorType.And -> {
                                if (left.isNotEmpty() && right.isNotEmpty()) {
                                    left.variables.forEach { evaluatorEntry ->
                                        evaluatorEntry.value.valued.forEach { (key, value) ->
                                            if (value != null) {
                                                typeValued.putValue(element, key, value)
                                            }
                                        }
                                    }
                                    right.variables.forEach { evaluatorEntry ->
                                        evaluatorEntry.value.valued.forEach { (key, value) ->
                                            if (value != null) {
                                                typeValued.putValue(element, key, value)
                                            }
                                        }
                                    }
                                }
                            }

                            OperatorType.Or -> {
                                if (left.isNotEmpty() || right.isNotEmpty()) {
                                    left.variables.forEach { evaluatorEntry ->
                                        evaluatorEntry.value.valued.forEach { (key, value) ->
                                            if (value != null) {
                                                typeValued.putValue(element, key, value)
                                            }
                                        }
                                    }
                                    right.variables.forEach { evaluatorEntry ->
                                        evaluatorEntry.value.valued.forEach { (key, value) ->
                                            if (value != null) {
                                                typeValued.putValue(element, key, value)
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                logger<FunctionStatementProcessor>().warn("unknown operator: ${statement.operator}")
                            }
                        }
                    }

                    else -> {
                        logger<FunctionStatementProcessor>().warn("unknown statement: ${statement.display()}")
                    }
                }
            }
        }

        return typeValued
    }

    private fun evaluateComparison(
        element: Any,
        typeValued: VariableContainerManager,
        leftStmt: FrontMatterType,
        rightStmt: FrontMatterType,
    ) {
        if (element is ShireQLSchema) {
            val left = evaluate(leftStmt, element)
            if (left != null) {
                typeValued.putValue(element, leftStmt, left)
            } else {
                val right = evaluate(rightStmt, element)
                if (right != null) {
                    typeValued.putValue(element, rightStmt, right)
                }
            }

            return
        }

        val left = evaluate(leftStmt, element)
        if (left != null) {
            typeValued.putValue(element, leftStmt, left)
        } else {
            val right = evaluate(rightStmt, element)
            if (right != null) {
                typeValued.putValue(element, rightStmt, right)
            }
        }
    }


    fun <T : Any> evaluate(type: FrontMatterType, element: T): Any? {
        return when (type) {
            is FrontMatterType.ARRAY -> {
                (type.value as List<FrontMatterType>).map {
                    evaluate(it, element)
                }
            }

            is FrontMatterType.EXPRESSION -> {
                evalExpression(type, element)
            }

            is FrontMatterType.BOOLEAN,
            is FrontMatterType.DATE,
            is FrontMatterType.IDENTIFIER,
            is FrontMatterType.STRING,
                -> {
                type.value
            }

            is FrontMatterType.NUMBER -> {
                (type.value as Int).toLong()
            }

            else -> {
                throw IllegalArgumentException("unknown type: $type")
            }
        }
    }

    open fun <T : Any> evalExpression(type: FrontMatterType, element: T): Any? {
        when (type.value) {
            is MethodCall -> {
                return invokeMethodOrField(type.value, element)
            }

            else -> {
                throw IllegalArgumentException("unknown type: $type")
            }
        }
    }

    open fun <T : Any> invokeMethodOrField(methodCall: MethodCall, element: T): Any? {
        val objName = methodCall.objectName.display()
        when (element) {
            is Map<*, *> -> {
                val variable = element[objName] as? String ?: return null
                return methodCall.evaluateExpression(methodCall.methodName, listOf(), variable)
            }

            else -> return null
        }
    }

    open fun invokeMethod(element: ShireDateSchema, methodName: String?, args: List<Any>? = null): Any? {
        val allMethods = element.javaClass.methods
        val method = allMethods.find {
            it.name == methodName
        }
        if (method != null) {
            return method.invoke(element)
        }

        return element.toString()
    }
}