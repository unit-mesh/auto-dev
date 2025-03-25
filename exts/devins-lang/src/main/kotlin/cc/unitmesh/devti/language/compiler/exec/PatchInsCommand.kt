package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.sketch.AutoSketchMode
import cc.unitmesh.devti.sketch.ui.patch.SingleFileDiffSketch
import cc.unitmesh.devti.sketch.ui.patch.readText
import cc.unitmesh.devti.sketch.ui.patch.writeText
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.ApplyPatchStatus
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.diff.impl.patch.apply.GenericPatchApplier
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Disposer

class PatchInsCommand(val myProject: Project, val prop: String, val codeContent: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.PATCH

    override suspend fun execute(): String? {
        runInEdt {
            FileDocumentManager.getInstance().saveAllDocuments()
        }

        val myReader = PatchReader(codeContent)
        myReader.parseAllPatches()

        val filePatches: MutableList<TextFilePatch> = myReader.textPatches

        val disposable = Disposer.newCheckedDisposable()
        var result: String? = null

        runInEdtAsync(disposable) {
            filePatches.forEach {
                val vfile = myProject.guessProjectDir()!!.findFileByRelativePath(it.beforeName.toString())
                if (vfile == null) {
                    result += "$DEVINS_ERROR: File not found: ${it.beforeName}"
                    return@runInEdtAsync
                }

                ApplicationManager.getApplication().invokeAndWait {
                    FileEditorManager.getInstance(myProject).openFile(vfile, true)
                }

                var oldCode = vfile.readText()

                var appliedPatch = try {
                    GenericPatchApplier.apply(oldCode, it.hunks)
                } catch (e: Exception) {
                    logger<SingleFileDiffSketch>().warn("Failed to apply patch: ${it.beforeName}", e)
                    null
                }

                if (appliedPatch == null) {
                    result += "$DEVINS_ERROR: Failed to apply patch: ${it.beforeName}"
                    return@runInEdtAsync
                }

                if (appliedPatch.status == ApplyPatchStatus.SUCCESS || appliedPatch.status == ApplyPatchStatus.PARTIAL) {
                    vfile.writeText(appliedPatch.patchedText)
                    result += "Patch applied successfully: ${it.beforeName}\n"
                }
            }

            result
        }

        if (AutoSketchMode.getInstance(myProject).isEnable) {
            result = ""
        }

        return result
    }
}
