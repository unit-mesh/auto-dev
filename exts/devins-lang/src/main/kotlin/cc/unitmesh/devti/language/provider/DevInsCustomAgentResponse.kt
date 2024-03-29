package cc.unitmesh.devti.language.provider

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.language.compiler.DevInsCompiler
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.provider.devins.CustomAgentContext
import cc.unitmesh.devti.provider.devins.LanguagePromptProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiUtilBase


class DevInsCustomAgentResponse : LanguagePromptProcessor {
    override val name: String = "DevIn"

    override fun execute(project: Project, context: CustomAgentContext): String {
        val devInFile = DevInFile.fromString(project, context.response)
        val devInsCompiler = DevInsCompiler(project, devInFile)

        val result = devInsCompiler.compile()
        AutoDevNotifications.notify(project, result.output)
        return result.output
    }

    private fun getCurrentPsiFile(project: Project, editor: Editor): PsiFile? {
        return PsiUtilBase.getPsiFileInEditor(editor, project)
    }

    private fun getElementAtOffset(psiFile: PsiElement, offset: Int): PsiElement? {
        // 获取偏移量对应的元素
        var element = psiFile.findElementAt(offset) ?: return null

        // 如果元素是空白元素，尝试获取其父元素
        if (element is PsiWhiteSpace) {
            element = element.getParent()
        }

        return element
    }

    override fun compile(project: Project, text: String): String {
        val devInFile = DevInFile.fromString(project, text)
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val element: PsiElement? = editor?.caretModel?.currentCaret?.offset?.let {
            val psiFile = getCurrentPsiFile(project, editor) ?: return@let null
            getElementAtOffset(psiFile, it)
        }

        val devInsCompiler = DevInsCompiler(project, devInFile, editor, element)

        val result = devInsCompiler.compile()
        return if (result.hasError || result.isLocalCommand) {
            text
        } else {
            result.output
        }
    }
}

