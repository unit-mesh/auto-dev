package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.utils.canBeAdded
import com.intellij.find.FindManager
import com.intellij.find.FindModel
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
class LocalSearchInsCommand(val myProject: Project, private val text: String) : InsCommand {
    private val searchScope = GlobalSearchScope.projectScope(myProject)

    override suspend fun execute(): String {
        val search = search(myProject, text)
        println("Search result: $search")

        val textSearch = textSearch(myProject, Language.ANY, text)
        println("TextSearch result: $textSearch")

        // get line before and after
        return textSearch.toString()
    }

    /**
     * can be iterateContentUnderDirectory
     */
    fun search(project: Project, keyword: String): List<VirtualFile> {
        val result = ArrayList<VirtualFile>()
        ProjectFileIndex.getInstance(project).iterateContent {
            if (!it.canBeAdded()) return@iterateContent true
            if (!ProjectFileIndex.getInstance(project).isInContent(it)) return@iterateContent true

            // search in file
            val content = it.contentsToByteArray().toString(Charsets.UTF_8)
            if (content.contains(keyword)) {
                result.add(it)
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
     * FindUtil
     * [FindManager] Allows to invoke and control Find, Replace and Find Usages operations in files
     */
    fun searchInFile(project: Project?, keyword: String?, virtualFile: VirtualFile): String {
        val findModel = FindModel()
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