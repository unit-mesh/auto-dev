package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.language.DevInLanguage
import cc.unitmesh.devti.language.actions.DevInRunFileAction
import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil

class DevInRunLineMarkersProvider : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.language !is DevInLanguage || element.textRange.startOffset != 0) return null

        val psiFile = element.containingFile
        if (psiFile !is DevInFile) return null

        val actions = arrayOf<AnAction>(ActionManager.getInstance().getAction(DevInRunFileAction.ID))

        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            actions
        ) { psiElement: PsiElement ->
            StringUtil.join(
                ContainerUtil.mapNotNull<AnAction, String>(actions) {
                    AutoDevBundle.message("line.marker.run.0", psiElement.containingFile.name)
                }, "\n"
            )
        }
    }
}
