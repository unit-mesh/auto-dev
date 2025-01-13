package cc.unitmesh.devti.language.compiler.exec

import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereManager
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.util.indexing.FileBasedIndex
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Todo: Spike different search API in Intellij
 * - [com.intellij.util.indexing.FileBasedIndex]
 * - [com.intellij.find.FindManager] or [com.intellij.find.impl.FindInProjectUtil]
 * - [com.intellij.psi.search.PsiSearchHelper]
 * - [com.intellij.structuralsearch.StructuralSearchUtil] (Structural search API)
 *
 */
class GrepSearchInsCommand(val myProject: Project, private val text: String) : InsCommand {
    override suspend fun execute(): String {
        // 1. use  Intellij Search everywhere API to serach text

        val searchService = SearchEverywhereManager.getInstance(myProject)
        val searchResult = searchService.isEverywhere

        // 2. use Structural search API
//        Language.findLanguageByID("java")?.let {
//            val profileByLanguage = StructuralSearchUtil.getProfileByLanguage(it)
//
//        }

        return searchResult.toString()
    }

    fun psiSearch(project: Project, keyword: String): List<PsiFile> {
        val searchHelper = PsiSearchHelper.getInstance(project)
        val findFilesWithPlainTextWords = searchHelper.findFilesWithPlainTextWords(
            keyword,
        )

        return findFilesWithPlainTextWords.toList()
    }

    fun textSearch(project: Project, language: Language, keyword: String): List<PsiFile> {
        val matchingFiles: MutableList<PsiFile> = ArrayList()
        val fileType = language.associatedFileType ?: return emptyList()
        val scope = GlobalSearchScope.allScope(project)
        FileTypeIndex.processFiles(
            fileType, { virtualFile: VirtualFile? ->
                val psiFile = PsiManager.getInstance(project).findFile(
                    virtualFile!!
                )
                if (psiFile != null && psiFile.text.contains(keyword)) {
                    matchingFiles.add(psiFile)
                }
                true
            },
            scope
        )

        return matchingFiles
    }

    fun search(project: Project?, keyword: String?): Array<out VirtualFile>? {
        val findModel = FindModel()
        findModel.stringToFind = keyword!!
        findModel.isCaseSensitive = false
        findModel.isWholeWordsOnly = false

        val findManager = FindManager.getInstance(project)
        findManager.findString("keyword", 100, findModel)

        val files = project!!.baseDir.children
        return files
    }
}