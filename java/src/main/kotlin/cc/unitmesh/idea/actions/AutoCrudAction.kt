package cc.unitmesh.idea.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.intentions.action.base.ChatBaseIntention
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.provider.DevFlowProvider
import cc.unitmesh.devti.gui.sendToChatPanel
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiFile

class AutoCrudAction : ChatBaseIntention() {
    private val logger = logger<AutoCrudAction>()

    override fun priority(): Int = 900

    override fun getText(): String = AutoDevBundle.message("intentions.crud.new.name")
    override fun getFamilyName(): String = AutoDevBundle.message("intentions.crud.new.family.name")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) return false
        if (file.language !is JavaLanguage) return false

        val isEnvironmentAvailable = super.isAvailable(project, editor, file)
        return isEnvironmentAvailable && editor.selectionModel.hasSelection()
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return

        val flowProvider = DevFlowProvider.flowProvider(JavaLanguage.INSTANCE.displayName)
        if (flowProvider == null) {
            logger.error("current Language don't implementation DevFlow")
            return
        }

        sendToChatPanel(project) { contentPanel, _ ->
            val openAIRunner = LlmFactory().create(project)
            val selectedText = editor.selectionModel.selectedText ?: throw IllegalStateException("no select text")
            flowProvider.initContext(null, openAIRunner, contentPanel, project)
            ProgressManager.getInstance().run(executeCrud(flowProvider, project, selectedText))
        }
    }

    private fun executeCrud(flowProvider: DevFlowProvider, project: Project, selectedText: @NlsSafe String) =
        object : Task.Backgroundable(project, "Loading retained test failure", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.fraction = 0.2

                indicator.text = AutoDevBundle.message("autocrud.generatingDtoAndEntity")
                flowProvider.updateOrCreateDtoAndEntity(selectedText)

                indicator.fraction = 0.4

                indicator.text = AutoDevBundle.message("autocrud.progress.fetchingSuggestEndpoint")
                val target = flowProvider.fetchSuggestEndpoint(selectedText)

                indicator.fraction = 0.6

                indicator.text = AutoDevBundle.message("autocrud.progress.updatingEndpointMethod")
                flowProvider.updateOrCreateEndpointCode(target, selectedText)

                indicator.fraction = 0.8

                indicator.text = AutoDevBundle.message("autocrud.progress.creatingServiceAndRepository")
                flowProvider.updateOrCreateServiceAndRepository()

                indicator.fraction = 1.0
            }
        }


}
