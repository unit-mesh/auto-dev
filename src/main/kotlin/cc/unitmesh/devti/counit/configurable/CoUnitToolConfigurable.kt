package cc.unitmesh.devti.counit.configurable

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.gui.component.JsonLanguageField
import cc.unitmesh.devti.settings.helper.ToolchainPathChoosingComboBox
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.*
import javax.swing.JTextField

class CoUnitToolConfigurable(val project: Project) : BoundConfigurable(AutoDevBundle.message("counit.name")), Disposable {
    private val pathToToolchainComboBox = ToolchainPathChoosingComboBox()
    private val serverAddress = JTextField()

    val settings = project.service<CoUnitProjectSettingsService>()
    val state = settings.state.copy()

    override fun createPanel(): DialogPanel = panel {
        row {
            checkBox(AutoDevBundle.message("counit.enable.label")).bindSelected(state::enableCustomRag)
        }

        row(AutoDevBundle.message("counit.server.address.label")) {
            fullWidthCell(serverAddress)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::serverAddress.toMutableProperty()
                )
        }

// TODO: JSON RPC
//        row(AutoDevBundle.message("counit.location.label")) {
//            fullWidthCell(pathToToolchainComboBox)
//        }

        row {
            val languageField = JsonLanguageField(
                project,
                state::ragsJsonConfig.toString(),
                AutoDevBundle.message("counit.rags.json.placeholder")
            )
            fullWidthCell(languageField)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::ragsJsonConfig.toMutableProperty()
                )
        }

        onApply {
            settings.modify {
                it.enableCustomRag = state.enableCustomRag
                it.serverAddress = state.serverAddress
                it.ragsJsonConfig = state.ragsJsonConfig
            }
        }
    }

    override fun dispose() {
        Disposer.dispose(pathToToolchainComboBox)
    }
}
