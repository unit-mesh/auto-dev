package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.language.DevInLanguage
import cc.unitmesh.devti.language.actions.DevInRunFileAction
import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.psi.PsiElement

class DevInRunLineMarkersProvider : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.language !is DevInLanguage || element.textRange.startOffset != 0) return null

        val psiFile = element.containingFile
        if (psiFile !is DevInFile) return null

        val actions = arrayOf<AnAction>(ActionManager.getInstance().getAction(DevInRunFileAction.ID))

        return Info(
            AllIcons.RunConfigurations.TestState.Run, { "Run DevIn file: ${psiFile.name}" }, *actions
        )
    }
}
