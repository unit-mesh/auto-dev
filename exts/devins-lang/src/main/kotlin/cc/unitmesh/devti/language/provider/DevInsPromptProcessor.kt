package cc.unitmesh.devti.language.provider

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.language.DevInLanguage
import cc.unitmesh.devti.language.compiler.DevInsCompiler
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.provider.devins.CustomAgentContext
import cc.unitmesh.devti.provider.devins.LanguagePromptProcessor
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiUtilBase


class DevInsPromptProcessor : LanguagePromptProcessor {
    override val name: String = DevInLanguage.displayName

    override fun execute(project: Project, context: CustomAgentContext): String {
        var text = context.response
        // re-check the language of the code
        CodeFence.parse(text).let {
            if (it.language == DevInLanguage.INSTANCE) {
                text = it.text
            }
        }

        val devInsCompiler = createCompiler(project, text)
        val result = devInsCompiler.compile()
        AutoDevNotifications.notify(project, result.output)

        if (result.nextJob != null) {
            val nextJob = result.nextJob!!
            val nextResult = createCompiler(project, nextJob).compile()
            AutoDevNotifications.notify(project, nextResult.output)
            return nextResult.output
        }

        return result.output
    }

    override fun compile(project: Project, text: String): String {
        val devInsCompiler = createCompiler(project, text)
        val result = devInsCompiler.compile()
        return result.output
    }

    /**
     * Creates a new instance of `DevInsCompiler`.
     *
     * @param project The current project.
     * @param text The source code text.
     * @return A new instance of `DevInsCompiler`.
     */
    private fun createCompiler(
        project: Project,
        text: String
    ): DevInsCompiler {
        val devInFile = DevInFile.fromString(project, text)
        return createCompiler(project, devInFile)
    }

    private fun createCompiler(
        project: Project,
        devInFile: DevInFile
    ): DevInsCompiler {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val element: PsiElement? = editor?.caretModel?.currentCaret?.offset?.let {
            val psiFile = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return@let null
            getElementAtOffset(psiFile, it)
        }

        return DevInsCompiler(project, devInFile, editor, element)
    }

    private fun getElementAtOffset(psiFile: PsiElement, offset: Int): PsiElement? {
        var element = psiFile.findElementAt(offset) ?: return null

        if (element is PsiWhiteSpace) {
            element = element.parent
        }

        return element
    }
}

