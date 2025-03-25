package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.sketch.AutoSketchMode
import cc.unitmesh.devti.sketch.ui.patch.SingleFileDiffSketch
import cc.unitmesh.devti.sketch.ui.patch.readText
import cc.unitmesh.devti.sketch.ui.patch.writeText
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.ApplyPatchStatus
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.diff.impl.patch.apply.GenericPatchApplier
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Disposer

class PatchInsCommand(val myProject: Project, val prop: String, val codeContent: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.PATCH

    private val logger = logger<PatchInsCommand>()

    override suspend fun execute(): String? {

        val filePatches = parsePatches(codeContent)
        if (filePatches.isEmpty()) {
            logger.warn("No patches found in content")
            return "$DEVINS_ERROR: No patches found"
        }

        val disposable = Disposer.newCheckedDisposable()
        var result: String? = null

        runInEdtAsync(disposable) {
            filePatches.forEach {
                val vfile = myProject.guessProjectDir()!!.findFileByRelativePath(it.beforeName.toString())
                if (vfile == null) {
                    result += "$DEVINS_ERROR: File not found: ${it.beforeName}"
                    return@runInEdtAsync
                }

                var appliedPatch = try {
                    GenericPatchApplier.apply(vfile.readText(), it.hunks)
                } catch (e: Exception) {
                    result += "$DEVINS_ERROR: Failed to apply patch: ${it.beforeName}, ${e.message}"
                    null
                } ?: return@runInEdtAsync

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

    private fun parsePatches(content: String): List<TextFilePatch> {
        return try {
            PatchReader(content).apply { parseAllPatches() }.textPatches
        } catch (e: Exception) {
            logger.error("Failed to parse patches", e)
            emptyList()
        }
    }
}
