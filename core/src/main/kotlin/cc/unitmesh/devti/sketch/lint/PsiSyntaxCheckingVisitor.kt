package cc.unitmesh.devti.sketch.lint

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

abstract class PsiSyntaxCheckingVisitor : PsiElementVisitor() {
    override fun visitElement(element: PsiElement) {
        runReadAction {
            element.children.forEach { it.accept(this) }
        }
    }
}