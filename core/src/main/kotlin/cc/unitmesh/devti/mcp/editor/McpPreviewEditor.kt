package cc.unitmesh.devti.mcp.editor

import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

/**
 * Display shire file render prompt and have a sample file as view
 */
open class McpPreviewEditor(
    val project: Project,
    val virtualFile: VirtualFile,
) : UserDataHolder by UserDataHolderBase(), FileEditor {
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
    private var mainEditor = MutableStateFlow<Editor?>(null)
    private val mainPanel = JPanel(BorderLayout())
    private val visualPanel: JBScrollPane = JBScrollPane(
        mainPanel,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    )

    private var editorPanel: JPanel? = null

    init {
        val corePanel = panel {
            row {
                val label = JBLabel("MCP Preview (Experimental)").apply {
                    fontColor = UIUtil.FontColor.BRIGHTER
                    background = JBColor(0xF5F5F5, 0x2B2D30)
                    font = JBUI.Fonts.label(16.0f).asBold()
                    border = JBUI.Borders.empty(0, 16)
                    isOpaque = true
                }

                cell(label).align(Align.FILL).resizableColumn()
            }
        }

        this.mainPanel.add(corePanel, BorderLayout.CENTER)
    }

    fun setMainEditor(editor: Editor) {
        check(mainEditor.value == null)
        mainEditor.value = editor
    }

    fun updateDisplayedContent() {

    }

    fun scrollToSrcOffset(offset: Int) {

    }

    override fun getComponent(): JComponent = visualPanel
    override fun getName(): String = "MCP Preview"
    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = true
    override fun getFile(): VirtualFile = virtualFile
    override fun getPreferredFocusedComponent(): JComponent? = null
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun dispose() {}
}