package cc.unitmesh.diagram.graphviz.editor

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Common interface for diagram preview file editors
 */
interface DiagramPreviewFileEditor {
    fun getProject(): Project
    fun getFile(): VirtualFile
}
