package cc.unitmesh.devti.language.actions.intention

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.language.actions.DevInsRunFileAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import cc.unitmesh.devti.language.actions.base.validator.WhenConditionValidator
import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.ast.config.ShireActionLocation
import cc.unitmesh.devti.language.startup.DynamicShireActionService
import kotlin.collections.firstOrNull

class ShireIntentionAction(private val hobbitHole: HobbitHole?, val file: PsiFile, private val event: AnActionEvent?) :
    IntentionAction {
    override fun startInWriteAction(): Boolean = true
    override fun getFamilyName(): String = AutoDevBundle.message("devins.intention")
    override fun getText(): String = hobbitHole?.name ?: AutoDevBundle.message("devins.intention")

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        val conditions = hobbitHole?.when_ ?: return true
        return WhenConditionValidator.isAvailable(conditions, file)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val config = DynamicShireActionService.getInstance(project)
            .getActions(ShireActionLocation.INTENTION_MENU)
            .firstOrNull { it.hole == hobbitHole } ?: return

        DevInsRunFileAction.executeFile(project, config, null)
    }
}