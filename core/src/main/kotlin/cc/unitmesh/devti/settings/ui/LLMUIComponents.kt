package cc.unitmesh.devti.settings.ui

import cc.unitmesh.devti.settings.LLMParam
import cc.unitmesh.devti.settings.ReactiveComboBox
import cc.unitmesh.devti.settings.ReactiveTextField
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.i18nLabel
import com.intellij.util.ui.FormBuilder

/**
 * Extension function to add LLMParam to FormBuilder
 */
private fun LLMParam.addToFormBuilder(formBuilder: FormBuilder) {
    when (this.type) {
        LLMParam.ParamType.Text -> {
            formBuilder.addLabeledComponent(i18nLabel(this.label), ReactiveTextField(this) {
                this.isEnabled = it.isEditable
            }, 1, false)
        }
        LLMParam.ParamType.ComboBox -> {
            formBuilder.addLabeledComponent(i18nLabel(this.label), ReactiveComboBox(this), 1, false)
        }
        else -> {
            formBuilder.addSeparator()
        }
    }
}
