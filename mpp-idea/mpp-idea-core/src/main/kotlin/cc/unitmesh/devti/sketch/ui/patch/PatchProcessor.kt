package cc.unitmesh.devti.sketch.ui.patch

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.settings.coder.coderSetting
import cc.unitmesh.devti.util.DirUtil
import com.intellij.openapi.application.*
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.*
import com.intellij.openapi.diff.impl.patch.apply.GenericPatchApplier
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.diff.editor.DiffVirtualFileBase
import java.io.IOException
import java.nio.charset.Charset

/**
 * Handles patch processing operations including application, repair, and status checking
 */
class PatchProcessor(private val project: Project) {
    
    private val logger = logger<PatchProcessor>()
    
    /**
     * Applies a patch to the original code and returns the result
     */
    fun applyPatch(originalCode: String, patch: TextFilePatch): GenericPatchApplier.AppliedPatch? {
        return try {
            GenericPatchApplier.apply(originalCode, patch.hunks)
        } catch (e: Exception) {
            logger.warn(AutoDevBundle.message("sketch.patch.failed.apply", patch.beforeFileName ?: ""), e)
            null
        }
    }
    
    /**
     * Checks if a patch application failed
     */
    fun isFailure(appliedPatch: GenericPatchApplier.AppliedPatch?): Boolean {
        return appliedPatch?.status != ApplyPatchStatus.SUCCESS
                && appliedPatch?.status != ApplyPatchStatus.ALREADY_APPLIED
                && appliedPatch?.status != ApplyPatchStatus.PARTIAL
    }
    
    /**
     * Applies a patch to a file in the editor
     */
    fun applyPatchToFile(
        file: VirtualFile,
        appliedPatch: GenericPatchApplier.AppliedPatch?,
        onSuccess: () -> Unit = {}
    ) {
        if (appliedPatch == null || isFailure(appliedPatch)) {
            logger.error("Cannot apply failed patch to file: ${file.path}")
            return
        }
        
        if (file is LightVirtualFile) {
            handleLightVirtualFile(file, appliedPatch, onSuccess)
        } else {
            handleRegularFile(file, appliedPatch, onSuccess)
        }
    }
    
    private fun handleLightVirtualFile(
        file: LightVirtualFile,
        appliedPatch: GenericPatchApplier.AppliedPatch,
        onSuccess: () -> Unit
    ) {
        val fileName = file.name.substringAfterLast("/")
        val filePath = file.path.substringBeforeLast(fileName)
        
        try {
            runReadAction {
                val directory = DirUtil.getOrCreateDirectory(project.baseDir, filePath)
                val vfile = runWriteAction { directory.createChildData(this, fileName) }
                vfile.writeText(appliedPatch.patchedText)
                
                FileEditorManager.getInstance(project).openFile(vfile, true)
                onSuccess()
            }
        } catch (e: Exception) {
            logger.error("Failed to create file: ${file.path}", e)
        }
    }
    
    private fun handleRegularFile(
        file: VirtualFile,
        appliedPatch: GenericPatchApplier.AppliedPatch,
        onSuccess: () -> Unit
    ) {
        val document = FileDocumentManager.getInstance().getDocument(file)
        if (document == null) {
            logger.error(AutoDevBundle.message("sketch.patch.document.null", file.path))
            return
        }
        
        CommandProcessor.getInstance().executeCommand(project, {
            WriteCommandAction.runWriteCommandAction(project) {
                document.setText(appliedPatch.patchedText)
                
                if (file is DiffVirtualFileBase) {
                    FileEditorManager.getInstance(project).closeFile(file)
                } else {
                    FileEditorManager.getInstance(project).openFile(file, true)
                }
                onSuccess()
            }
        }, "ApplyPatch", null)
    }
    
    /**
     * Performs auto repair on a patch
     */
    fun performAutoRepair(
        oldCode: String,
        patch: TextFilePatch,
        onRepaired: (TextFilePatch, String) -> Unit
    ) {
        val failurePatch = if (patch.hunks.size > 1) {
            patch.hunks.joinToString("\n") { it.text }
        } else {
            patch.singleHunkPatchText
        }
        
        DiffRepair.applyDiffRepairSuggestionSync(project, oldCode, failurePatch) { fixedCode ->
            createPatchFromCode(oldCode, fixedCode)?.let { repairedPatch ->
                onRepaired(repairedPatch, fixedCode)
            }
        }
    }
    
    /**
     * Registers a patch change with the agent state service
     */
    fun registerPatchChange(patch: TextFilePatch) {
        if (project.coderSetting.state.enableDiffViewer) {
            project.getService<AgentStateService>(AgentStateService::class.java)
                .addToChange(patch)
        }
    }
}
