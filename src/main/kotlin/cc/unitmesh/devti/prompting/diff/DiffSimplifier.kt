package cc.unitmesh.devti.prompting.diff

import com.intellij.openapi.components.Service
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.*
import com.intellij.project.stateStore
import org.jetbrains.annotations.NotNull
import java.io.StringWriter
import java.nio.file.Path
import java.nio.file.PathMatcher
import kotlin.math.min

@Service(Service.Level.PROJECT)
class DiffSimplifier(val project: Project) {
    fun simplify(changes: List<Change>, ignoreFilePatterns: List<PathMatcher>): String {
        try {
            val writer = StringWriter()
            val basePath = project.basePath ?: throw RuntimeException("Project base path is null.")

            val filteredChanges = changes.stream()
                .filter { change -> !isBinaryOrTooLarge(change!!) }
                .filter {
                    val filePath = it.afterRevision?.file
                    if (filePath != null) {
                        ignoreFilePatterns.none { pattern ->
                            pattern.matches(Path.of(it.afterRevision!!.file.path))
                        }
                    } else {
                        true
                    }
                }
                .toList()

            if (filteredChanges.isEmpty()) {
                return ""
            }

            val patches = IdeaTextPatchBuilder.buildPatch(
                project,
                filteredChanges.subList(0, min(filteredChanges.size, 500)),
                Path.of(basePath),
                false,
                true
            )


            UnifiedDiffWriter.write(
                project,
                project.stateStore.projectBasePath,
                patches,
                writer,
                "\n",
                null as CommitContext?,
                emptyList()
            )
            val diffString = writer.toString()
            return postProcess(diffString)
        } catch (e: VcsException) {
            throw RuntimeException("Error calculating diff: ${e.message}", e)
        }
    }

    companion object {
        private val revisionRegex = Regex("\\(revision [^)]+\\)")
        private val lineTip = "\\ No newline at end of file"

        @NotNull
        fun postProcess(@NotNull diffString: String): String {
            val lines = diffString.lines()
            val destination = ArrayList<String>()
            lines.forEach { line ->
                if (line.startsWith("diff --git ") || line.startsWith("index ") || line.startsWith("Index ")) return@forEach

                if (line == "===================================================================") return@forEach

                // if a patch includes `\ No newline at the end of file` remove it
                if (line.contains(lineTip)) {
                    return@forEach
                }

                // skip for empty lines
                if (line.startsWith("--- /dev/null")) {
                    return@forEach
                }

                if (line.startsWith("@@") && line.endsWith("@@")) {
                    return@forEach
                }

                if (line.startsWith("---") || line.startsWith("+++")) {
                    // remove revision number with regex
                    val result = revisionRegex.replace(line, "")
                    destination.add(result)
                } else {
                    destination.add(line)
                }
            }

            return destination.joinToString("\n")
        }

        private fun isBinaryOrTooLarge(@NotNull change: Change): Boolean {
            return isBinaryOrTooLarge(change.beforeRevision) || isBinaryOrTooLarge(change.afterRevision)
        }

        private fun isBinaryOrTooLarge(revision: ContentRevision?): Boolean {
            val virtualFile = (revision as? CurrentContentRevision)?.virtualFile
            return revision != null && (isBinaryRevision(revision) || (virtualFile != null && FileUtilRt.isTooLarge(
                virtualFile.length
            )))
        }

        private fun isBinaryRevision(cr: ContentRevision?): Boolean {
            if (cr == null) return false
            return if (cr is BinaryContentRevision) true else cr.file.fileType.isBinary
        }
    }
}