package cc.unitmesh.diagram.diagram

import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.preview.FileEditorPreviewSketch
import cc.unitmesh.diagram.idea.DotFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

/**
 * Language sketch provider for Graphviz DOT files
 * Provides LangSketch support for rendering DOT files in the AutoDev tool window
 */
class CodeTopologySketchProvider : LanguageSketchProvider {
    
    override fun isSupported(lang: String): Boolean {
        val normalizedLang = lang.lowercase()
        return normalizedLang == "dot" || 
               normalizedLang == "graphviz" || 
               normalizedLang == "gv"
    }
    
    override fun create(project: Project, content: String): ExtensionLangSketch {
        val file = LightVirtualFile("graphviz.dot", DotFileType.INSTANCE, content)
        return GraphvizSketch(project, file)
    }
}

/**
 * Graphviz sketch implementation that extends FileEditorPreviewSketch
 * This provides a split editor with text editing and live diagram preview
 */
class GraphvizSketch(
    project: Project, 
    myFile: VirtualFile
) : FileEditorPreviewSketch(project, myFile, "GraphvizSplitEditorProvider") {
    
    override fun getExtensionName(): String = "graphviz"
    
    override fun updateViewText(text: String, complete: Boolean) {
        super.updateViewText(text, complete)
        
        // Update the virtual file content to trigger diagram refresh
        try {
            virtualFile.setBinaryContent(text.toByteArray())
        } catch (e: Exception) {
            // Handle any errors gracefully
            e.printStackTrace()
        }
    }
    
    override fun onDoneStream(allText: String) {
        // Called when streaming is complete
        updateViewText(allText, true)
    }
    
    override fun onComplete(code: String) {
        // Called when the code block is complete
        updateViewText(code, true)
    }
}
