package cc.unitmesh.mermaid

import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.preview.FileEditorPreviewSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

class MermaidSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean {
        return lang == "mermaid" || lang == "mmd"
    }

    override fun create(project: Project, content: String): ExtensionLangSketch {
        val file = LightVirtualFile("mermaid.mermaid", content)
        return MermaidSketch(project, file)
    }
}

class MermaidSketch(project: Project, myFile: VirtualFile) :
    FileEditorPreviewSketch(project, myFile, "MermaidEditorWithPreviewProvider") {

    override fun getExtensionName(): String = "mermaid"
}
