package cc.unitmesh.devti.context.chunks

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
    companion object {
        val INSTANCE: SimilarChunksWithPaths = SimilarChunksWithPaths()

        fun createQuery(element: PsiElement, chunkSize: Int = 60): String? {
            return runReadAction {
                val similarChunksWithPaths = SimilarChunksWithPaths(chunkSize).similarChunksWithPaths(element)
                if (similarChunksWithPaths.paths?.isEmpty() == true || similarChunksWithPaths.chunks?.isEmpty() == true) {
                    return@runReadAction null
                }

                return@runReadAction similarChunksWithPaths.toQuery()
            }
        }
    }

    fun similarChunksWithPaths(element: PsiElement): SimilarChunkContext {
        val mostRecentFiles: List<VirtualFile> = getMostRecentFiles(element)
        val mostRecentFilesRelativePaths: List<String> = mostRecentFiles.mapNotNull {
            INSTANCE.relativePathTo(it, element)
        }

        val chunks: List<List<String>> = extractChunks(element, mostRecentFiles)
        val jaccardSimilarities: List<List<Double>> = tokenLevelJaccardSimilarity(chunks, element)
        val paths: MutableList<String> = ArrayList()
        val chunksList: MutableList<String> = ArrayList()

        jaccardSimilarities.forEachIndexed { index, list ->
            val maxIndex = list.indexOf(list.maxOrNull()!!)
            paths.add(mostRecentFilesRelativePaths[index])
            chunksList.add(chunks[index][maxIndex])
        }

        return SimilarChunkContext(element.language, paths, chunksList)
    }

    private fun tokenLevelJaccardSimilarity(chunks: List<List<String>>, element: PsiElement): List<List<Double>> {
        val currentFileTokens: Set<String> = tokenize(element.containingFile.text).toSet()
        return chunks.map { list ->
            list.map {
                val tokenizedFile: Set<String> = tokenize(it).toSet()
                jaccardSimilarity(currentFileTokens, tokenizedFile)
            }
        }
    }

    private fun relativePathTo(relativeFile: VirtualFile, element: PsiElement): String? {
        val fileIndex: ProjectFileIndex = ProjectRootManager.getInstance(element.project).fileIndex
        var contentRoot: VirtualFile? = fileIndex.getContentRootForFile(relativeFile)
        if (contentRoot == null) {
            contentRoot = fileIndex.getClassRootForFile(relativeFile)
        }

        return contentRoot?.let { VfsUtilCore.getRelativePath(relativeFile, it, File.separatorChar) }
    }

    private fun tokenize(chunk: String): List<String> {
        return chunk.split(Regex("[^a-zA-Z0-9]"))
            .filter { it.isNotBlank() }
    }

    private fun jaccardSimilarity(set1: Set<String>, set2: Set<String>): Double {
        val intersectionSize: Int = set1.intersect(set2).size
        val unionSize: Int = set1.union(set2).size
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
        val fileType: FileType? = element.containingFile?.fileType
        if (element.containingFile == null || fileType == null) {
            return emptyList()
        }
        val recentFiles: List<VirtualFile> = EditorHistoryManager.getInstance(element.project).fileList.filter { file ->
            file.isValid && file.fileType == fileType
        }
        val start = (recentFiles.size - maxRelevantFiles + 1).coerceAtLeast(0)
        val end = (recentFiles.size - 1).coerceAtLeast(0)
        return recentFiles.subList(start, end)
    }
}
