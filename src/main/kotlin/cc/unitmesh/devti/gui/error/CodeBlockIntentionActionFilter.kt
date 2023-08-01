package cc.unitmesh.devti.gui.error

import cc.unitmesh.devti.gui.block.AutoDevSnippetFile.isSnippet
import com.intellij.codeInsight.daemon.impl.IntentionActionFilter
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiFile

class CodeBlockIntentionActionFilter : IntentionActionFilter {
    override fun accept(intentionAction: IntentionAction, file: PsiFile?): Boolean {
        val virtualFile = file?.virtualFile ?: return true
        return !isSnippet(virtualFile)
    }
}
