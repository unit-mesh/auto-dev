package cc.unitmesh.diagram.graphviz.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreviewProvider

/**
 * Split editor provider for Graphviz DOT files
 * Similar to JdlSplitEditorProvider in JHipster UML implementation
 */
class GraphvizSplitEditorProvider : TextEditorWithPreviewProvider(GraphvizPreviewFileEditorProvider()) {
    
    override fun createSplitEditor(firstEditor: TextEditor, secondEditor: FileEditor): FileEditor {
        return GraphvizEditorWithPreview(firstEditor, secondEditor as GraphvizPreviewFileEditor)
    }
}
