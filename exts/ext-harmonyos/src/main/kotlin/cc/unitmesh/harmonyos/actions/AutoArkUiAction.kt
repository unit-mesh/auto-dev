package cc.unitmesh.harmonyos.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.intentions.action.base.ChatBaseIntention
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.harmonyos.actions.auto.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class AutoArkUiAction : ChatBaseIntention() {
    override fun priority(): Int = 900
    override fun getText(): String = AutoDevBundle.message("autoarkui.generate")
    override fun getFamilyName(): String = AutoDevBundle.message("autoarkui.name")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        val isDevEcoStudio = System.getProperty("idea.platform.prefix", "idea") == "DevEcoStudio"
        val isAndroidStudio = System.getProperty("idea.platform.prefix", "idea") == "AndroidStudio"

        return isDevEcoStudio || isAndroidStudio
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val selectedText = editor.selectionModel.selectedText ?: return

        val context = AutoArkUiContext(
            selectedText,
            language = file.language.displayName,
            layoutOverride = ArkUiLayoutType.overview(),
            componentOverride = ArkUiComponentType.overview(),
        )

        sendToChatPanel(project) { contentPanel, _ ->
            val llmProvider = LlmFactory().create(project)
            val prompter = AutoArkUiFlow(contentPanel, llmProvider, context)
            val task = AutoArkUiTask(project, prompter, editor)

            ProgressManager.getInstance()
                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        }
    }
}


