package cc.unitmesh.diagram.editor

import cc.unitmesh.diagram.idea.DotFileType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GraphvizFileEditorTest {

    @Test
    fun testEditorTypeId() {
        val provider = GraphvizPreviewFileEditorProvider()
        assertEquals("graphviz-uml-editor", provider.editorTypeId)
    }

    @Test
    fun testFileTypeProperties() {
        val fileType = DotFileType.INSTANCE

        assertEquals("DOT", fileType.name)
        assertEquals("Graphviz DOT file", fileType.description)
        assertEquals("dot", fileType.defaultExtension)
        assertFalse(fileType.isBinary)
        assertFalse(fileType.isReadOnly)
    }
    
    @Test
    fun `should have correct editor type ID`() {
        val provider = GraphvizPreviewFileEditorProvider()
        assertEquals("graphviz-uml-editor", provider.editorTypeId)
    }
    
    @Test
    fun `should have correct file type properties`() {
        val fileType = DotFileType.INSTANCE
        
        assertEquals("DOT", fileType.name)
        assertEquals("Graphviz DOT file", fileType.description)
        assertEquals("dot", fileType.defaultExtension)
        assertFalse(fileType.isBinary)
        assertFalse(fileType.isReadOnly)
    }
}
