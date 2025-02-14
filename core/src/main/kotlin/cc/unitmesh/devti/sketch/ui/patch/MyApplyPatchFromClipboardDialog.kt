package cc.unitmesh.devti.sketch.ui.patch

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsApplicationSettings
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.changes.patch.ApplyPatchDefaultExecutor
import com.intellij.openapi.vcs.changes.patch.ApplyPatchDifferentiatedDialog
import com.intellij.openapi.vcs.changes.patch.ApplyPatchMode
import com.intellij.testFramework.LightVirtualFile
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.JCheckBox
import javax.swing.JComponent

class MyApplyPatchFromClipboardDialog(project: Project, clipboardText: String) :
    ApplyPatchDifferentiatedDialog(
        project, ApplyPatchDefaultExecutor(project), emptyList(), ApplyPatchMode.APPLY_PATCH_IN_MEMORY,
        LightVirtualFile("clipboardPatchFile", clipboardText), null, null,  //NON-NLS
        null, null, null, false
    ) {
    override fun createDoNotAskCheckbox(): JComponent = createAnalyzeOnTheFlyOptionPanel()

    companion object {
        private fun createAnalyzeOnTheFlyOptionPanel(): JCheckBox {
            val removeOptionCheckBox =
                JCheckBox(VcsBundle.message("patch.apply.analyze.from.clipboard.on.the.fly.checkbox"))
            removeOptionCheckBox.mnemonic = KeyEvent.VK_L
            removeOptionCheckBox.isSelected = VcsApplicationSettings.getInstance().DETECT_PATCH_ON_THE_FLY
            removeOptionCheckBox.addActionListener { e: ActionEvent? ->
                VcsApplicationSettings.getInstance().DETECT_PATCH_ON_THE_FLY = removeOptionCheckBox.isSelected
            }
            return removeOptionCheckBox
        }
    }
}