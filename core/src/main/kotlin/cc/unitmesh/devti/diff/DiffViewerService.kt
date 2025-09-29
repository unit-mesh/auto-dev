package cc.unitmesh.devti.diff

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.DiffRequestFactory
import com.intellij.diff.chains.DiffRequestChain
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing diff viewers and providing diff functionality
 * Similar to IntelliJ Augment's DiffViewerService
 */
@Service(Service.Level.PROJECT)
class DiffViewerService(private val project: Project) {
    
    private val diffContentFactory = DiffContentFactory.getInstance()
    private val diffRequestFactory = DiffRequestFactory.getInstance()
    private val diffManager = DiffManager.getInstance()
    
    // Track active diff processors
    private val activeDiffProcessors = ConcurrentHashMap<String, StandaloneDiffRequestProcessor>()
    
    // Track active diff previews
    private val activeDiffPreviews = ConcurrentHashMap<String, StandaloneEditorTabDiffPreview>()
    
    companion object {
        fun getInstance(project: Project): DiffViewerService {
            return project.getService(DiffViewerService::class.java)
        }
    }
    
    /**
     * Show diff between two text contents in a dialog
     */
    fun showDiffDialog(
        leftContent: String,
        rightContent: String,
        leftTitle: String = "Original",
        rightTitle: String = "Modified",
        dialogTitle: String = "Diff Viewer"
    ) {
        runInEdt {
            val leftDiffContent = diffContentFactory.create(leftContent)
            val rightDiffContent = diffContentFactory.create(rightContent)
            
            val diffRequest = SimpleDiffRequest(dialogTitle, leftDiffContent, rightDiffContent, leftTitle, rightTitle)
            diffManager.showDiff(project, diffRequest, DiffDialogHints.DEFAULT)
        }
    }
    
    /**
     * Show diff between a file and text content
     */
    fun showFileDiffDialog(
        file: VirtualFile,
        newContent: String,
        newContentTitle: String = "Modified",
        dialogTitle: String = "File Diff"
    ) {
        runInEdt {
            val fileDiffContent = diffContentFactory.create(project, file)
            val newDiffContent = diffContentFactory.create(newContent)
            
            val diffRequest = SimpleDiffRequest(
                dialogTitle, 
                fileDiffContent, 
                newDiffContent, 
                file.name, 
                newContentTitle
            )
            diffManager.showDiff(project, diffRequest, DiffDialogHints.DEFAULT)
        }
    }
    
    /**
     * Create a standalone diff request processor
     */
    fun createStandaloneDiffProcessor(
        leftContent: String,
        rightContent: String,
        leftTitle: String = "Original",
        rightTitle: String = "Modified",
        processorId: String = generateProcessorId()
    ): StandaloneDiffRequestProcessor {
        val processor = StandaloneDiffRequestProcessor(
            project,
            leftContent,
            rightContent,
            leftTitle,
            rightTitle,
            processorId
        )
        
        activeDiffProcessors[processorId] = processor
        
        // Clean up when processor is disposed
        Disposer.register(processor) {
            activeDiffProcessors.remove(processorId)
        }
        
        return processor
    }
    
    /**
     * Create a standalone editor tab diff preview
     */
    fun createEditorTabDiffPreview(
        editor: Editor,
        originalContent: String,
        modifiedContent: String,
        previewId: String = generatePreviewId()
    ): StandaloneEditorTabDiffPreview {
        val preview = StandaloneEditorTabDiffPreview(
            project,
            editor,
            originalContent,
            modifiedContent,
            previewId
        )
        
        activeDiffPreviews[previewId] = preview
        
        // Clean up when preview is disposed
        Disposer.register(preview) {
            activeDiffPreviews.remove(previewId)
        }
        
        return preview
    }
    
    /**
     * Get active diff processor by ID
     */
    fun getDiffProcessor(processorId: String): StandaloneDiffRequestProcessor? {
        return activeDiffProcessors[processorId]
    }
    
    /**
     * Get active diff preview by ID
     */
    fun getDiffPreview(previewId: String): StandaloneEditorTabDiffPreview? {
        return activeDiffPreviews[previewId]
    }
    
    /**
     * Close all active diff processors and previews
     */
    fun closeAll() {
        activeDiffProcessors.values.forEach { processor ->
            if (!processor.isDisposed) {
                Disposer.dispose(processor)
            }
        }
        activeDiffProcessors.clear()
        
        activeDiffPreviews.values.forEach { preview ->
            if (!preview.isDisposed) {
                Disposer.dispose(preview)
            }
        }
        activeDiffPreviews.clear()
    }
    
    /**
     * Create a diff request chain for multiple diffs
     */
    fun createDiffChain(diffs: List<DiffRequestData>): DiffRequestChain {
        val requests = diffs.map { diffData ->
            val leftContent = diffContentFactory.create(diffData.leftContent)
            val rightContent = diffContentFactory.create(diffData.rightContent)
            SimpleDiffRequest(diffData.title, leftContent, rightContent, diffData.leftTitle, diffData.rightTitle)
        }

        return SimpleDiffRequestChain(requests)
    }
    
    /**
     * Show multiple diffs in a chain
     */
    fun showDiffChain(diffs: List<DiffRequestData>) {
        if (diffs.isEmpty()) return
        
        runInEdt {
            val chain = createDiffChain(diffs)
            diffManager.showDiff(project, chain, DiffDialogHints.DEFAULT)
        }
    }
    
    private fun generateProcessorId(): String {
        return "diff_processor_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }
    
    private fun generatePreviewId(): String {
        return "diff_preview_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }
}

/**
 * Data class for diff request information
 */
data class DiffRequestData(
    val leftContent: String,
    val rightContent: String,
    val leftTitle: String = "Original",
    val rightTitle: String = "Modified",
    val title: String = "Diff"
)
