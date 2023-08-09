// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.similar.chunks

import cc.unitmesh.devti.llms.tokenizer.TokenizerImpl
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import java.io.File

class SimilarChunksWithPaths(private var chunkSize: Int = 60, private var maxRelevantFiles: Int = 20) {
    private val tokenizer = TokenizerImpl.INSTANCE

    companion object {
        val INSTANCE: SimilarChunksWithPaths = SimilarChunksWithPaths()

        fun createQuery(element: PsiElement, chunkSize: Int = 60): String? {
            if (element.language.displayName.lowercase() == "markdown") {
                return null
            }

            return runReadAction {
                try {
                    val similarChunksWithPaths = SimilarChunksWithPaths(chunkSize).similarChunksWithPaths(element)
                    if (similarChunksWithPaths.paths?.isEmpty() == true || similarChunksWithPaths.chunks?.isEmpty() == true) {
                        return@runReadAction null
                    }

                    return@runReadAction similarChunksWithPaths.toQuery()
                } catch (e: Exception) {
                    return@runReadAction null
                }
            }
        }
    }

    private fun similarChunksWithPaths(element: PsiElement): SimilarChunkContext {
        val mostRecentFiles = getMostRecentFiles(element)
        val mostRecentFilesRelativePaths = mostRecentFiles.map { INSTANCE.relativePathTo(it, element)!! }
        val chunks = extractChunks(element, mostRecentFiles)
        val jaccardSimilarities = tokenLevelJaccardSimilarity(chunks, element)

        val paths = mutableListOf<String>()
        val chunksList = mutableListOf<String>()

        for ((fileIndex, jaccardList) in jaccardSimilarities.withIndex()) {
            val maxIndex = jaccardList.indexOf(jaccardList.maxOrNull())
            paths.add(mostRecentFilesRelativePaths[fileIndex])
            chunksList.add(chunks[fileIndex][maxIndex])
        }

        val language = element.language
        return SimilarChunkContext(language, paths, chunksList)
    }

    private fun tokenLevelJaccardSimilarity(chunks: List<List<String>>, element: PsiElement): List<List<Double>> {
        val currentFileTokens: List<Int> = tokenize(element.containingFile.text)
        return chunks.map { list ->
            list.map {
                val tokenizedFile: List<Int> = tokenize(it)
                jaccardSimilarity(currentFileTokens, tokenizedFile)
            }
        }
    }

    private fun relativePathTo(relativeFile: VirtualFile, element: PsiElement): String? {
        val fileIndex: ProjectFileIndex = ProjectRootManager.getInstance(element.project).fileIndex
        var contentRoot: VirtualFile? = runReadAction {
            fileIndex.getContentRootForFile(relativeFile)
        }

        if (contentRoot == null) {
            contentRoot = fileIndex.getClassRootForFile(relativeFile)
        }

        return contentRoot?.let { VfsUtilCore.getRelativePath(relativeFile, it, File.separatorChar) }
    }

    private fun tokenize(chunk: String): List<Int> {
        return tokenizer.tokenize(chunk)
    }

    private fun jaccardSimilarity(left: List<Int>, right: List<Int>): Double {
        val intersectionSize: Int = left.intersect(right.toSet()).size
        val unionSize: Int = left.union(right).size
        return intersectionSize.toDouble() / unionSize.toDouble()
    }

    private fun extractChunks(element: PsiElement, mostRecentFiles: List<VirtualFile>): List<List<String>> {
        val psiManager: PsiManager = PsiManager.getInstance(element.project)
        return mostRecentFiles.mapNotNull { file ->
            val psiFile = psiManager.findFile(file)
            psiFile?.text?.split("\n", limit = chunkSize)
                ?.filter {
                    !it.trim().startsWith("import ") && !it.trim().startsWith("package ")
                }
                ?.chunked(chunkSize)?.flatten()
        }
    }

    private fun getMostRecentFiles(element: PsiElement): List<VirtualFile> {
        val fileType: FileType = element.containingFile?.fileType ?: return emptyList()

        val recentFiles: List<VirtualFile> = EditorHistoryManager.getInstance(element.project).fileList.filter { file ->
            file.isValid && file.fileType == fileType && file != element.containingFile.virtualFile
        }

        val start = (recentFiles.size - maxRelevantFiles + 1).coerceAtLeast(0)
        val end = (recentFiles.size - 1).coerceAtLeast(0)
        return recentFiles.subList(start, end)
    }
}
