package cc.unitmesh.devti.custom.compile

import cc.unitmesh.devti.custom.team.DefaultTeamContextProvider
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import cc.unitmesh.devti.intentions.action.getElementToAction
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiManager
import kotlinx.coroutines.runBlocking
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import java.io.StringWriter

class VariableTemplateCompiler(
    val language: Language,
    val file: PsiFile,
    val element: PsiElement?,
    val editor: Editor,
    val selectedText: String = "",
) {
    private val log = logger<VariableTemplateCompiler>()
    private val velocityContext = VelocityContext()

    init {
        this.set(CustomVariable.SELECTION.variable, runReadAction { editor.selectionModel.selectedText ?: selectedText })
        this.set(CustomVariable.BEFORE_CURSOR.variable, file.text.substring(0, editor.caretModel.offset))
        this.set(CustomVariable.AFTER_CURSOR.variable, file.text.substring(editor.caretModel.offset))
        this.set(CustomVariable.ALL.variable, file.text)
    }

    fun set(key: String, value: String) {
        velocityContext.put(key, value)
    }

    fun putAll(map: Map<String, Any>) {
        map.forEach { (key, value) ->
            velocityContext.put(key, value)
        }
    }

    fun compile(template: String): String {
        velocityContext.put(CustomVariable.FILE_NAME.variable, file.name)
        velocityContext.put(CustomVariable.FILE_PATH.variable, file.virtualFile?.path ?: "")
        velocityContext.put(
            CustomVariable.METHOD_NAME.variable, when (element) {
                is PsiNameIdentifierOwner -> element.nameIdentifier?.text ?: ""
                else -> ""
            }
        )

        configForLanguage()
        configForFramework()

        val oldContextClassLoader = Thread.currentThread().getContextClassLoader()
        Thread.currentThread().setContextClassLoader(VariableTemplateCompiler::class.java.getClassLoader())

        val sw = StringWriter()
        try {
            velocityContext.put("context", DefaultTeamContextProvider(element, editor))
            Velocity.evaluate(velocityContext, sw, "#" + this.javaClass.name, template)
        } catch (e: Exception) {
            log.error("Failed to compile template: $template", e)
            sw.write(template)
        }

        Thread.currentThread().setContextClassLoader(oldContextClassLoader)
        return sw.toString()
    }

    private fun configForFramework() {
        runBlocking {
            val context = ChatCreationContext(
                ChatOrigin.Intention,
                ChatActionType.CUSTOM_ACTION,
                file,
                listOf(),
                null
            )

            val collectChatContextList = ChatContextProvider.collectChatContextList(file.project, context)
            velocityContext.put(CustomVariable.FRAMEWORK_CONTEXT.variable, collectChatContextList.joinToString("\n") {
                it.text
            })
        }
    }

    private fun configForLanguage() {
        velocityContext.put(CustomVariable.LANGUAGE.variable, language.displayName)
        velocityContext.put(
            CustomVariable.COMMENT_SYMBOL.variable, when (language.displayName.lowercase()) {
                "java", "kotlin" -> "//"
                "python" -> "#"
                "javascript" -> "//"
                "typescript" -> "//"
                "go" -> "//"
                "c", "c++", "c#" -> "//"
                "rust" -> "//"
                "ruby" -> "#"
                "shell" -> "#"
                else -> "-"
            }
        )
    }

    companion object {
        fun create(project: Project, myEditor: Editor?): VariableTemplateCompiler? {
            val editor = myEditor ?: FileEditorManager.getInstance(project).selectedTextEditor ?: return null
            val file: PsiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null
            val selectedText = editor.selectionModel.selectedText ?: ""

            return VariableTemplateCompiler(
                language = file.language,
                file = file,
                element = getElementToAction(project, editor),
                editor = editor,
                selectedText = selectedText,
            )
        }

        fun defaultEditor(myProject: Project): Editor? {
            return FileEditorManager.getInstance(myProject).selectedTextEditor
        }

        fun defaultElement(myProject: Project, currentEditor: Editor?): PsiElement? =
            ReadAction.compute<PsiElement?, Throwable> {
                currentEditor?.caretModel?.currentCaret?.offset?.let {
                    val psiFile = currentEditor.let { editor ->
                        val psiFile = FileDocumentManager.getInstance().getFile(editor.document)?.let { file ->
                            PsiManager.getInstance(myProject).findFile(file)
                        }

                        psiFile
                    } ?: return@let null

                    psiFile.findElementAt(it) ?: return@let psiFile
                }
            }
    }
}