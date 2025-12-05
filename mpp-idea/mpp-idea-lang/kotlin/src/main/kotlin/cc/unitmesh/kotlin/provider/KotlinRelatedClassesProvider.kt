package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.provider.RelatedClassesProvider
import cc.unitmesh.kotlin.util.KotlinTypeResolver
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.structuralsearch.visitor.KotlinRecursiveElementWalkingVisitor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

class KotlinRelatedClassProvider : RelatedClassesProvider {
    override fun lookupIO(element: PsiElement): List<PsiElement> {
        return KotlinTypeResolver.resolveByElement(element).values.filterNotNull().toList()
    }

    override fun lookupIO(element: PsiFile): List<PsiElement> {
        return KotlinTypeResolver.resolveByElement(element).values.filterNotNull().toList()
    }

    override fun lookupCallee(project: Project, element: PsiElement): List<PsiNamedElement> {
        return when (element) {
            is KtNamedFunction -> findCallees(project, element)
            else -> emptyList()
        }
    }

    override fun lookupCaller(
        project: Project,
        element: PsiElement
    ): List<PsiNamedElement> {
        return when (element) {
            is KtNamedFunction -> findCallers(project, element)
            else -> emptyList()
        }
    }

    fun findCallees(project: Project, method: KtNamedFunction): List<KtNamedFunction> {
        val calledMethods = mutableSetOf<KtNamedFunction>()
        method.accept(object : KotlinRecursiveElementWalkingVisitor() {
            override fun visitCallExpression(expression: KtCallExpression) {
                val psiElement = expression.calleeExpression?.node?.psi?.reference?.resolve() ?: return
                if (psiElement is KtNamedFunction) {
                    calledMethods.add(psiElement)
                }
            }
        })

        return calledMethods.toList()
    }
    
    fun findCallers(project: Project, method: KtNamedFunction): List<KtNamedFunction> {
        val callers = mutableSetOf<KtNamedFunction>()
        val references = ReferencesSearch.search(method).findAll()
        
        for (reference in references) {
            val element = reference.element
            
            var parentFunction: KtNamedFunction? = null
            var parent = element.parent
            
            while (parent != null) {
                if (parent is KtNamedFunction) {
                    parentFunction = parent
                    break
                }
                parent = parent.parent
            }
            
            if (parentFunction != null && parentFunction != method) {
                callers.add(parentFunction)
            }
        }
        
        return callers.toList()
    }
}
