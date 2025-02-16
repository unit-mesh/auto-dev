package cc.unitmesh.mermaid

import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.FileEditorSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import javax.swing.JPanel

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
    FileEditorSketch(project, myFile, "MermaidEditorWithPreviewProvider") {

    override fun getExtensionName(): String = "mermaid"
}
