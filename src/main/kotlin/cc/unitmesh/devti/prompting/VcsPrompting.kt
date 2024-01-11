// MIT License
//
//Copyright (c) Jakob Maležič
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

package cc.unitmesh.devti.prompting

import com.intellij.openapi.components.Service
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.*
import com.intellij.project.stateStore
import com.intellij.vcs.log.VcsFullCommitDetails
import git4idea.repo.GitRepositoryManager
import org.jetbrains.annotations.NotNull
import java.io.IOException
import java.io.StringWriter
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.stream.Collectors
import kotlin.math.min

@Service(Service.Level.PROJECT)
class VcsPrompting(private val project: Project) {
    private val gitRepositoryManager = GitRepositoryManager.getInstance(project)

    fun calculateDiff(changes: List<Change>, project: Project, ignoreFilePatterns: List<PathMatcher>): String {
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
            return trimDiff(diffString)
        } catch (e: VcsException) {
            throw RuntimeException("Error calculating diff: ${e.message}", e)
        }
    }

    fun prepareContext(): String {
        val changeListManager = ChangeListManagerImpl.getInstance(project)
        val changes = changeListManager.changeLists.flatMap {
            it.changes
        }

        return this.calculateDiff(changes, project, listOf())
    }

    /**
     * Builds a diff prompt for a list of VcsFullCommitDetails.
     *
     * @param details The list of VcsFullCommitDetails containing commit details.
     * @param project The Project object representing the current project.
     * @param ignoreFilePatterns The list of PathMatcher objects representing file patterns to be ignored during diff generation. Default value is an empty list.
     * @return A Pair object containing a list of commit message summaries and the generated diff prompt as a string. Returns null if the list is empty or no valid changes are found.
     * @throws VcsException If an error occurs during VCS operations.
     * @throws IOException If an I/O error occurs.
     */
    @Throws(VcsException::class, IOException::class)
    fun buildDiffPrompt(
        details: List<VcsFullCommitDetails>,
        selectList: List<Change>,
        project: Project,
        ignoreFilePatterns: List<PathMatcher> = listOf(),
    ): String? {

        val writer = StringWriter()
        if (details.isNotEmpty()) {
            writer.write("Commit Message: ")
            details.forEach { writer.write(it.fullMessage + "\n\n") }
        }

        writer.write("Changes:\n\n")
        val changeText = calculateDiff(selectList, project, ignoreFilePatterns)

        if (changeText.isEmpty()) {
            return null
        }

        writer.write("```patch\n\n")
        writer.write(trimDiff(changeText))
        writer.write("\n\n```\n\n")

        return writer.toString()
    }

    private val revisionRegex = Regex("\\(revision [^)]+\\)")
    private val lineTip = "\\ No newline at end of file"

    @NotNull
    fun trimDiff(@NotNull diffString: String): String {
        val lines = diffString.lines()
        val destination = ArrayList<String>()
        lines.forEach { line ->
            if (line.startsWith("diff --git ") || line.startsWith("index ") || line.startsWith("Index ")) return@forEach

            if (line == "===================================================================") return@forEach

            // if a patch includes `\ No newline at the end of file` remove it
            if (line.contains(lineTip)) {
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

    fun hasChanges(): List<Change> {
        val changeListManager = ChangeListManagerImpl.getInstance(project)
        val changes = changeListManager.changeLists.flatMap {
            it.changes
        }

        return changes
    }
}

fun isBinaryRevision(cr: ContentRevision?): Boolean {
    if (cr == null) return false
    return if (cr is BinaryContentRevision) true else cr.file.fileType.isBinary
}