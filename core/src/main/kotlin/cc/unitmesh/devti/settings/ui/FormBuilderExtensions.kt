package cc.unitmesh.devti.settings.ui

import cc.unitmesh.devti.settings.LLMParam
import cc.unitmesh.devti.settings.ReactiveComboBox
import cc.unitmesh.devti.settings.ReactiveTextField
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.jBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder

/**
 * Helper extension function for FormBuilder to add LLMParam
 */
fun FormBuilder.addLLMParam(llmParam: LLMParam): FormBuilder = apply {
    when (llmParam.type) {
        LLMParam.ParamType.Text -> {
            addLabeledComponent(jBLabel(llmParam.label), ReactiveTextField(llmParam) {
                this.isEnabled = it.isEditable
            }, 1, false)
        }
        LLMParam.ParamType.ComboBox -> {
            addLabeledComponent(jBLabel(llmParam.label), ReactiveComboBox(llmParam), 1, false)
        }
        else -> {
            addSeparator()
        }
    }
}
