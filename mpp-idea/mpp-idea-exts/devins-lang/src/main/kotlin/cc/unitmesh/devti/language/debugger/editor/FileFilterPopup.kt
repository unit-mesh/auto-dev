package cc.unitmesh.devti.language.debugger.editor

import cc.unitmesh.devti.language.utils.canBeAdded
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.ide.presentation.VirtualFilePresentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.TextFieldWithAutoCompletionListProvider
import java.awt.Component
import java.awt.Dimension
import javax.swing.Icon

/**
 * @author lk
 */
class FileFilterPopup(project: Project, onSelect: (VirtualFile) -> Unit) {
    private val textField: TextFieldWithAutoCompletion<VirtualFile>

    private var popup: JBPopup? = null

    init {
        val fileList = mutableListOf<VirtualFile>()

        ApplicationManager.getApplication().executeOnPooledThread {
            runReadAction {
                ProjectFileIndex.getInstance(project).iterateContent({ file ->
                    fileList.add(file)
                    true
                }) { it.canBeAdded() }
            }
        }

        val basePath = project.guessProjectDir()?.path ?: ""

        textField = TextFieldWithAutoCompletion(
            project, object : TextFieldWithAutoCompletionListProvider<VirtualFile>(fileList) {
                override fun getLookupString(item: VirtualFile): String {
                    return item.path.removePrefix(basePath)
                }

                override fun getIcon(item: VirtualFile): Icon? {
                    return VirtualFilePresentation.getIcon(item)
                }

                override fun createInsertHandler(item: VirtualFile): InsertHandler<LookupElement> {
                    return InsertHandler { _, _ -> onSelect(item); popup?.cancel() }
                }

            }, false, null
        )


        textField.addDocumentListener(object : DocumentListener {
            override fun documentChanged(e: DocumentEvent) {
                if (e.oldLength > e.newLength && textField.editor != null) {
                    AutoPopupController.getInstance(project).autoPopupMemberLookup(textField.editor) { true }
                }
            }
        })
    }

    fun show(component: Component) {
        textField.preferredSize = Dimension(180, 30)
        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(textField, textField)
            .setResizable(true)
            .setMovable(false)
            .setRequestFocus(true)
            .createPopup()
        popup?.showUnderneathOf(component)
    }

}