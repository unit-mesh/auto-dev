package cc.unitmesh.devti.language.ast.shireql

import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.ast.VariableElement
import cc.unitmesh.devti.language.ast.action.PatternActionFunc
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import cc.unitmesh.devti.devins.provider.vcs.ShireGitCommit
import cc.unitmesh.devti.devins.provider.ShireQLDataProvider
import cc.unitmesh.devti.devins.provider.ShireSymbolProvider

class ShireQLVariableBuilder(val myProject: Project, hole: HobbitHole) {
    fun buildVariables(fromStmt: PatternActionFunc.From): Map<String, List<Any>> {
        return fromStmt.variables.associate {
            when {
                it.variableType.startsWith("Psi") -> {
                    it.value to lookupElement(it)
                }
                it.variableType.startsWith("Git") -> {
                    it.value to lookupVcsCommit(it)
                }
                it.variableType == ShireQLFromType.Date.typeName -> {
                    it.value to createDateFunc(it)
                }
                else -> {
                    it.value to lookupElement(it)
                }
            }
        }
    }

    // cache
    private val cache = mutableMapOf<String, List<PsiElement>>()

    private fun lookupElement(it: VariableElement): List<PsiElement> {
        if (cache.containsKey(it.variableType)) {
            return cache[it.variableType] ?: emptyList()
        }

        val elements: List<PsiElement> = ShireSymbolProvider.all().flatMap { provider ->
            provider.lookupElementByName(myProject, it.variableType) ?: emptyList()
        }

        cache[it.variableType] = elements
        return elements
    }


    private fun lookupVcsCommit(it: VariableElement): List<ShireGitCommit> {
        val elements: List<ShireGitCommit> = ShireQLDataProvider.all().flatMap { provider ->
            provider.lookup(myProject, it.variableType) ?: emptyList()
        }

        return elements
    }

    private fun createDateFunc(it: VariableElement): List<ShireDateSchema> {
        return listOf(ShireDateSchema())
    }
}
