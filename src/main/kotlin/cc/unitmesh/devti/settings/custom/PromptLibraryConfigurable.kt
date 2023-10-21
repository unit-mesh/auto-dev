package cc.unitmesh.devti.settings.custom

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JComponent
import javax.swing.JTextField

class PromptLibraryConfigurable(project: Project) : BoundConfigurable(AutoDevBundle.message("settings.external.prompt.library.name")) {

    private val promptLibraryTextField = JTextField()

    val settings = project.service<PromptLibraryProjectSettingsService>()
    val state = settings.state.copy()

    override fun createPanel(): DialogPanel = panel {
        row(AutoDevBundle.message("settings.external.prompt.library.path")) {
            fullWidthCell(promptLibraryTextField)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::libraryDirectory.toMutableProperty()
                )
        }

        onApply {
            settings.modify {
                it.libraryDirectory = state.libraryDirectory
            }
        }
    }
}

fun <T : JComponent> Row.fullWidthCell(component: T): Cell<T> {
    return cell(component)
        .horizontalAlign(HorizontalAlign.FILL)
}
