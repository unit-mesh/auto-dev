package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.LivingDocumentation
import cc.unitmesh.devti.provider.LivingDocumentationType
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

class JavaLivingDocumentation : LivingDocumentation {
    override fun startEndString(type: LivingDocumentationType): Pair<String, String> {
        return when (type) {
            LivingDocumentationType.NORMAL -> Pair("/**", "*/")
            LivingDocumentationType.ANNOTATED -> Pair("", "")
            LivingDocumentationType.LIVING -> Pair("", "")
        }
    }

    override fun updateDoc(psiElement: PsiElement, str: String) {
        val project = psiElement.project
        WriteCommandAction.runWriteCommandAction(project, "Living Document", "cc.unitmesh.livingDoc", {
            val psiElementFactory = JavaPsiFacade.getElementFactory(project)
            val newDocComment = psiElementFactory.createDocCommentFromText(str)

            if (psiElement is PsiDocCommentOwner) {
                val oldDocComment = psiElement.docComment
                if (oldDocComment != null) {
                    oldDocComment.replace(newDocComment)
                } else {
                    psiElement.addBefore(newDocComment, psiElement.firstChild)
                }
            } else {
                throw IncorrectOperationException("Unable to update documentation")
            }
        })
    }

    override fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner? {
        if (psiElement is PsiMethod || psiElement is PsiClass) return psiElement as PsiNameIdentifierOwner

        val closestIdentifierOwner = PsiTreeUtil.getParentOfType(psiElement, PsiNameIdentifierOwner::class.java)
        if (closestIdentifierOwner !is PsiMethod) {
            return PsiTreeUtil.getParentOfType(psiElement, PsiMethod::class.java) ?: closestIdentifierOwner
        }

        return closestIdentifierOwner

    }

    fun containsElement(selectionModel: SelectionModel, element: PsiElement): Boolean {
        return selectionModel.selectionStart <= element.textRange.startOffset && element.textRange.endOffset <= selectionModel.selectionEnd
    }

    override fun findDocTargetsInSelection(
        psiElement: PsiElement,
        selectionModel: SelectionModel,
    ): List<PsiNameIdentifierOwner> {
        val findCommonParent = CollectHighlightsUtil.findCommonParent(
            psiElement,
            selectionModel.selectionStart,
            selectionModel.selectionEnd
        ) ?: return emptyList()

        if (findCommonParent is PsiJavaFile) {
            val classAndFieldMethods = mutableListOf<PsiNameIdentifierOwner>()
            val classes = findCommonParent.classes
            for (psiClass in classes) {
                if (containsElement(selectionModel, psiClass)) {
                    classAndFieldMethods.add(psiClass)
                }
            }

            return classAndFieldMethods
        }

        val target = findNearestDocumentationTarget(findCommonParent) ?: return emptyList()

        if (target !is PsiClass || containsElement(selectionModel, target)) {
            return listOf(target)
        }

        val methodsAndFieldsInRange = mutableListOf<PsiNameIdentifierOwner>()
        for (psiField in target.fields) {
            if (containsElement(selectionModel, psiField)) {
                methodsAndFieldsInRange.add(psiField)
            }
        }
        for (psiMethod in target.methods) {
            if (containsElement(selectionModel, psiMethod)) {
                methodsAndFieldsInRange.add(psiMethod)
            }
        }

        return methodsAndFieldsInRange
    }

}