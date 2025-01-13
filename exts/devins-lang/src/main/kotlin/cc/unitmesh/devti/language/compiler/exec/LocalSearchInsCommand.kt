package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.utils.canBeAdded
import com.intellij.find.FindManager
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.util.CommonProcessors


/**
 * Todo: Spike different search API in Intellij
 * - [com.intellij.util.indexing.FileBasedIndex]
 * - [com.intellij.find.FindManager] or [com.intellij.find.impl.FindInProjectUtil]
 * - [com.intellij.psi.search.PsiSearchHelper]
 * - [com.intellij.structuralsearch.StructuralSearchUtil] (Structural search API)
 * - [com.intellij.find.EditorSearchSession]
 *
 * ```java
 * EditorSearchSession.start(editor,project).setTextInField("Your Text to search");
 * ```
 *
 */
class LocalSearchInsCommand(val myProject: Project, private val scope: String, val text: String?) : InsCommand {
    private val searchScope = GlobalSearchScope.projectScope(myProject)

    override suspend fun execute(): String {
        var text = (text ?: scope).trim()
        /// check text length if less then 3 return alert slowly
        if (text.length < 3) {
            throw IllegalArgumentException("Text length should be more than 5")
        }

        val textSearch = search(myProject, text)
        return textSearch.map { (file, lines) ->
            val filePath = file.path
            val linesWithContext = lines.joinToString("\n")
            "$filePath\n$linesWithContext"
        }.joinToString("\n")
    }

    /**
     * Search in file get before 5 and 5 after text lines
     */
    fun search(project: Project, keyword: String): Map<VirtualFile, List<String>> {
        val result = mutableMapOf<VirtualFile, List<String>>()

        ProjectFileIndex.getInstance(project).iterateContent { file ->
            if (!file.canBeAdded() || !ProjectFileIndex.getInstance(project)
                    .isInContent(file)
            ) return@iterateContent true

            val content = file.contentsToByteArray().toString(Charsets.UTF_8).lines()
            val matchedIndices = content.withIndex()
                .filter { (_, line) -> line.contains(keyword) }
                .map { it.index }

            if (matchedIndices.isNotEmpty()) {
                val linesWithContext = matchedIndices.flatMap { index ->
                    val start = (index - 5).coerceAtLeast(0)
                    val end = (index + 5).coerceAtMost(content.size - 1)
                    content.subList(start, end + 1)
                }.distinct()

                result[file] = linesWithContext
            }
            true
        }
        return result
    }


    /**
     * Provides low-level search and find usages services for a project, like finding references
     * to an element, finding overriding / inheriting elements, finding to do items and so on.
     */
    fun textSearch(project: Project, language: Language, key: String): List<PsiFile> {
        val searchHelper = PsiSearchHelper.getInstance(project)

        val files: Set<PsiFile> = HashSet()
        val psiFileProcessor = CommonProcessors.CollectProcessor(files)

        searchHelper.processAllFilesWithWord(key, searchScope, psiFileProcessor, true)
        println("processAllFilesWithWord: $files")
        searchHelper.processAllFilesWithWordInText(key, searchScope, psiFileProcessor, true)
        println("processAllFilesWithWordInText: $files")
        searchHelper.processAllFilesWithWordInLiterals(key, searchScope, psiFileProcessor)
        println("processAllFilesWithWordInLiterals: $files")

        return files.toList()
    }

    /**
     * [FindManager] Allows to invoke and control Find, Replace and Find Usages operations in files,
     * Get the 5 lines before keyword lines and 5 lines after keyword lines
     * And merge all string if had intersect
     */
    fun searchInFile(project: Project?, keyword: String?, virtualFile: VirtualFile): String {
        val findModel = FindManager.getInstance(project).findInFileModel
        findModel.stringToFind = keyword!!
        findModel.isCaseSensitive = false
        findModel.isWholeWordsOnly = false

        val findManager = FindManager.getInstance(project)
        val findResult = findManager.findString(keyword, 0, findModel, virtualFile)

        return findResult.toString()
    }

    /**
     * [com.jetbrains.python.psi.search.PySearchUtilBase] {@link PySearchUtilBase#excludeSdkTestsScope}
     */
    fun scope() {

    }
}