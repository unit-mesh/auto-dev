package cc.unitmesh.devti.diff.service

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.*

/**
 * Standalone editor tab diff preview component for displaying diffs in editor tabs
 * Similar to IntelliJ Augment's StandaloneEditorTabDiffPreview
 */
class StandaloneEditorTabDiffPreview(
    private val project: Project,
    private val sourceEditor: Editor,
    private val originalContent: String,
    private val modifiedContent: String,
    val previewId: String
) : Disposable {
    
    private val diffContentFactory = DiffContentFactory.getInstance()
    private var diffComponent: JComponent? = null
    private var previewPanel: JPanel? = null
    private var isVisible = false
    
    @Volatile
    private var disposed = false
    
    val isDisposed: Boolean
        get() = disposed
    
    companion object {
        private val PREVIEW_KEY = Key.create<StandaloneEditorTabDiffPreview>("StandaloneEditorTabDiffPreview")
    }
    
    init {
        createPreviewComponent()
        setupDocumentListener()
    }
    
    private fun createPreviewComponent() {
        runInEdt {
            try {
                val leftContent = diffContentFactory.create(originalContent)
                val rightContent = diffContentFactory.create(modifiedContent)
                
                val diffRequest = SimpleDiffRequest(
                    "Preview Diff",
                    leftContent,
                    rightContent,
                    "Original",
                    "Modified"
                ).apply {
                    putUserData(PREVIEW_KEY, this@StandaloneEditorTabDiffPreview)
                }
                
                // Use simple text comparison as fallback
                // In a real implementation, you might want to use more sophisticated diff tools
                previewPanel = createSimplePreviewPanel()
            } catch (e: Exception) {
                previewPanel = createErrorPanel(e.message ?: "Failed to create diff preview")
            }
        }
    }
    
    private fun createPreviewPanel(diffComponent: JComponent, toolbar: JComponent?): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        
        // Add header with title and controls
        val headerPanel = createHeaderPanel()
        panel.add(headerPanel, BorderLayout.NORTH)
        
        // Add toolbar if available
        toolbar?.let {
            val toolbarPanel = JPanel(BorderLayout())
            toolbarPanel.add(it, BorderLayout.CENTER)
            panel.add(toolbarPanel, BorderLayout.CENTER)
        }
        
        // Add diff component
        panel.add(diffComponent, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createSimplePreviewPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        
        // Add header
        val headerPanel = createHeaderPanel()
        panel.add(headerPanel, BorderLayout.NORTH)
        
        // Create simple side-by-side text areas
        val splitter = JBSplitter(false, 0.5f)
        
        val originalTextArea = JTextArea(originalContent).apply {
            isEditable = false
            font = sourceEditor.colorsScheme.getFont(EditorFontType.PLAIN)
        }
        
        val modifiedTextArea = JTextArea(modifiedContent).apply {
            isEditable = false
            font = sourceEditor.colorsScheme.getFont(EditorFontType.PLAIN)
        }
        
        splitter.firstComponent = JScrollPane(originalTextArea).apply {
            border = JBUI.Borders.customLine(JBUI.CurrentTheme.Editor.BORDER_COLOR, 1)
        }
        splitter.secondComponent = JScrollPane(modifiedTextArea).apply {
            border = JBUI.Borders.customLine(JBUI.CurrentTheme.Editor.BORDER_COLOR, 1)
        }
        
        panel.add(splitter, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createHeaderPanel(): JPanel {
        val headerPanel = JPanel(BorderLayout())
        headerPanel.border = JBUI.Borders.empty(4, 8)
        
        val titleLabel = JBLabel("Diff Preview").apply {
            font = JBUI.Fonts.label().asBold()
        }
        headerPanel.add(titleLabel, BorderLayout.WEST)
        
        // Add control buttons
        val buttonPanel = JPanel()
        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.X_AXIS)
        
        val showInDialogButton = JButton("Show in Dialog").apply {
            addActionListener { showInDialog() }
        }
        
        val closeButton = JButton("Close").apply {
            addActionListener { hide() }
        }
        
        buttonPanel.add(showInDialogButton)
        buttonPanel.add(Box.createHorizontalStrut(4))
        buttonPanel.add(closeButton)
        
        headerPanel.add(buttonPanel, BorderLayout.EAST)
        
        return headerPanel
    }
    
    private fun createErrorPanel(errorMessage: String): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        
        val headerPanel = createHeaderPanel()
        panel.add(headerPanel, BorderLayout.NORTH)
        
        val errorLabel = JBLabel("Error: $errorMessage").apply {
            horizontalAlignment = SwingConstants.CENTER
            foreground = JBColor.RED
        }
        
        panel.add(errorLabel, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun setupDocumentListener() {
        val document = sourceEditor.document
        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (!disposed) {
                    // Optionally refresh preview when document changes
                    // refresh(originalContent, document.text)
                }
            }
        }, this)
    }
    
    /**
     * Show the diff preview
     */
    fun show() {
        if (disposed) return
        isVisible = true
        // Implementation depends on how you want to display the preview
        // This could be in a popup, side panel, or overlay
    }
    
    /**
     * Hide the diff preview
     */
    fun hide() {
        isVisible = false
        // Hide the preview component
    }
    
    /**
     * Show diff in a separate dialog
     */
    fun showInDialog() {
        if (disposed) return
        
        runInEdt {
            val leftContent = diffContentFactory.create(originalContent)
            val rightContent = diffContentFactory.create(modifiedContent)
            
            val diffRequest = SimpleDiffRequest(
                "Diff Preview",
                leftContent,
                rightContent,
                "Original",
                "Modified"
            )
            
            DiffManager.getInstance().showDiff(project, diffRequest)
        }
    }
    
    /**
     * Refresh the preview with new content
     */
    fun refresh(newOriginalContent: String, newModifiedContent: String) {
        if (disposed) return
        
        runInEdt {
            // Recreate the preview component with new content
            createPreviewComponent()
        }
    }
    
    /**
     * Get the preview component for embedding
     */
    fun getComponent(): JComponent? = previewPanel
    
    /**
     * Check if preview is currently visible
     */
    fun isVisible(): Boolean = isVisible
    
    /**
     * Get the source editor
     */
    fun getSourceEditor(): Editor = sourceEditor
    
    /**
     * Get original content
     */
    fun getOriginalContent(): String = originalContent
    
    /**
     * Get modified content
     */
    fun getModifiedContent(): String = modifiedContent
    
    /**
     * Create action group for preview operations
     */
    fun createActionGroup(): DefaultActionGroup {
        val actionGroup = DefaultActionGroup()
        
        actionGroup.add(object : AnAction("Show Preview") {
            override fun actionPerformed(e: AnActionEvent) {
                show()
            }
            
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = !isVisible
            }
        })
        
        actionGroup.add(object : AnAction("Hide Preview") {
            override fun actionPerformed(e: AnActionEvent) {
                hide()
            }
            
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = isVisible
            }
        })
        
        actionGroup.add(object : AnAction("Show in Dialog") {
            override fun actionPerformed(e: AnActionEvent) {
                showInDialog()
            }
        })
        
        return actionGroup
    }
    
    override fun dispose() {
        if (disposed) return
        disposed = true
        
        isVisible = false
        diffComponent = null
        previewPanel = null
    }
}
