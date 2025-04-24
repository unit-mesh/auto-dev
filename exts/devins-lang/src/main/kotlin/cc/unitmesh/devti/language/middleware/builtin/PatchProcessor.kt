package cc.unitmesh.devti.language.middleware.builtin

import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diff.impl.patch.FilePatch
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.patch.AbstractFilePatchInProgress
import com.intellij.openapi.vcs.changes.patch.ApplyPatchDefaultExecutor
import com.intellij.openapi.vcs.changes.patch.MatchPatchPaths
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MultiMap
import cc.unitmesh.devti.devins.post.PostProcessor
import cc.unitmesh.devti.devins.post.PostProcessorContext
import cc.unitmesh.devti.devins.post.PostProcessorType

class PatchProcessor : PostProcessor {
    override val processorName: String = PostProcessorType.Patch.handleName
    override val description: String = "`patch` will apply the patch to the current file, default will use llm response."

    override fun isApplicable(context: PostProcessorContext): Boolean = true

    override fun execute(
        project: Project,
        context: PostProcessorContext,
        console: ConsoleView?,
        args: List<Any>,
    ): Any {
        val args = args.map {
            val argName = it.toString()
            if (argName.startsWith("$")) {
                if (argName == "output" && context.lastTaskOutput != null) {
                    context.lastTaskOutput
                } else {
                    context.compiledVariables[argName.substring(1)] ?: ""
                }
            } else {
                it
            }
        }

        if (args.size < 2) {
            console?.print("PatchProcessor: not enough arguments", ConsoleViewContentType.ERROR_OUTPUT)
            return ""
        }

        val fileName = args[0].toString()
        val content = args[1].toString()

        val shelfExecutor = ApplyPatchDefaultExecutor(project)

        val myReader = PatchReader(content)
        myReader.parseAllPatches()

        val filePatches: MutableList<FilePatch> = myReader.allPatches

        ApplicationManager.getApplication().invokeAndWait {
            val matchedPatches =
                MatchPatchPaths(project).execute(filePatches, true)

            val patchGroups = MultiMap<VirtualFile, AbstractFilePatchInProgress<*>>()
            for (patchInProgress in matchedPatches) {
                patchGroups.putValue(patchInProgress.base, patchInProgress)
            }

            if(filePatches.isEmpty() ) {
                console?.print("PatchProcessor: no patches found", ConsoleViewContentType.ERROR_OUTPUT)
                return@invokeAndWait
            }

            val additionalInfo = myReader.getAdditionalInfo(ApplyPatchDefaultExecutor.pathsFromGroups(patchGroups))
            shelfExecutor.apply(filePatches, patchGroups, null, fileName, additionalInfo)
        }

        return context.genText ?: ""
    }
}
