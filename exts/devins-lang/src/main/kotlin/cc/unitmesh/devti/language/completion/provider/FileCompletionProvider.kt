package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.completion.AutoDevFileLookupElement
import cc.unitmesh.devti.language.utils.canBeAdded
import cc.unitmesh.devti.util.relativePath
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.IconUtil
import com.intellij.util.ProcessingContext

class FileCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val project = parameters.position.project

        var recentlyFiles: MutableList<VirtualFile> = mutableListOf()
        EditorHistoryManager.getInstance(project).fileList.forEach {
            if (!it.canBeAdded()) return@forEach
            val element = buildElement(it, project, 99.0) ?: return@forEach
            result.addElement(element)
            recentlyFiles.add(it)
        }

        ProjectFileIndex.getInstance(project).iterateContent {
            if (!it.canBeAdded()) return@iterateContent true
            if (recentlyFiles.contains(it)) return@iterateContent true
            if (!ProjectFileIndex.getInstance(project).isInContent(it)) return@iterateContent true
            if (ProjectFileIndex.getInstance(project).isUnderIgnored(it)) return@iterateContent true
            val element = buildElement(it, project, 1.0)  ?: return@iterateContent true
            result.addElement(element)
            true
        }
    }

    private fun buildElement(virtualFile: VirtualFile, project: Project, priority: Double): LookupElement? {
        val filepath = virtualFile.relativePath(project) ?: return null
        val elementBuilder = LookupElementBuilder.create(filepath)
            .withCaseSensitivity(false)
            .withRenderer(object : LookupElementRenderer<LookupElement>() {
                override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
                    presentation.itemText = virtualFile.name
                    presentation.tailText = filepath
                    presentation.icon = IconUtil.computeFileIcon(virtualFile, 0, project)
                }
            })
            .withInsertHandler { context, _ ->
                context.editor.caretModel.moveCaretRelatively(1, 1, false, false, false)
            }

        return AutoDevFileLookupElement.withPriority(elementBuilder, priority, virtualFile)
    }
}

