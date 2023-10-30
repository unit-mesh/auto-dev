package cc.unitmesh.devti.settings.configurable

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.settings.helper.ToolchainPathChoosingComboBox
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

class CoUnitToolConfigurable(project: Project) :
    BoundConfigurable(AutoDevBundle.message("settings.external.counit.name")), Disposable {

    private val pathToToolchainComboBox = ToolchainPathChoosingComboBox()
    private val serverAddress = JTextField()

    val settings = project.service<CoUnitProjectSettingsService>()
    val state = settings.state.copy()

    override fun createPanel(): DialogPanel = panel {
        row {
            checkBox(AutoDevBundle.message("settings.external.counit.enable.label"))
                .comment(AutoDevBundle.message("settings.external.counit.enable.label.comment"))
                .bindSelected(state::enableCoUnit)
        }

        row(AutoDevBundle.message("settings.external.counit.server.address.label")) {
            // TODO: spike better way for support 213 and 221
            fullWidthCell(serverAddress)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::serverAddress.toMutableProperty()
                )
        }

        row(AutoDevBundle.message("settings.external.counit.location.label")) {
            fullWidthCell(pathToToolchainComboBox)
        }

        onApply {
            settings.modify {
                it.enableCoUnit = state.enableCoUnit
                it.serverAddress = state.serverAddress
            }
        }
    }

    override fun dispose() {
        Disposer.dispose(pathToToolchainComboBox)
    }
}

fun <T : JComponent> Row.fullWidthCell(component: T): Cell<T> {
    return cell(component)
        .horizontalAlign(HorizontalAlign.FILL)
}
