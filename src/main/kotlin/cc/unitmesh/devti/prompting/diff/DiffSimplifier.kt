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
            val length = lines.size
            val destination = ArrayList<String>()
            var index = 0
            while (true) {
                if (index >= lines.size) {
                    break
                }

                val line = lines[index]

                if (line.startsWith("diff --git ") || line.startsWith("index:") || line.startsWith("Index:")) {
                    index++
                    continue
                }

                if (line == "===================================================================") {
                    index++
                    continue
                }

                // if a patch includes `\ No newline at the end of file` remove it
                if (line.contains(lineTip)) {
                    index++
                    continue
                }

                if (line.startsWith("--- /dev/null")) {
                    index++
                    continue
                }


                // todo: spike for handle for new file
                if (line.startsWith("@@") && line.endsWith("@@")) {
                    index++
                    continue
                }

                // handle for new file
                if (line.startsWith("new file mode")) {
                    val nextLine = lines[index + 1]
                    if (nextLine.startsWith("--- /dev/null")) {
                        val nextNextLine = lines[index + 2]
                        val withoutHead = nextNextLine.substring("+++ b/".length)
                        // footer: 	(date 1704768267000)
                        val withoutFooter = withoutHead.substring(0, withoutHead.indexOf("\t"))
                        destination.add("new file $withoutFooter")
                        index += 3
                        continue
                    }
                }

                // handle for rename
                if (line.startsWith("rename from")) {
                    val nextLine = lines[index + 1]
                    if (nextLine.startsWith("rename to")) {
                        val from = line.substring("rename from ".length)
                        val to = nextLine.substring("rename to ".length)
                        destination.add("rename file $from $to")
                        // The next value will be "---" and the value after that will be "+++".
                        index += 4
                        continue
                    }
                }

                // handle for delete
                if (line.startsWith("deleted file mode")) {
                    val nextLine = lines[index + 1]
                    if (nextLine.startsWith("--- a/")) {
                        val withoutHead = nextLine.substring("--- a/".length)
                        // footer: 	(date 1704768267000)
                        val withoutFooter = withoutHead.substring(0, withoutHead.indexOf("\t"))
                        destination.add("delete file $withoutFooter")
                        // search for the next line starts with "Index:"
                        while (true) {
                            if (index + 2 >= length) {
                                break
                            }

                            val nextNextLine = lines[index + 2]
                            if (nextNextLine.startsWith("Index:")) {
                                index += 3
                                break
                            }
                            index++
                        }

                        continue
                    }
                }

                if (line.startsWith("---") || line.startsWith("+++")) {
                    // remove revision number with regex
                    val result = revisionRegex.replace(line, "")
                    destination.add(result)
                } else {
                    destination.add(line)
                }

                index++
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