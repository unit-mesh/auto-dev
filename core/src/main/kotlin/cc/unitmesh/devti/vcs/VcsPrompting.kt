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

package cc.unitmesh.devti.vcs

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.*
import com.intellij.vcs.log.VcsFullCommitDetails
import java.io.IOException
import java.io.StringWriter
import java.nio.file.FileSystems
import java.nio.file.PathMatcher

@Service(Service.Level.PROJECT)
class VcsPrompting(private val project: Project) {
    private val defaultIgnoreFilePatterns: List<PathMatcher> = listOf(
        "**/*.json", "**/*.jsonl", "**/*.txt", "**/*.log", "**/*.tmp", "**/*.temp", "**/*.bak", "**/*.swp", "**/*.svg",
    ).map {
        FileSystems.getDefault().getPathMatcher("glob:$it")
    }

    fun prepareContext(changes: List<Change>, ignoreFilePatterns: List<PathMatcher> = defaultIgnoreFilePatterns): String {
        return project.service<DiffSimplifier>().simplify(changes, ignoreFilePatterns)
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
        ignoreFilePatterns: List<PathMatcher> = defaultIgnoreFilePatterns,
    ): String? {
        val changeText = project.service<DiffSimplifier>().simplify(selectList, ignoreFilePatterns)

        if (changeText.isEmpty()) {
            return null
        }

        val processedText = DiffSimplifier.postProcess(changeText)

        val writer = StringWriter()
        if (details.isNotEmpty()) {
            writer.write("Commit Message: ")
            details.forEach { writer.write(it.fullMessage + "\n\n") }
        }

        writer.write("Changes:\n\n```patch\n$processedText\n```")

        return writer.toString()
    }

    fun getChanges(): List<Change> {
        val changeListManager = ChangeListManager.getInstance(project)
        return changeListManager.changeLists.flatMap { it.changes }
    }
}
