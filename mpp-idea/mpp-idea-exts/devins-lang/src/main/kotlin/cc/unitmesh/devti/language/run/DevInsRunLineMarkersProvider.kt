package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.language.DevInBundle
import cc.unitmesh.devti.language.DevInLanguage
import cc.unitmesh.devti.language.actions.DevInsRunFileAction
import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement

class DevInsRunLineMarkersProvider : RunLineMarkerContributor(), DumbAware {
    override fun getInfo(element: PsiElement): Info? {
        if (element.language !is DevInLanguage) return null
        val psiFile = element as? DevInFile ?: return null

        val actions = arrayOf<AnAction>(ActionManager.getInstance().getAction(DevInsRunFileAction.ID))

        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            { DevInBundle.message("devin.line.marker.run.0", psiFile.containingFile.name) },
            *actions
        )
    }
}
