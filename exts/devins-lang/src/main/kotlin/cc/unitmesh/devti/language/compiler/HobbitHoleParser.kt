package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.ast.CaseKeyValue
import cc.unitmesh.devti.language.ast.Comparison
import cc.unitmesh.devti.language.ast.ConditionCase
import cc.unitmesh.devti.language.ast.ForeignFunctionStmt
import cc.unitmesh.devti.language.ast.FrontMatterType
import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.ast.LogicalExpression
import cc.unitmesh.devti.language.ast.MethodCall
import cc.unitmesh.devti.language.ast.Operator
import cc.unitmesh.devti.language.ast.OperatorType
import cc.unitmesh.devti.language.ast.Processor
import cc.unitmesh.devti.language.ast.Statement
import cc.unitmesh.devti.language.ast.Value
import cc.unitmesh.devti.language.ast.action.PatternActionFunc
import cc.unitmesh.devti.language.ast.action.RuleBasedPatternAction
import cc.unitmesh.devti.language.ast.shireql.ShireAstQLParser
import cc.unitmesh.devti.language.psi.DevInFrontMatterHeader
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.*
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType

object HobbitHoleParser {
    private val logger = logger<HobbitHoleParser>()

    fun hasFrontMatter(file: DevInFile): Boolean {
        return PsiTreeUtil.getChildrenOfTypeAsList(file, DevInFrontMatterHeader::class.java).isNotEmpty()
    }

    fun frontMatterOffset(file: DevInFile): Int {
        val frontMatterHeader = PsiTreeUtil.getChildrenOfTypeAsList(file, DevInFrontMatterHeader::class.java).firstOrNull()
        return frontMatterHeader?.textOffset ?: 0
    }

    /**
     * Parses the given DevInFrontMatterHeader and returns a FrontMatterDevInConfig object.
     *
     * @param psiElement the DevInFrontMatterHeader to be parsed
     * @return a FrontMatterDevInConfig object if parsing is successful, null otherwise
     */
    fun parse(psiElement: DevInFrontMatterHeader): HobbitHole? {
        return psiElement.children.firstOrNull()?.let {
            val fm = processFrontMatter(it.children)
            HobbitHole.from(fm)
        }
    }

    fun parse(file: DevInFile): HobbitHole? {
        return runReadAction {
            PsiTreeUtil.getChildrenOfTypeAsList(file, DevInFrontMatterHeader::class.java)?.firstOrNull()?.let {
                parse(it)
            }
        }
    }

    private fun processFrontMatter(frontMatterEntries: Array<PsiElement>): MutableMap<String, FrontMatterType> {
        val frontMatter: MutableMap<String, FrontMatterType> = mutableMapOf()
        var lastKey = ""

        frontMatterEntries.forEach { entry ->
            entry.children.forEach { child ->
                val text = runReadAction { child.text }
                when (child.elementType) {
                    DevInTypes.LIFECYCLE_ID,
                    DevInTypes.FRONT_MATTER_KEY,
                        -> {
                        lastKey = text
                    }

                    DevInTypes.FRONT_MATTER_VALUE -> {
                        frontMatter[lastKey] = parseFrontMatterValue(child)
                            ?: FrontMatterType.STRING("FrontMatter value parsing failed: $text")
                    }

                    DevInTypes.PATTERN_ACTION -> {
                        frontMatter[lastKey] = parsePatternAction(child)
                            ?: FrontMatterType.STRING("Pattern action parsing failed: $text")
                    }

                    DevInTypes.LOGICAL_AND_EXPR -> {
                        frontMatter[lastKey] = parseLogicAndExprToType(child as DevInLogicalAndExpr)
                            ?: FrontMatterType.STRING("Logical expression parsing failed: ${text}")
                    }

                    DevInTypes.LOGICAL_OR_EXPR -> {
                        frontMatter[lastKey] = parseLogicOrExprToType(child as DevInLogicalOrExpr)
                            ?: FrontMatterType.STRING("Logical expression parsing failed: ${text}")
                    }

                    DevInTypes.CALL_EXPR -> {
                        parseExpr(child)?.let {
                            frontMatter[lastKey] = FrontMatterType.EXPRESSION(it)
                        }
                    }

                    DevInTypes.FUNCTION_STATEMENT -> {
                        frontMatter[lastKey] = parseFunction(child as DevInFunctionStatement)
                    }

                    /**
                     * For blocked when condition
                     *
                     * ```shire
                     * when: { $filePath.contains("src/main/java") && $fileName.contains(".java") }
                     * ```
                     */
                    DevInTypes.VARIABLE_EXPR -> {
                        val childExpr = (child as? DevInVariableExpr)?.expr
                        if (childExpr != null) {
                            parseExpr(childExpr)?.let {
                                frontMatter[lastKey] = FrontMatterType.EXPRESSION(it)
                            }
                        }
                    }

                    DevInTypes.LITERAL_EXPR -> {
                        parseExpr(child)?.let {
                            frontMatter[lastKey] = FrontMatterType.EXPRESSION(it)
                        }
                    }

                    DevInTypes.FOREIGN_FUNCTION -> {
                        parseForeignFunc(lastKey, child as DevInForeignFunction)?.let {
                            frontMatter[lastKey] = FrontMatterType.EXPRESSION(it)
                        }
                    }

                    else -> {
                        logger.warn("processFrontMatter, Unknown FrontMatter type: ${child.elementType}, value: $child")
                    }
                }
            }
        }

        return frontMatter
    }

