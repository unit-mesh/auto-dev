package cc.unitmesh.devti.language.compiler.exec

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diff.impl.patch.FilePatch
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.patch.AbstractFilePatchInProgress
import com.intellij.openapi.vcs.changes.patch.ApplyPatchDefaultExecutor
import com.intellij.openapi.vcs.changes.patch.MatchPatchPaths
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MultiMap

class PatchInsCommand(val myProject: Project, val prop: String, val codeContent: String) : InsCommand {
    override fun execute(): String? {
        FileDocumentManager.getInstance().saveAllDocuments()

        val shelfExecutor = ApplyPatchDefaultExecutor(myProject)

        val myReader = PatchReader(codeContent)
        myReader.parseAllPatches()

        val filePatches: MutableList<FilePatch> = myReader.allPatches
        val matchedPatches =
            MatchPatchPaths(myProject).execute(filePatches, true)

        val patchGroups = MultiMap<VirtualFile, AbstractFilePatchInProgress<*>>()
        for (patchInProgress in matchedPatches) {
            patchGroups.putValue(patchInProgress.base, patchInProgress)
        }

        ApplicationManager.getApplication().invokeLater {
            val additionalInfo = myReader.getAdditionalInfo(ApplyPatchDefaultExecutor.pathsFromGroups(patchGroups))
            shelfExecutor.apply(filePatches, patchGroups, null, prop, additionalInfo)
        }

        return "Patch applied"
    }

}