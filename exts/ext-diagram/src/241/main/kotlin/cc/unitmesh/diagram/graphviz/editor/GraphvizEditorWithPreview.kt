package cc.unitmesh.diagram.graphviz.editor

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview

/**
 * Editor with preview for Graphviz DOT files
 * Similar to JdlEditorWithPreview in JHipster UML implementation
 */
class GraphvizEditorWithPreview(
    editor: TextEditor,
    preview: GraphvizPreviewFileEditor
) : TextEditorWithPreview(
    editor,
    preview,
    "Graphviz Preview",
    Layout.SHOW_EDITOR_AND_PREVIEW,
    false
) {
    
    override fun createViewActionGroup(): ActionGroup {
        return DefaultActionGroup(
            showEditorAction,
            showEditorAndPreviewAction,
            showPreviewAction
        )
    }
}
