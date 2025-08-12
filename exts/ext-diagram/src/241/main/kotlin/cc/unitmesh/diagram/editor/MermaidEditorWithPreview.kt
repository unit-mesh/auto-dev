package cc.unitmesh.diagram.editor

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview

/**
 * Editor with preview for Mermaid files
 * Similar to GraphvizEditorWithPreview but for Mermaid diagrams
 */
class MermaidEditorWithPreview(
    editor: TextEditor,
    preview: MermaidPreviewFileEditor
) : TextEditorWithPreview(
    editor,
    preview,
    "Mermaid Preview",
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