    private fun parseForeignFunc(fmKey: String, function: DevInForeignFunction): Statement {
        val funcPath = function.foreignPath.quoteString.text.removeSurrounding("\"")
        val accessFuncName = function.foreignFuncName?.text ?: ""
        val inputTypes = function.foreignTypeList.map { it.text }
        val returnVars = function.foreignOutput?.children?.associate { it.text to "" } ?: emptyMap()

        return ForeignFunctionStmt(fmKey, funcPath, accessFuncName, inputTypes, returnVars)
    }

    private fun parseFunction(statement: DevInFunctionStatement): FrontMatterType {
        return when (val body = statement.functionBody?.firstChild) {
            is DevInQueryStatement -> {
                ShireAstQLParser.parse(body)
            }

            is DevInActionBody -> {
                val expressions = body.actionExprList.mapNotNull {
                    parseExpr(it)
                }.map {
                    FrontMatterType.EXPRESSION(it)
                }

                FrontMatterType.ARRAY(expressions)
            }

            null -> {
                FrontMatterType.EMPTY()
            }

            else -> {
                val expr = parseExpr(body)
                if (expr is Statement) {
                    return FrontMatterType.EXPRESSION(expr)
                }

                logger.error("parseFunction, Unknown function type: ${body.elementType}")
                FrontMatterType.STRING("Unknown function type: ${body.elementType}")
            }
        }
    }

    private fun parseLogicAndExprToType(child: DevInLogicalAndExpr): FrontMatterType? {
        val logicalExpression = parseLogicAndExpr(child) ?: return null
        return FrontMatterType.EXPRESSION(logicalExpression)
    }

    private fun parseLogicAndExpr(child: DevInLogicalAndExpr): LogicalExpression? {
        val left = child.exprList?.firstOrNull() ?: return null
        val right = child.exprList?.lastOrNull() ?: return null

        val leftStmt = parseExpr(left) ?: return null
        val rightStmt = parseExpr(right) ?: return null

        val logicalExpression = LogicalExpression(
            left = leftStmt,
            operator = OperatorType.And,
            right = rightStmt
        )

        return logicalExpression
    }

    private fun parseLogicOrExprToType(child: DevInLogicalOrExpr): FrontMatterType? {
        val logicOrExpr = parseLogicOrExpr(child) ?: return null
        return FrontMatterType.EXPRESSION(logicOrExpr)
    }

    private fun parseLogicOrExpr(child: DevInLogicalOrExpr): LogicalExpression? {
        val left = child.exprList.firstOrNull() ?: return null
        val right = child.exprList.lastOrNull() ?: return null

        val leftStmt = parseExpr(left) ?: return null
        val rightStmt = parseExpr(right) ?: return null

        val logicOrExpr = LogicalExpression(
            left = leftStmt,
            operator = OperatorType.Or,
            right = rightStmt
        )
        return logicOrExpr
    }

