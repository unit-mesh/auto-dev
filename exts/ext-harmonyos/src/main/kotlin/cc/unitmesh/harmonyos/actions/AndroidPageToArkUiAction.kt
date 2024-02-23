package cc.unitmesh.harmonyos.actions

import cc.unitmesh.devti.intentions.action.base.ChatBaseIntention
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class AndroidPageToArkUiAction : ChatBaseIntention() {
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return System.getProperty("idea.platform.prefix", "idea") == "DevEcoStudio"
    }

    override fun priority(): Int = 900

    override fun getText(): String = "Android Page to Ark UI"

    override fun getFamilyName(): String = "Android Page to Ark UI"
}