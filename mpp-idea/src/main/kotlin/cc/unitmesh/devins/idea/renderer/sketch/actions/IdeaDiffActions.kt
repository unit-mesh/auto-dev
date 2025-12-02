package cc.unitmesh.devins.idea.renderer.sketch.actions

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.sketch.ui.patch.DiffRepair
import cc.unitmesh.devti.sketch.ui.patch.showSingleDiff
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.patch.AbstractFilePatchInProgress
import com.intellij.openapi.vcs.changes.patch.ApplyPatchDefaultExecutor
import com.intellij.openapi.vcs.changes.patch.MatchPatchPaths
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.containers.MultiMap

/**
 * Business logic actions for Diff/Patch operations in mpp-idea.
 * Reuses core module's PatchProcessor, DiffRepair, and showSingleDiff logic.
 * 
 * Related GitHub Issue: https://github.com/phodal/auto-dev/issues/25
 */
object IdeaDiffActions {
    
    /**
     * Parse patch content and return file patches
     */
    fun parsePatches(patchContent: String): List<TextFilePatch> {
        return try {
            val reader = PatchReader(patchContent)
            reader.parseAllPatches()
            reader.textPatches
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Accept and apply patch to files
     * @return true if patch was applied successfully
     */
    fun acceptPatch(project: Project, patchContent: String): Boolean {
        val filePatches = parsePatches(patchContent)
        if (filePatches.isEmpty()) {
            AutoDevNotifications.error(project, "No valid patches found")
            return false
        }
        
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val commandProcessor = CommandProcessor.getInstance()
        val shelfExecutor = ApplyPatchDefaultExecutor(project)
        
        var success = false
        commandProcessor.executeCommand(project, {
            commandProcessor.markCurrentCommandAsGlobal(project)
            
            val matchedPatches = MatchPatchPaths(project).execute(filePatches, true)
            val patchGroups = MultiMap<VirtualFile, AbstractFilePatchInProgress<*>>()
            for (patchInProgress in matchedPatches) {
                patchGroups.putValue(patchInProgress.base, patchInProgress)
            }
            
            val pathsFromGroups = ApplyPatchDefaultExecutor.pathsFromGroups(patchGroups)
            val reader = PatchReader(patchContent)
            reader.parseAllPatches()
            val additionalInfo = reader.getAdditionalInfo(pathsFromGroups)
            
            shelfExecutor.apply(filePatches, patchGroups, null, "AutoDev.diff", additionalInfo)
            success = true
        }, "ApplyPatch", null, UndoConfirmationPolicy.REQUEST_CONFIRMATION, false)
        
        return success
    }
    
    /**
     * Reject/Undo the last patch application
     * @return true if undo was performed
     */
    fun rejectPatch(project: Project): Boolean {
        val undoManager = UndoManager.getInstance(project)
        val fileEditor = FileEditorManager.getInstance(project).selectedEditor ?: return false
        
        if (undoManager.isUndoAvailable(fileEditor)) {
            undoManager.undo(fileEditor)
            return true
        }
        return false
    }
    
    /**
     * Show diff preview dialog
     * @param onAccept callback when user clicks Accept in the dialog
     */
    fun viewDiff(project: Project, patchContent: String, onAccept: (() -> Unit)? = null) {
        showSingleDiff(project, patchContent, onAccept)
    }
    
    /**
     * Repair a failed patch using AI
     * @param onRepaired callback with the repaired code
     */
    fun repairPatch(
        project: Project,
        patchContent: String,
        onRepaired: ((String) -> Unit)? = null
    ) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor == null) {
            AutoDevNotifications.error(project, "No editor available for repair")
            return
        }
        
        ApplicationManager.getApplication().invokeLater {
            DiffRepair.applyDiffRepairSuggestion(
                project,
                editor,
                editor.document.text,
                patchContent
            ) { repairedCode ->
                onRepaired?.invoke(repairedCode)
            }
        }
    }
    
    /**
     * Repair patch synchronously (for background processing)
     */
    fun repairPatchSync(
        project: Project,
        originalCode: String,
        patchContent: String,
        onComplete: (String) -> Unit
    ) {
        DiffRepair.applyDiffRepairSuggestionSync(project, originalCode, patchContent, onComplete)
    }
    
    /**
     * Check if patches are valid
     */
    fun hasValidPatches(patchContent: String): Boolean {
        return parsePatches(patchContent).isNotEmpty()
    }
}
