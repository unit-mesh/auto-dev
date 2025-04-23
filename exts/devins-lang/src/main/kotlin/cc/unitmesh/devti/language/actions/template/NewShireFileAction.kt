package cc.unitmesh.devti.language.actions.template

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NonEmptyInputValidator
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile

class NewShireFileAction : CreateFileFromTemplateAction(
    AutoDevBundle.message("devins.newFile"), "Creates new shire action", AutoDevIcons.AI_COPILOT
), DumbAware {
    override fun getDefaultTemplateProperty(): String = "DefaultDevInsTemplate"

    override fun getActionName(psi: PsiDirectory?, p1: String, p2: String?): String =
        AutoDevBundle.message("devins.newFile")

    override fun buildDialog(project: Project, psiDir: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(AutoDevBundle.message("devins.newFile"))
            .addKind(AutoDevBundle.message("devins.file"), AutoDevIcons.AI_COPILOT, "AutoDevAction")
            .setValidator(NonEmptyInputValidator())
    }

    override fun createFile(name: String?, templateName: String?, dir: PsiDirectory?): PsiFile? {
        val template = FileTemplateManager.getInstance(dir!!.project).getInternalTemplate(templateName!!)
        val newName = name!!.lowercase().replace(" ", "_")

        template.text = template.text.replace("{{name}}", name)
        return createFileFromTemplate(newName, template, dir)
    }
}
