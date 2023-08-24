package cc.unitmesh.devti.settings.configurable

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JComponent
import javax.swing.JTextField

class CoUnitToolConfigurable(project: Project) :
    BoundConfigurable(AutoDevBundle.message("settings.external.counit.name")), Disposable {
    private val pathToToolchainComboBox = CoUnitToolchainPathChoosingComboBox()
    private val serverAddress = JTextField()
    override fun createPanel(): DialogPanel = panel {
        row {
            checkBox(AutoDevBundle.message("settings.external.counit.enable.label"))
                .comment(AutoDevBundle.message("settings.external.counit.enable.label.comment"))
//                .bindSelected(project::enableCoUnit)
        }

        row(AutoDevBundle.message("settings.external.counit.server.address.label")) {
            fullWidthCell(serverAddress)
//                .bind(
//                    componentGet = { it.text },
//                    componentSet = { component, value -> component.text = value },
//                    prop = state::additionalArguments.toMutableProperty()
//                )
        }

        row(AutoDevBundle.message("settings.external.counit.location.label")) {
            fullWidthCell(pathToToolchainComboBox)
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
