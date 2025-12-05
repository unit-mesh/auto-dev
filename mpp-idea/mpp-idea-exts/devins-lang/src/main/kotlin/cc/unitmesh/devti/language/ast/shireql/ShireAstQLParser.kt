package cc.unitmesh.devti.language.ast.shireql

import cc.unitmesh.devti.language.ast.FrontMatterType
import cc.unitmesh.devti.language.ast.ShirePsiQueryStatement
import cc.unitmesh.devti.language.ast.Statement
import cc.unitmesh.devti.language.ast.VariableElement
import cc.unitmesh.devti.language.compiler.HobbitHoleParser
import cc.unitmesh.devti.language.psi.DevInFromClause
import cc.unitmesh.devti.language.psi.DevInQueryStatement
import cc.unitmesh.devti.language.psi.DevInSelectClause
import cc.unitmesh.devti.language.psi.DevInWhereClause


object ShireAstQLParser {
    fun parse(statement: DevInQueryStatement): FrontMatterType {
        val value = ShirePsiQueryStatement(
            parseFrom(statement.fromClause),
            parseWhere(statement.whereClause)!!,
            parseSelect(statement.selectClause)
        )

        return FrontMatterType.QUERY_STATEMENT(value)
    }

    private fun parseFrom(fromClause: DevInFromClause): List<VariableElement> {
        return fromClause.psiElementDecl.psiVarDeclList.map {
            VariableElement(it.psiType.identifier.text, it.identifier.text)
        }
    }

    private fun parseWhere(whereClause: DevInWhereClause): Statement? {
        return HobbitHoleParser.parseExpr(whereClause.expr)
    }

    private fun parseSelect(selectClause: DevInSelectClause): List<Statement> {
        return selectClause.exprList.mapNotNull {
            HobbitHoleParser.parseExpr(it)
        }
    }
}