    /**
     * This function is used to parse an expression of type PsiElement into a Statement. The type of Statement returned depends on the type of the expression.
     *
     * @param expr The PsiElement expression to be parsed. This expression can be of type CALL_EXPR, EQ_COMPARISON_EXPR, INEQ_COMPARISON_EXPR, or any other type.
     *
     * If the expression is of type CALL_EXPR, the function finds the first child of type DevInExpr and builds a method call with the found DevInExpr and the list of expressions in the DevInCallExpr.
     *
     * If the expression is of type EQ_COMPARISON_EXPR, the function parses the first and last child of the expression into a Comparison statement with an equal operator.
     *
     * If the expression is of type INEQ_COMPARISON_EXPR, the function parses the first and last child of the expression into a Comparison statement with an operator determined by the ineqComparisonOp text of the DevInIneqComparisonExpr.
     *
     * If the expression is of any other type, the function logs a warning and returns a Comparison statement with an equal operator and empty string operands.
     *
     * @return A Statement parsed from the given expression. The type of Statement depends on the type of the expression.
     */
    fun parseExpr(expr: PsiElement): Statement? = when (expr) {
        is DevInCallExpr -> {
            val expressionList = expr.expressionList
            val hasParentheses = expressionList?.prevSibling?.text == "("

            buildMethodCall(expr.refExpr, expressionList?.children, hasParentheses)
        }

        is DevInEqComparisonExpr -> {
            val variable = parseRefExpr(expr.children.firstOrNull())
            val value = parseRefExpr(expr.children.lastOrNull())
            Comparison(variable, Operator(OperatorType.Equal), value)
        }

        is DevInIneqComparisonExpr -> {
            val variable = parseRefExpr(expr.children.firstOrNull())
            val value = parseRefExpr(expr.children.lastOrNull())
            val operatorType = OperatorType.fromString(expr.ineqComparisonOp.text)
            Comparison(variable, Operator(operatorType), value)
        }

        is DevInLogicalAndExpr -> {
            parseLogicAndExpr(expr)
                ?: Comparison(FrontMatterType.STRING(""), Operator(OperatorType.Equal), FrontMatterType.STRING(""))
        }

        is DevInLogicalOrExpr -> {
            parseLogicOrExpr(expr)
                ?: Comparison(FrontMatterType.STRING(""), Operator(OperatorType.Equal), FrontMatterType.STRING(""))
        }

        is DevInRefExpr -> {
            if (expr.expr == null) {
                Value(FrontMatterType.IDENTIFIER(expr.identifier.text))
            } else {
                val methodCall = buildMethodCall(expr, null, false)
                methodCall
            }
        }

        is DevInLiteralExpr -> {
            Value(parseLiteral(expr))
        }

        is DevInActionExpr -> {
            when (expr.firstChild) {
                is DevInFuncCall -> {
                    val args = parseParameters(expr.funcCall)
                    MethodCall(FrontMatterType.IDENTIFIER(expr.funcCall!!.funcName.text), FrontMatterType.EMPTY(), args)
                }

                is DevInCaseBody -> {
                    parseExprCaseBody(expr.firstChild as DevInCaseBody)
                }

                else -> {
                    logger.warn("parseExpr, Unknown action expression type: ${expr.firstChild.elementType}")
                    null
                }
            }
        }

        is DevInConditionStatement -> {
            val condition = parseLiteral(expr.caseCondition)
            val body = parseRefExpr(expr.expr)

            when (body) {
                is FrontMatterType.EXPRESSION -> {
                    CaseKeyValue(condition, body)
                }

                is FrontMatterType.STRING -> {
                    CaseKeyValue(condition, FrontMatterType.EXPRESSION(Value(body)))
                }

                else -> {
                    logger.warn("parseExpr, Unknown condition type: ${expr.expr?.elementType}")
                    null
                }
            }
        }

        else -> {
            logger.warn("parseExpr, Unknown expression type: ${expr.elementType}")
            null
        }
    }

    private fun parseExprCaseBody(caseBody: DevInCaseBody): ConditionCase? {
        val condition = caseBody.conditionFlag?.conditionStatementList?.mapNotNull {
            val condition = parseExpr(it)
            if (condition != null) {
                FrontMatterType.EXPRESSION(condition)
            } else {
                logger.warn("parseExprCaseBody, Unknown condition type: ${it.elementType}")
                null
            }
        } ?: emptyList()

        val body = caseBody.casePatternActionList.mapNotNull {
            val key = parseLiteral(it.caseCondition)
            val processor = parseActionBodyFuncCall(it.actionBody.actionExprList)
            FrontMatterType.EXPRESSION(CaseKeyValue(key, FrontMatterType.EXPRESSION(processor)))
        }

        return ConditionCase(condition, body)
    }

