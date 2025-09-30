package cc.unitmesh.devti.diff.service

import com.intellij.diff.*
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.JBSplitter
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

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
     * Create diff component for embedding
     */
    fun createDiffComponent(): JComponent? {
        if (disposed) return null

        return try {
            val request = diffRequest ?: return null
            val panel = JPanel(BorderLayout())

            val leftTextArea = JTextArea(leftContent).apply {
                isEditable = false
                font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            }

            val rightTextArea = JTextArea(rightContent).apply {
                isEditable = false
                font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            }

            val splitter = JBSplitter(false, 0.5f)
            splitter.firstComponent = JScrollPane(leftTextArea)
            splitter.secondComponent = JScrollPane(rightTextArea)

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
    
    override fun dispose() {
        if (disposed) return
        disposed = true
        
        diffComponent = null
        diffContext = null
        diffTool = null
        diffRequest = null
    }
}


