package cc.unitmesh.idea.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.NonNls

class JavaLivingDocumentation : LivingDocumentation {
    override val parameterTagInstruction: String get() = "use @param tag"
    override val returnTagInstruction: String get() = "use @return tag"

    override val forbiddenRules: List<String> = listOf(
        "do not return example code",
        "do not use @author and @version tags"
    )

    override fun startEndString(type: LivingDocumentationType): Pair<String, String> {
        return when (type) {
            LivingDocumentationType.COMMENT -> Pair("/**", "*/")
            LivingDocumentationType.ANNOTATED -> Pair("", "")
            LivingDocumentationType.CUSTOM -> Pair("", "")
        }
    }

    override fun updateDoc(target: PsiElement, newDoc: String, type: LivingDocumentationType, editor: Editor) {
        val project = target.project
        val codeStyleManager = CodeStyleManager.getInstance(project)
        val file = target.containingFile
        WriteCommandAction.runWriteCommandAction(project, "Living Document", "cc.unitmesh.livingDoc", {
            val startOffset = target.textRange.startOffset
            val newEndOffset = startOffset + newDoc.length

            when (type) {
                LivingDocumentationType.COMMENT -> {
                    val doc = preHandleDoc(newDoc)
                    val psiElementFactory = JavaPsiFacade.getElementFactory(project)
                    val newDocComment = psiElementFactory.createDocCommentFromText(doc)

                    if (target is PsiDocCommentOwner) {
                        val oldDocComment = target.docComment
                        if (oldDocComment != null) {
                            oldDocComment.replace(newDocComment)
                        } else {
                            target.addBefore(newDocComment, target.firstChild)
                        }
                    }
                }

                LivingDocumentationType.ANNOTATED -> {
                    editor.document.insertString(startOffset, newDoc)
                    codeStyleManager.reformatText(file, startOffset, newEndOffset)
                }

                LivingDocumentationType.CUSTOM -> {
                    editor.document.insertString(startOffset, newDoc)
                    codeStyleManager.reformatText(file, startOffset, newEndOffset)
                }
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

    override fun findDocTargetsInSelection(
        root: PsiElement,
        selectionModel: SelectionModel,
    ): List<PsiNameIdentifierOwner> {
        val findCommonParent = CollectHighlightsUtil.findCommonParent(
            root,
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

    companion object {
        fun preHandleDoc(newDoc: String): @NonNls String {
            // 1. remove ```java and ``` from the newDoc if it exists
            val newDocWithoutCodeBlock = newDoc.removePrefix("```java")
                .removePrefix("```")
                .removeSuffix("```")

            // 2. lookup for the first line of the newDoc
            val fromSuggestion = LivingDocumentation.buildDocFromSuggestion(newDocWithoutCodeBlock, "/**", "*/")
            return fromSuggestion
        }
    }

}