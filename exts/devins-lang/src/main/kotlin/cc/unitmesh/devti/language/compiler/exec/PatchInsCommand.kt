package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diff.impl.patch.FilePatch
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.patch.AbstractFilePatchInProgress
import com.intellij.openapi.vcs.changes.patch.ApplyPatchDefaultExecutor
import com.intellij.openapi.vcs.changes.patch.MatchPatchPaths
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MultiMap

class PatchInsCommand(val myProject: Project, val prop: String, val codeContent: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.PATCH

    override suspend fun execute(): String? {
        FileDocumentManager.getInstance().saveAllDocuments()

        val myReader = PatchReader(codeContent)
        myReader.parseAllPatches()

        val filePatches: MutableList<FilePatch> = myReader.allPatches

        ApplicationManager.getApplication().invokeAndWait {
            val matchedPatches = MatchPatchPaths(myProject).execute(filePatches, true)

            val patchGroups = MultiMap<VirtualFile, AbstractFilePatchInProgress<*>>()
            for (patchInProgress in matchedPatches) {
                patchGroups.putValue(patchInProgress.base, patchInProgress)
            }

            if (patchGroups.isEmpty) return@invokeAndWait
            /// open file in editor
            filePatches.firstOrNull()?.apply {
                val file = myProject.baseDir.findFileByRelativePath(this.beforeFileName.toString())
                file?.let {
                    ApplicationManager.getApplication().invokeAndWait {
                        FileEditorManager.getInstance(myProject).openFile(it, true)
                    }
                }
            }

            val additionalInfo = myReader.getAdditionalInfo(ApplyPatchDefaultExecutor.pathsFromGroups(patchGroups))
            ApplyPatchDefaultExecutor(myProject).apply(filePatches, patchGroups, null, prop, additionalInfo)
        }

        return "Applied ${filePatches.size} patches."
    }
}