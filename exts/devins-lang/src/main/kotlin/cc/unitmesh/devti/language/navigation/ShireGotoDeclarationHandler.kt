package cc.unitmesh.devti.language.navigation

import cc.unitmesh.devti.language.ast.action.PatternActionFuncDef
import cc.unitmesh.devti.devins.post.PostProcessorType
import cc.unitmesh.devti.language.psi.DevInFrontMatterEntry
import cc.unitmesh.devti.language.psi.DevInFrontMatterHeader
import cc.unitmesh.devti.language.psi.DevInFuncCall
import cc.unitmesh.devti.language.psi.DevInFuncName
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.utils.findFile
import cc.unitmesh.devti.language.utils.lookupFile
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandlerBase
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil.getChildrenOfTypeAsList
import com.intellij.psi.util.elementType


class ShireGotoDeclarationHandler : GotoDeclarationHandlerBase(), GotoDeclarationHandler {
    private val validFunctionNames = setOf(
        PatternActionFuncDef.EXECUTE.funcName,
        PatternActionFuncDef.THREAD.funcName,
        PatternActionFuncDef.BATCH.funcName,
        PostProcessorType.SaveFile.handleName,
        PatternActionFuncDef.APPROVAL_EXECUTE.funcName,
        "mock"
    )

    override fun getGotoDeclarationTarget(element: PsiElement?, editor: Editor?): PsiElement? {
        if (element !is LeafPsiElement) return null
        val project = element.project

        gotoSourceFile(element, project)

        return gotoToFunctionDecl(element)
    }

    private fun gotoToFunctionDecl(element: LeafPsiElement): DevInFrontMatterEntry? {
        val psiFile = element.containingFile
        // handle for foreign function
        val func = element.parent as? DevInFuncName ?: return null
        val header = getChildrenOfTypeAsList(psiFile, DevInFrontMatterHeader::class.java).firstOrNull()
        val functionsNode = header?.frontMatterEntries?.frontMatterEntryList?.firstOrNull {
            it.firstChild.text == "functions"
        } ?: return null

        val functionEntries: List<DevInFrontMatterEntry> =
            functionsNode.frontMatterValue?.objectKeyValue?.keyValueList?.filter {
                it.frontMatterEntry.foreignFunction != null
            }?.map {
                it.frontMatterEntry
            } ?: return null

        val funcName = func.text
        val foreignFunc = functionEntries.find { it.frontMatterKey?.text == funcName } ?: return null
        return foreignFunc
    }

    private fun gotoSourceFile(
        element: LeafPsiElement,
        project: Project,
    ) {
        if (element.elementType != DevInTypes.QUOTE_STRING) return
        if (element.parent?.elementType != DevInTypes.PIPELINE_ARG) return

        val funcCall = element.parent?.parent?.parent as? DevInFuncCall ?: return
        val funcName = funcCall.funcName.text

        if (funcName !in validFunctionNames) return

        val fileName = element.text.removeSurrounding("\"")
        val file = project.lookupFile(fileName) ?: project.findFile(fileName) ?: return

        runInEdt {
            FileEditorManager.getInstance(project).openFile(file, true)
        }
    }
}
