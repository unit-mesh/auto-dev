package cc.unitmesh.devti.language.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.ide.presentation.VirtualFilePresentation
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.NonNls
import java.io.File

class FileReferenceLanguageProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val project = parameters.position.project
        val basePath = project.guessProjectDir()?.path ?: return
        val recentlyFiles = EditorHistoryManager.getInstance(project).fileList

        recentlyFiles.forEach {
            if (!canBeAdded(it)) return@forEach
            result.addElement(buildElement(it, basePath))
        }

        val projectFileIndex = ProjectFileIndex.getInstance(project)
        projectFileIndex.iterateContent {
            if (!canBeAdded(it)) return@iterateContent true
            result.addElement(buildElement(it, basePath))
            true
        }
    }

    private fun buildElement(
        virtualFile: VirtualFile,
        basePath: @NonNls String
    ): LookupElementBuilder {
        val removePrefix = virtualFile.path.removePrefix(basePath)
        val relativePath: String = removePrefix.removePrefix(File.separator)

        return LookupElementBuilder.create(relativePath)
            .withIcon(VirtualFilePresentation.getIcon(virtualFile))
            .withInsertHandler { context, _ ->
                context.editor.caretModel.moveCaretRelatively(
                    1, 0, false, false, false
                )
            }
    }

    private fun canBeAdded(file: VirtualFile): Boolean {
        if (!file.isValid || file.isDirectory) return false

        if (file.fileType.isBinary || FileUtilRt.isTooLarge(file.length)) return false

        return true
    }
}