    private fun parseRefExpr(expr: PsiElement?): FrontMatterType {
        return when (expr) {
            is DevInLiteralExpr -> {
                parseLiteral(expr)
            }

            // fake refExpr ::= expr? '.' IDENTIFIER
            is DevInRefExpr -> {
                if (expr.expr == null) {
                    FrontMatterType.IDENTIFIER(expr.identifier.text)
                } else {
                    val methodCall = buildMethodCall(expr, null, false)
                    FrontMatterType.EXPRESSION(methodCall)
                }
            }

            is DevInCallExpr -> {
                val expressionList = expr.expressionList
                val hasParentheses = expressionList?.prevSibling?.text == "("

                val methodCall = buildMethodCall(expr.refExpr, expressionList?.children, hasParentheses)
                FrontMatterType.EXPRESSION(methodCall)
            }

            is DevInIneqComparisonExpr -> {
                val variable = parseRefExpr(expr.children.firstOrNull())
                val value = parseRefExpr(expr.children.lastOrNull())
                val operator = Operator(OperatorType.fromString(expr.ineqComparisonOp.text))

                val comparison = Comparison(variable, operator, value)
                FrontMatterType.EXPRESSION(comparison)
            }

            is DevInLogicalAndExpr -> {
                parseLogicAndExpr(expr)?.let {
                    FrontMatterType.EXPRESSION(it)
                } ?: FrontMatterType.ERROR("cannot parse DevInLogicalAndExpr: ${expr.text}")
            }

            is DevInLogicalOrExpr -> {
                parseLogicOrExpr(expr)?.let {
                    FrontMatterType.EXPRESSION(it)
                } ?: FrontMatterType.ERROR("cannot parse DevInLogicalOrExpr: ${expr.text}")
            }

            is DevInEqComparisonExpr -> {
                val variable = parseRefExpr(expr.children.firstOrNull())
                val value = parseRefExpr(expr.children.lastOrNull())
                val operator = Operator(OperatorType.Equal)

                val comparison = Comparison(variable, operator, value)
                FrontMatterType.EXPRESSION(comparison)
            }

            else -> {
                logger.warn("parseRefExpr, Unknown expression type: ${expr?.elementType}")
                FrontMatterType.STRING("")
            }
        }
    }


    private fun buildMethodCall(
        refExpr: DevInRefExpr,
        expressionList: Array<PsiElement>?,
        hasParentheses: Boolean,
    ): MethodCall {
        val left = if (refExpr.expr == null) {
            FrontMatterType.IDENTIFIER(refExpr.identifier.text)
        } else {
            parseRefExpr(refExpr.expr)
        }

        val id = refExpr.expr?.nextSibling?.nextSibling
        val right = FrontMatterType.IDENTIFIER(id?.text ?: "")

        var args = expressionList?.map {
            parseRefExpr(it)
        }

        // fix for () lost in display()
        if (hasParentheses && args == null) {
            args = emptyList()
        }

        return MethodCall(left, right, args)
    }

    private fun parseLiteral(ref: PsiElement): FrontMatterType {
        val firstChild = ref.firstChild
        return when (firstChild.elementType) {
            DevInTypes.IDENTIFIER -> {
                FrontMatterType.IDENTIFIER(ref.text)
            }

            DevInTypes.NUMBER -> {
                FrontMatterType.NUMBER(ref.text.toInt())
            }

            DevInTypes.QUOTE_STRING -> {
                val value = ref.text.substring(1, ref.text.length - 1)
                FrontMatterType.STRING(value)
            }

            DevInTypes.VARIABLE_START -> {
                val next = ref.lastChild
                FrontMatterType.VARIABLE(next.text)
            }

            DevInTypes.DEFAULT -> {
                FrontMatterType.IDENTIFIER(ref.text)
            }

            else -> {
                logger.warn("parseLiteral, Unknown ref type: ${firstChild.elementType}")
                FrontMatterType.STRING(ref.text)
            }
        }
    }

