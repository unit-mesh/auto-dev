package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.gui.chat.AutoDevFileLookupElement
import cc.unitmesh.devti.language.utils.canBeAdded
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.ide.presentation.VirtualFilePresentation
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ProcessingContext

class FileReferenceLanguageProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val project = parameters.position.project
        val basePath = project.guessProjectDir()?.path ?: return

        /**
         * Recent open files
         */
        EditorHistoryManager.getInstance(project).fileList.forEach {
            if (!it.canBeAdded()) return@forEach
            result.addElement(buildElement(it, basePath, 99.0))
        }

        ProjectFileIndex.getInstance(project).iterateContent {
            if (!it.canBeAdded()) return@iterateContent true
            if (!ProjectFileIndex.getInstance(project).isInContent(it)) return@iterateContent true

            /// skip ignore
            if (ProjectFileIndex.getInstance(project).isUnderIgnored(it)) return@iterateContent true

            result.addElement(buildElement(it, basePath, 1.0))
            true
        }
    }

    private fun buildElement(virtualFile: VirtualFile, basePath: String, priority: Double): LookupElement {
        val filepath = virtualFile.path.removePrefix(basePath).removePrefix("/")

        val elementBuilder = LookupElementBuilder.create(filepath)
            .withIcon(VirtualFilePresentation.getIcon(virtualFile))
            .withCaseSensitivity(false)
            .withRenderer(object : LookupElementRenderer<LookupElement>() {
                override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
                    presentation.itemText = virtualFile.name
                    presentation.tailText = filepath
                }
            })
            .withInsertHandler { context, _ ->
                context.editor.caretModel.moveCaretRelatively(1, 1, false, false, false)
            }

        return AutoDevFileLookupElement.withPriority(elementBuilder, priority, virtualFile)
    }
}

