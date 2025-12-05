package cc.unitmesh.devti.statusbar

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import org.jetbrains.annotations.NonNls

class AutoDevStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    override fun isAvailable(project: Project): Boolean = true

    override fun getId(): @NonNls String = "AutoDev"

    override fun getDisplayName(): @NlsContexts.ConfigurableName String =
        AutoDevBundle.message("autodev.statusbar.name")

    override fun createWidget(project: Project): StatusBarWidget = AutoDevStatusBarWidget(project)

    override fun disposeWidget(widget: StatusBarWidget) = widget.dispose()
}