    private fun parseFrontMatterValue(element: PsiElement): FrontMatterType? {
        when (element) {
            is DevInObjectKeyValue -> {
                val map: MutableMap<String, FrontMatterType> = mutableMapOf()
                element.children.mapNotNull {
                    if (it.elementType == DevInTypes.KEY_VALUE) {
                        processFrontMatter(it.children)
                    } else {
                        null
                    }
                }.forEach {
                    map.putAll(it)
                }

                return FrontMatterType.OBJECT(map)
            }
        }

        return when (element.firstChild.elementType) {
            DevInTypes.IDENTIFIER -> {
                FrontMatterType.IDENTIFIER(element.text)
            }

            DevInTypes.DATE -> {
                FrontMatterType.DATE(element.text)
            }

            DevInTypes.QUOTE_STRING -> {
                val value = element.text.substring(1, element.text.length - 1)
                FrontMatterType.STRING(value)
            }

            DevInTypes.NUMBER -> {
                FrontMatterType.NUMBER(element.text.toInt())
            }

            DevInTypes.BOOLEAN -> {
                FrontMatterType.BOOLEAN(element.text.toBoolean())
            }

            DevInTypes.FRONT_MATTER_ARRAY -> {
                val array: List<FrontMatterType> = parseArray(element)
                FrontMatterType.ARRAY(array)
            }

            DevInTypes.NEWLINE -> {
                return parseFrontMatterValue(element.firstChild.nextSibling)
            }

            DevInTypes.LBRACKET,
            DevInTypes.RBRACKET,
            DevInTypes.COMMA,
            WHITE_SPACE,
            null,
                -> {
                null
            }

            else -> {
                logger.warn("parseFrontMatterValue, Unknown frontmatter type: ${element.firstChild}")
                null
            }
        }
    }

    private fun parsePatternAction(element: PsiElement): FrontMatterType? {
        val pattern = element.children.firstOrNull()?.text ?: return null

        val actionBlock = PsiTreeUtil.getChildOfType(element, DevInActionBlock::class.java)
        val actionBody = actionBlock?.actionBody ?: return null

        val processor: List<PatternActionFunc> = parseActionBodyFuncCall(actionBody.actionExprList).processors
        return FrontMatterType.PATTERN(RuleBasedPatternAction(pattern, processor))
    }

    private fun parseActionBodyFuncCall(shireActionExprs: List<DevInActionExpr>?): Processor {
        val processor: MutableList<PatternActionFunc> = mutableListOf()
        shireActionExprs?.forEach { expr: DevInActionExpr ->
            expr.funcCall?.let { funcCall ->
                parseActionBodyFunCall(funcCall)?.let {
                    processor.add(it)
                }
            }
            expr.caseBody?.let { caseBody ->
                parseExprCaseBody(caseBody)?.let { conditionCase ->
                    val cases = conditionCase.cases.map {
                        (it as FrontMatterType.EXPRESSION).value as CaseKeyValue
                    }

                    processor.add(PatternActionFunc.CaseMatch(cases))
                }
            }
        }

        return Processor(processor)
    }

    private fun parseActionBodyFunCall(funcCall: DevInFuncCall?): PatternActionFunc? {
        val args = parseParameters(funcCall) ?: emptyList()
        val funcName = funcCall?.funcName?.text ?: return null
        return PatternActionFunc.from(funcName, args)
    }

    private fun parseParameters(funcCall: DevInFuncCall?): List<String>? = runReadAction {
        PsiTreeUtil.findChildOfType(funcCall, DevInPipelineArgs::class.java)
            ?.let {
                it.pipelineArgList.map { arg -> arg }
            }?.map {
                when (it.firstChild.elementType) {
                    DevInTypes.QUOTE_STRING -> it.text
                        .removeSurrounding("\"")
                        .removeSurrounding("'")

                    DevInTypes.IDENTIFIER -> it.text.removeSurrounding("\"")
                    else -> it.text
                }
            }
    }

    private fun parseArray(element: PsiElement): List<FrontMatterType> {
        val array = mutableListOf<FrontMatterType>()
        var arrayElement: PsiElement? = element.children.firstOrNull()?.firstChild
        while (arrayElement != null) {
            parseFrontMatterValue(arrayElement)?.let {
                array.add(it)
            }
            arrayElement = arrayElement.nextSibling
        }

        return array
    }
}
