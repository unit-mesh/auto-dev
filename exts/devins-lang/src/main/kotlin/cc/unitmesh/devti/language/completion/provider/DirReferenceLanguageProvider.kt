package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.gui.chat.AutoDevFileLookupElement
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ProcessingContext

class DirReferenceLanguageProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val project = parameters.position.project
        val basePath = project.guessProjectDir()?.path ?: return

        ProjectFileIndex.getInstance(project).iterateContent {
            if (it.isDirectory) {
                result.addElement(buildElement(it, basePath, 1.0))
            }

            true
        }
    }

    private fun buildElement(virtualFile: VirtualFile, basePath: String, priority: Double): LookupElement {
        val filepath = virtualFile.path.removePrefix(basePath).removePrefix("/")

        val elementBuilder = LookupElementBuilder.create(filepath)
            .withInsertHandler { context, _ ->
                context.editor.caretModel.moveCaretRelatively(1, 1, false, false, false)
            }

        return AutoDevFileLookupElement.withPriority(elementBuilder, priority, virtualFile)
    }
}

