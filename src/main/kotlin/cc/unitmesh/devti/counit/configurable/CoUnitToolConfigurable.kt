package cc.unitmesh.devti.counit.configurable

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.settings.helper.ToolchainPathChoosingComboBox
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.*
import javax.swing.JTextField

class CoUnitToolConfigurable(project: Project) : BoundConfigurable(AutoDevBundle.message("counit.name")), Disposable {
    private val pathToToolchainComboBox = ToolchainPathChoosingComboBox()
    private val serverAddress = JTextField()

    val settings = project.service<CoUnitProjectSettingsService>()
    val state = settings.state.copy()

    override fun createPanel(): DialogPanel = panel {
        row {
            checkBox(AutoDevBundle.message("counit.enable.label"))
                .comment(AutoDevBundle.message("counit.enable.label.comment"))
                .bindSelected(state::enableCoUnit)
        }

        row(AutoDevBundle.message("counit.server.address.label")) {
            // TODO: spike better way for support 213 and 221
            fullWidthCell(serverAddress)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::serverAddress.toMutableProperty()
                )
        }

        row(AutoDevBundle.message("counit.location.label")) {
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
