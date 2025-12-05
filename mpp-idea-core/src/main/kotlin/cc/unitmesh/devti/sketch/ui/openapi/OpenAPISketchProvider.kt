package cc.unitmesh.devti.sketch.ui.openapi

import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.sketch.ui.preview.FileEditorPreviewSketch
import cc.unitmesh.devti.util.parser.CodeFence.Companion.findLanguage
import cc.unitmesh.devti.util.parser.CodeFence.Companion.findLanguageByExt
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.fileEditor.TextEditorWithPreview.Layout
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import javax.swing.JComponent

class OpenAPISketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String) = lang == "yaml" || lang == "yml"

    override fun create(
        project: Project,
        content: String
    ): ExtensionLangSketch {
        val isValidOpenAPI = content.contains("openapi:") && content.contains("info:")
        if (!isValidOpenAPI) {
            val language = findLanguage("yaml")
            return object : CodeHighlightSketch(project, content, language), ExtensionLangSketch {
                override fun getExtensionName(): String = "Yaml"
            }
        }

        val language = findLanguageByExt("yaml")
        val virtualFile = ScratchRootType.getInstance()
            .createScratchFile(project, createFileNameWithTime(), language, content)
            ?: LightVirtualFile(createFileNameWithTime(), content)

        return OpenAPISketch(project, content, virtualFile)
    }
}

class OpenAPISketch(val myProject: Project, private val content: String, virtualFile: VirtualFile) :
    FileEditorPreviewSketch(
        myProject,
        virtualFile,
        "SwaggerUIEditorProvider"
    ) {
    init {
        virtualFile.putUserData(Key.create<Layout>("TextEditorWithPreview.DefaultLayout"), Layout.SHOW_PREVIEW)
    }

    override val mainPanel: JComponent get() = editor.component

    override fun getExtensionName(): String = "OpenAPI"
}

private fun createFileNameWithTime(): String = "openapi-${System.currentTimeMillis()}.yaml"