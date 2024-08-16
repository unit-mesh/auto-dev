package cc.unitmesh.devti.settings

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import java.awt.event.ItemEvent
import javax.swing.event.DocumentEvent
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


/**
 * A simple version of reactive delegate
 *
 * ```kotlin
 * var s by Reactive("hello") {
 *   println("s changed to $it")
 * }
 * s = "world" // println "s changed to world"
 * ```
 */
class Reactive<V>(var value: V, val onChange: (V) -> Unit)

operator fun <V> Reactive<V>.setValue(thisRef: Any?, property: KProperty<*>, value: V) {
    if (this.value == value) return
    this.value = value
    onChange(value)
}

operator fun <V> Reactive<V>.getValue(thisRef: Any?, property: KProperty<*>): V = this.value

fun ReactiveTextField(param: LLMParam, initBlock: JBTextField.(LLMParam) -> Unit = {}): JBTextField {
    val component = JBTextField(param.value)
    val reactive by Reactive(param) {
        component.text = param.value
    }

    component.initBlock(reactive)

    component.document.addDocumentListener(CustomDocumentListener{
        param.value = component.text
    })
    return component
}

fun ReactivePasswordField(param: LLMParam, initBlock: JBPasswordField.(LLMParam) -> Unit = {}): JBPasswordField {
    val component = JBPasswordField().apply {
        text = param.value
    }
    val reactive = Reactive(param) {
        component.text = it.value
    }

    component.initBlock(reactive.value)
    component.document.addDocumentListener(CustomDocumentListener{
        if (component.password.joinToString("") == param.value) return@CustomDocumentListener
        reactive.value.value = component.password.joinToString("")
    })

    return component
}

fun ReactiveComboBox(param: LLMParam, initBlock: ComboBox<String>.(LLMParam) -> Unit = {}): ComboBox<String> {
    val component = ComboBox(param.items.toTypedArray()).apply {
        selectedItem = param.value
    }
    val reactive by Reactive(param) {
        component.selectedItem = it.value
    }

    component.initBlock(reactive)
    component.addItemListener {
        if (it.stateChange == ItemEvent.SELECTED) {
            reactive.value = component.selectedItem as String
        }
    }
    return component
}

class CustomDocumentListener(val onChange: () -> Unit) : DocumentAdapter() {
    override fun textChanged(e: DocumentEvent) {
        onChange()
    }
}


/**
 *
 * A LLMParam is a setting for the LLMSettingsComponent.
 *
 * Adding a LLM Param:
 *
 *  - Step 1. add label to [AutoDevBundle] with key `settings.<yourName>`
 *  - Step 2. define a variable named yourName. in this example, it's `openAIKey`
 *     in the `creating` block, you can use one of the factory functions:
 *     [LLMParam.Editable], [LLMParam.Password], [LLMParam.ComboBox]
 *    ```kotlin
 *       val openAIKey by LLMParam.creating {
 *          Editable(service.getOpenAIKey())
 *       }
 *    ```
 *
 *
 * @param label the label of the setting, will automatically get from bundle resource [AutoDevBundle], named by `settings.${property.name}`
 * @param value the value of the setting, default from bundle resource [AutoDevBundle], named `settings.${property.name}.default`
 * @param type the type of the setting, default is [ParamType.Text], can be [ParamType.Password] or [ParamType.ComboBox]
 * @param isEditable whether the setting is editable, if is not editable, user can't change the value on UI
 * @param items if [type] is [ParamType.ComboBox], this field will be used to set the items of the combo box
 */
class LLMParam(
    value: String = "",
    var label: String = "",
    val isEditable: Boolean = true,
    val type: ParamType = ParamType.Text,
    var items: List<String> = emptyList(),
) {
    enum class ParamType {
        Text, Password, ComboBox, Separator
    }


    private var onChange: (LLMParam.(String) -> Unit)? = null

    var value: String = value
        set(newValue) {
            val changed = field != newValue
            field = newValue
            if (changed) {
                onChange?.invoke(this, newValue)
            }
        }

    companion object {
        private val bundle = AutoDevBundle

        /**
         * @param block a block to create a LLMParam, will be called only once
         *
         * will set [label] and [value] to the value defined in config file [AutoDevBundle] if they are empty
         */
        fun creating(onChange: LLMParam.(String) -> Unit = {}, block: Companion.() -> LLMParam) =
            PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, LLMParam>> { _, _ ->
                object : ReadOnlyProperty<Any?, LLMParam> {
                    private var param: LLMParam? = null
                    override fun getValue(thisRef: Any?, property: KProperty<*>): LLMParam {
                        return param ?: this@Companion.block().apply {
                            if (label.isEmpty()) {
                                label = "settings.${property.name}"
                            }

                            this.onChange = onChange
                            param = this
                        }
                    }
                }
            }

        // factory functions to create LLMParam
        fun Editable(value: String = "") = LLMParam(value = value)
        fun Password(password: String = "") = LLMParam(value = password, type = ParamType.Password)

        fun ComboBox(value: String, items: List<String>) =
            LLMParam(value = value, type = ParamType.ComboBox, items = items.toList())
    }
}

