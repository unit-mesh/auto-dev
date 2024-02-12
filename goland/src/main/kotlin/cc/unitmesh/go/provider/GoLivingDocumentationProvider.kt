package cc.unitmesh.go.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.goide.psi.GoFieldDeclaration
import com.goide.psi.GoMethodSpec
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.goide.psi.impl.GoPsiUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager

class GoLivingDocumentationProvider : LivingDocumentation {
    override val forbiddenRules: List<String> get() = listOf("do not return example code")

    override fun startEndString(type: LivingDocumentationType): Pair<String, String> = "/*" to "*/"

    override fun updateDoc(target: PsiElement, newDoc: String, type: LivingDocumentationType, editor: Editor) {
        val project = target.project
        val codeStyleManager = CodeStyleManager.getInstance(project)
        WriteCommandAction.runWriteCommandAction(project, "Living Document", "cc.unitmesh.livingDoc", {
            val startOffset = target.textRange.startOffset
            val newEndOffset = startOffset + newDoc.length

            when (type) {
                LivingDocumentationType.COMMENT -> {

                }

                LivingDocumentationType.ANNOTATED -> {
                    editor.document.insertString(startOffset, newDoc)
                    codeStyleManager.reformatText(target.containingFile, startOffset, newEndOffset)
                }

                LivingDocumentationType.CUSTOM -> {
                    editor.document.insertString(startOffset, newDoc)
                    codeStyleManager.reformatText(target.containingFile, startOffset, newEndOffset)
                }
            }
        })
    }

    override fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner? {
        TODO("Not yet implemented")
    }

    fun getMayBeDocumented(element: PsiElement): Boolean {
        return element is GoFieldDeclaration || element is GoMethodSpec || GoPsiUtil.isTopLevelDeclaration(element)
    }

    override fun findDocTargetsInSelection(
        root: PsiElement,
        selectionModel: SelectionModel
    ): List<PsiNameIdentifierOwner> {
        TODO("Not yet implemented")
    }

}
