package cc.unitmesh.devti.vcs

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vcs.changes.*
import com.intellij.project.stateStore
import git4idea.config.GitExecutableManager
import org.jetbrains.annotations.NotNull
import java.io.StringWriter
import java.nio.file.Path
import java.nio.file.PathMatcher
import kotlin.math.min

@Service(Service.Level.PROJECT)
class DiffSimplifier(val project: Project) {
    private val logger = logger<DiffSimplifier>()

    /**
     * Simplifies the given list of changes and returns the resulting diff as a string.
     *
     * @param changes The list of changes to be simplified.
     * @param ignoreFilePatterns The list of file patterns to be ignored during simplification.
     * @return The simplified diff as a string.
     * @throws RuntimeException if the project base path is null or if there is an error calculating the diff.
     */
    fun simplify(changes: List<Change>, ignoreFilePatterns: List<PathMatcher>): String {
        var originChanges = ""

        try {
            val writer = StringWriter()
            val basePath = project.basePath ?: throw RuntimeException("Project base path is null.")
            val binaryOrTooLargeChanges: List<String> = changes.stream()
                .filter { change -> isBinaryOrTooLarge(change!!) }
                .map {
                    when(it.type) {
                        Change.Type.NEW -> "new file ${it.afterRevision?.file?.path}"
                        Change.Type.DELETED -> "delete file ${it.beforeRevision?.file?.path}"
                        Change.Type.MODIFICATION -> "modify file ${it.beforeRevision?.file?.path}"
                        Change.Type.MOVED -> "rename file from ${it.beforeRevision?.file?.path} to ${it.afterRevision?.file?.path}"
                    }
                }
                .toList()

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


            val limitedChnages = filteredChanges.subList(0, min(filteredChanges.size, 500))

            val patches = IdeaTextPatchBuilder.buildPatch(
                project, limitedChnages, Path.of(basePath), false, true
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

            originChanges = writer.toString()
            originChanges += binaryOrTooLargeChanges.joinToString("\n")
            return postProcess(originChanges)
        } catch (e: Exception) {
            if (originChanges.isNotEmpty()) {
                logger.info("Error calculating diff: $originChanges", e)
            }

            throw RuntimeException("Error calculating diff: ${e.message}", e)
        }
    }

    companion object {
        private val revisionRegex = Regex("\\(revision [^)]+\\)")
        private const val lineTip = "\\ No newline at end of file"

        /**
         * This method is used to process the given diff string and extract relevant information from it.
         *
         * @param diffString The diff string to be processed.
         * @return The processed string containing the extracted information.
         */
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

                if (line.startsWith("---\t/dev/null")) {
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
                        destination.add("rename file from $from to $to")
                        // The next value will be "---" and the value after that will be "+++".
                        index += 4
                        continue
                    }
                }

                // handle for java and kotlin import change
                if (line.startsWith(" import")) {
                    val nextLine = lines.getOrNull(index + 1)
                    if (nextLine?.startsWith(" import") == true) {
                        var oldImportLine = ""
                        var newImportLine = ""
                        // search all import lines until the next line starts with "Index:"
                        val importLines = ArrayList<String>()
                        importLines.add(line)
                        importLines.add(nextLine)

                        var tryToFindIndex = index + 2
                        while (true) {
                            if (tryToFindIndex >= length) {
                                break
                            }

                            val tryLine = lines[tryToFindIndex]
                            when {

                                tryLine.startsWith("Index:") -> {
                                    break
                                }

                                tryLine.startsWith(" import") -> {
                                    importLines.add(tryLine)
                                }

                                tryLine.startsWith("-import ") -> {
                                    oldImportLine = tryLine.substring("-import ".length)
                                    importLines.add(tryLine)
                                }

                                tryLine.startsWith("+import ") -> {
                                    newImportLine = tryLine.substring("+import ".length)
                                    importLines.add(tryLine)
                                }
                            }

                            tryToFindIndex++
                        }

                        if (oldImportLine.isNotEmpty() && newImportLine.isNotEmpty()) {
                            if (importLines.size == tryToFindIndex - index) {
                                index = tryToFindIndex
                                destination.add("change import from $oldImportLine to $newImportLine")
                                continue
                            }
                        }
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
                    // next line
                    val nextLine = lines[index + 1]
                    if (nextLine.startsWith("+++")) {
                        // remove end date
                        val substringBefore = line.substringBefore("(revision")

                        val startLine = substringBefore
                            .substring("--- a/".length).trim()
                        var endIndex = nextLine.indexOf("(date")
                        if (endIndex == -1) {
                            endIndex = nextLine.indexOf("(revision")
                        }
                        if (endIndex == -1) {
                            endIndex = nextLine.length
                        }

                        val withoutEnd = nextLine.substring("+++ b/".length, endIndex).trim()

                        if (startLine == withoutEnd) {
                            index += 2
                            destination.add("modify file $startLine")
                            continue
                        }
                    }

                    // remove revision number with regex
                    val result = revisionRegex.replace(line, "").trim()
                    if (result.isNotEmpty()) {
                        destination.add(result)
                    }
                } else {
                    if (line.trim().isNotEmpty()) {
                        destination.add(line)
                    }
                }

                index++
            }

            return destination.joinToString("\n")
        }

        private fun isBinaryOrTooLarge(@NotNull change: Change): Boolean {
            return isBinaryOrTooLarge(change.beforeRevision) || isBinaryOrTooLarge(change.afterRevision)
        }

        private fun isBinaryOrTooLarge(revision: ContentRevision?): Boolean {
            val virtualFile = (revision as? CurrentContentRevision)?.virtualFile ?: return false
            return isBinaryRevision(revision) || FileUtilRt.isTooLarge(virtualFile.length)
        }

        private fun isBinaryRevision(cr: ContentRevision?): Boolean {
            if (cr == null) return false

            return when (cr) {
                is BinaryContentRevision -> true
                else -> cr.file.fileType.isBinary
            }
        }
    }
}