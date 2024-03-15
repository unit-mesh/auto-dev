package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.language.DevInLanguage
import cc.unitmesh.devti.language.actions.DevInRunFileAction
import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement

class DevInRunLineMarkersProvider : RunLineMarkerContributor(), DumbAware {
    override fun getInfo(element: PsiElement): Info? {
        if (element.language !is DevInLanguage) return null
        val psiFile = element as? DevInFile ?: return null

        logger<DevInRunLineMarkersProvider>().warn("offset: ${element.textRange}")

        val actions = arrayOf<AnAction>(ActionManager.getInstance().getAction(DevInRunFileAction.ID))

        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            { AutoDevBundle.message("line.marker.run.0", psiFile.containingFile.name) },
            *actions
        )
    }
}
