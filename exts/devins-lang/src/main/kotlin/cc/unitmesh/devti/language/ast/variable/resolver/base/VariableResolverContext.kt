package cc.unitmesh.devti.language.ast.variable.resolver.base

import cc.unitmesh.devti.language.ast.HobbitHole
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import cc.unitmesh.devti.language.ast.variable.VariableTable

data class VariableResolverContext(
    val myProject: Project,
    val editor: Editor,
    val hole: HobbitHole?,
    val variableTable: VariableTable,
    var element: PsiElement?
)