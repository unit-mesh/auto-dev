package cc.unitmesh.diagram.editor.mermaid

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreviewProvider

/**
 * Split editor provider for Mermaid files
 * Similar to GraphvizSplitEditorProvider but for Mermaid diagrams
 */
class MermaidSplitEditorProvider : TextEditorWithPreviewProvider(MermaidPreviewFileEditorProvider()) {
    override fun createSplitEditor(firstEditor: TextEditor, secondEditor: FileEditor): FileEditor {
        return MermaidEditorWithPreview(firstEditor, secondEditor as MermaidPreviewFileEditor)
    }
}
