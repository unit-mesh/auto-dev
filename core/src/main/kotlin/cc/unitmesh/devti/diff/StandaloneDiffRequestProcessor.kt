package cc.unitmesh.devti.diff

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffContext
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.tools.util.DiffDataKeys
import com.intellij.diff.util.DiffUserDataKeys
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Standalone diff request processor that can handle diff operations independently
 * Similar to IntelliJ Augment's StandaloneDiffRequestProcessor
 */
class StandaloneDiffRequestProcessor(
    private val project: Project,
    private val leftContent: String,
    private val rightContent: String,
    private val leftTitle: String,
    private val rightTitle: String,
    val processorId: String
) : Disposable {
    
    private val diffContentFactory = DiffContentFactory.getInstance()
    private val diffManager = DiffManager.getInstance()
    
    private var diffRequest: DiffRequest? = null
    private var diffContext: DiffContext? = null
    private var diffTool: FrameDiffTool? = null
    private var diffComponent: JComponent? = null
    
    @Volatile
    private var disposed = false
    
    val isDisposed: Boolean
        get() = disposed
    
    companion object {
        private val PROCESSOR_KEY = Key.create<StandaloneDiffRequestProcessor>("StandaloneDiffRequestProcessor")
    }
    
    init {
        createDiffRequest()
    }
    
    private fun createDiffRequest() {
        val leftDiffContent = diffContentFactory.create(leftContent)
        val rightDiffContent = diffContentFactory.create(rightContent)
        
        diffRequest = SimpleDiffRequest(
            "Diff: $leftTitle vs $rightTitle",
            leftDiffContent,
            rightDiffContent,
            leftTitle,
            rightTitle
        ).apply {
            // Add processor reference to the request
            putUserData(PROCESSOR_KEY, this@StandaloneDiffRequestProcessor)
        }
    }
    
    /**
     * Get the diff request
     */
    fun getDiffRequest(): DiffRequest? = diffRequest
    
    /**
     * Show diff in a dialog
     */
    fun showInDialog() {
        if (disposed) return
        
        runInEdt {
            diffRequest?.let { request ->
                diffManager.showDiff(project, request, DiffDialogHints.DEFAULT)
            }
        }
    }
    
    /**
     * Show diff in a new editor tab
     */
    fun showInNewTab(tabTitle: String = "Diff") {
        if (disposed) return
        
        runInEdt {
            try {
                val fileEditorManager = FileEditorManager.getInstance(project)
                
                // Create a virtual file for the diff
                val diffVirtualFile = DiffVirtualFile(tabTitle, this)
                
                // Open the diff in a new tab
                fileEditorManager.openFile(diffVirtualFile, true)
            } catch (e: Exception) {
                // Fallback to dialog if tab creation fails
                showInDialog()
            }
        }
    }
    
    /**
     * Create diff component for embedding
     */
    fun createDiffComponent(): JComponent? {
        if (disposed) return null

        return try {
            val request = diffRequest ?: return null
            // Note: DiffContext is abstract, so we'll create a simple diff display instead

            // Use DiffManager to show diff in a component
            // This is a simplified approach - in a real implementation you might need
            // to use more specific diff tools
            val panel = JPanel(BorderLayout())

            // Create a simple text-based diff display as fallback
            val leftTextArea = javax.swing.JTextArea(leftContent).apply {
                isEditable = false
                font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12)
            }

            val rightTextArea = javax.swing.JTextArea(rightContent).apply {
                isEditable = false
                font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12)
            }

            val splitter = com.intellij.ui.JBSplitter(false, 0.5f)
            splitter.firstComponent = javax.swing.JScrollPane(leftTextArea)
            splitter.secondComponent = javax.swing.JScrollPane(rightTextArea)

            panel.add(splitter, BorderLayout.CENTER)
            diffComponent = panel

            panel
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Refresh the diff content
     */
    fun refresh(newLeftContent: String, newRightContent: String) {
        if (disposed) return
        
        runInEdt {
            val leftDiffContent = diffContentFactory.create(newLeftContent)
            val rightDiffContent = diffContentFactory.create(newRightContent)
            
            diffRequest = SimpleDiffRequest(
                "Diff: $leftTitle vs $rightTitle",
                leftDiffContent,
                rightDiffContent,
                leftTitle,
                rightTitle
            ).apply {
                putUserData(PROCESSOR_KEY, this@StandaloneDiffRequestProcessor)
            }
            
            // Recreate component if it exists
            diffComponent?.let {
                createDiffComponent()
            }
        }
    }
    
    /**
     * Get current left content
     */
    fun getLeftContent(): String = leftContent
    
    /**
     * Get current right content
     */
    fun getRightContent(): String = rightContent
    
    /**
     * Get left title
     */
    fun getLeftTitle(): String = leftTitle
    
    /**
     * Get right title
     */
    fun getRightTitle(): String = rightTitle
    
    /**
     * Create action group for diff operations
     */
    fun createActionGroup(): DefaultActionGroup {
        val actionGroup = DefaultActionGroup()
        
        actionGroup.add(object : AnAction("Show in Dialog") {
            override fun actionPerformed(e: AnActionEvent) {
                showInDialog()
            }
        })
        
        actionGroup.add(object : AnAction("Show in New Tab") {
            override fun actionPerformed(e: AnActionEvent) {
                showInNewTab()
            }
        })
        
        actionGroup.add(object : AnAction("Refresh") {
            override fun actionPerformed(e: AnActionEvent) {
                refresh(leftContent, rightContent)
            }
        })
        
        return actionGroup
    }
    
    override fun dispose() {
        if (disposed) return
        disposed = true
        
        diffComponent = null
        diffContext = null
        diffTool = null
        diffRequest = null
    }
}


