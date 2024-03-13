package cc.unitmesh.devti.gui.component

import com.intellij.json.JsonLanguage
import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.EditorColorsUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.ui.LanguageTextField
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import java.awt.Dimension
import java.awt.FontMetrics

private const val CUSTOM_AGENT_NAME = "AutoDevCustomAgentFile.json"

class JsonLanguageField(private val myProject: Project, val value: String, private val placeholder: String) :
    LanguageTextField(JsonLanguage.INSTANCE, myProject, value,
        object : SimpleDocumentCreator() {
            override fun createDocument(value: String?, language: Language?, project: Project?): Document {
                return createDocument(value, language, project, this)
            }

            override fun customizePsiFile(file: PsiFile?) {
                file?.name = CUSTOM_AGENT_NAME
            }
        }
    ) {

    override fun createEditor(): EditorEx {
        return super.createEditor().apply {
            setShowPlaceholderWhenFocused(true)
            setHorizontalScrollbarVisible(false)
            setVerticalScrollbarVisible(true)
            setPlaceholder(placeholder)

            val scheme = EditorColorsUtil.getColorSchemeForBackground(this.colorsScheme.defaultBackground)
            this.colorsScheme = this.createBoundColorSchemeDelegate(scheme)

            val metrics: FontMetrics = getFontMetrics(font)
            val columnWidth = metrics.charWidth('m')
            isOneLineMode = false
            preferredSize = Dimension(25 * columnWidth, 25 * metrics.height)
        }
    }
}

internal class AutoDevJsonJsonSchemaProviderFactory: JsonSchemaProviderFactory {
    override fun getProviders(project: Project): MutableList<JsonSchemaFileProvider> {
        return mutableListOf(AutoDevJsonSchemaFileProvider(project))
    }
}

class AutoDevJsonSchemaFileProvider(val project: Project): JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean {
        return runReadAction { isAutoDevCustomAgentFile(file) }
    }

    private fun isAutoDevCustomAgentFile(file: VirtualFile): Boolean {
        if (!file.isValid) {
            return false
        }

        return file.name == CUSTOM_AGENT_NAME
    }

    override fun getName(): String = "AutoDevCustomAgentFile"

    override fun getSchemaFile(): VirtualFile? {
        return JsonSchemaProviderFactory.getResourceFile(this::class.java, schemaFileName)
    }

    override fun getSchemaType(): SchemaType {
        return SchemaType.schema
    }

    companion object {
        private const val schemaFileName = "autodev-custom-agent.json"
    }
}
