package cc.unitmesh.devti.settings

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBLabel
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JComponent
import javax.swing.JLabel

/**
 * Add callbacks to components to support resetting the state and called when the associated
 * component selection state changes. Change the component context displayed based
 * on the component values selected in the LLMsettings.
 *
 * For example, when the [language] changes, the labels and tips of the components can be reset
 *
 * @author lk
 */
object LanguageChangedCallback : SelectionChangedCallback {
    var language: String = AutoDevSettingsState.getInstance().language
        set(value) {
            if ((field != value).also { field = value }) {
                call()
            }
        }
        get() = AutoDevSettingsState.getInstance().fetchLocalLanguage(field)

    private val callBacks = ConcurrentHashMap<String, LanguageChangedCallback.() -> Unit>()

    fun jLabel(key: String, index: Int = 0): JLabel {
        return label(key, JLabel(), index)
    }

    fun jBLabel(key: String, index: Int = 0): JLabel {
        return label(key, JBLabel(), index)
    }

    private fun label(key: String, label: JLabel, index: Int): JLabel {
        return label.apply {
            componentStateChanged(key, this, index) { l, d ->
                l.text = d
            }
        }
    }

    fun tips(key: String, jComponent: JComponent, index: Int = 0) {
        componentStateChanged(key, jComponent, index) { c, d ->
            c.toolTipText = d
        }
    }

    fun placeholder(key: String, field: EditorTextField, index: Int = 0) {
        componentStateChanged(key, field, index) { e, d ->
            e.setPlaceholder(d)
            e.repaint()
        }
    }

    fun presentationText(key: String, presentation: Presentation, index: Int = 0) {
        when {
            !callBacks.containsKey("$key$index") -> {
                componentStateChanged(key, presentation, index) { p, d ->
                    p.text = d
                }
            }
        }
    }

    fun <T> componentStateChanged(
        key: String,
        jComponent: T,
        index: Int = 0,
        lang: () -> String = { language },
        change: (T, String) -> Unit
    ) {
        addCallback("$key$index", jComponent) {
            {
                change(this, runCatching {
                    AutoDevBundle.messageWithLanguage(key, lang())
                }.getOrElse { "unknown" })
            }
        }
    }

    private fun <T> addCallback(key: String, component: T, callback: () -> (T.() -> Unit)) {
        callback().apply { component.this() }.let {
            callBacks.put(key, { component.it() })
        }

    }

    override fun call() {
        callBacks.values.forEach { this@LanguageChangedCallback.it() }
    }
}


interface SelectionChangedCallback {
    fun call()
}
