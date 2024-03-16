package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diff.impl.patch.FilePatch
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.openapi.vcs.changes.patch.MatchPatchPaths
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import java.io.IOException
import java.io.InputStreamReader

class PatchInsCommand(val myProject: Project, val prop: String, val codeContent: String) : InsCommand {
    override fun execute(): String? {
        FileDocumentManager.getInstance().saveAllDocuments()

        val patchFile = LightVirtualFile(prop, codeContent)

        val myReader: PatchReader = loadPatches(patchFile) ?: return "<DevliError>: Cannot read patch file."
        myReader.parseAllPatches()

        val filePatches: MutableList<FilePatch> = ArrayList()
        filePatches.addAll(myReader.allPatches)


        val matchedPatches =
            MatchPatchPaths(myProject).execute(filePatches, true)

        ApplicationManager.getApplication().invokeLater {
            matchedPatches
        }

        return "Patch applied"
    }

    private fun loadPatches(patchFile: VirtualFile): PatchReader? {
        try {
            val text = ReadAction.compute<String, IOException> {
                InputStreamReader(patchFile.inputStream, patchFile.charset)
                    .use { inputStreamReader ->
                        return@compute StreamUtil.readText(inputStreamReader)
                    }
            }

            val reader = PatchReader(text)
            reader.parseAllPatches()
            return reader
        } catch (e: Exception) {
            AutoDevBundle.message("devin.patch.cannot.read.patch", patchFile.presentableName)
            return null
        }
    }

}