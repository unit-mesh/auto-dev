package cc.unitmesh.devti.intentions.action

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.document.CustomDocumentationConfig
import cc.unitmesh.devti.intentions.action.base.BasedDocumentationIntention
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class DefaultDocumentationIntention: BasedDocumentationIntention() {
    override val config: CustomDocumentationConfig
        get() = CustomDocumentationConfig.default()

    override fun getText(): String = AutoDevBundle.message("intentions.living.documentation.name")

    override fun getFamilyName(): String = AutoDevBundle.message("intentions.living.documentation.family.name")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) return false

        return LivingDocumentation.forLanguage(file.language) != null
    }
}