package cc.unitmesh.devti.actions.template

import cc.unitmesh.devti.AutoDevIcons
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NonEmptyInputValidator
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.actions.*

class NewPromptTemplateAction : CreateFileFromTemplateAction(
    "AutoDev Customize", "Creates new AutoDev customize", AutoDevIcons.AI_COPILOT
), DumbAware {
    override fun getDefaultTemplateProperty(): String {
        return "DefaultAutoDevTemplate"
    }

    override fun buildDialog(project: Project, psiDir: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle("AutoDev AI Action")
            .addKind("Custom Prompt Action", AutoDevIcons.AI_COPILOT, "Custom Prompt Action")
            .setValidator(NonEmptyInputValidator())
    }

    override fun getActionName(psi: PsiDirectory?, p1: String, p2: String?): String {
        return "AutoDev AI Action"
    }
}
