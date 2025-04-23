package cc.unitmesh.devti.language.debugger

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableUi
import com.intellij.openapi.options.SimpleConfigurable
import com.intellij.openapi.util.Getter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.xdebugger.settings.DebuggerSettingsCategory
import com.intellij.xdebugger.settings.XDebuggerSettings
import javax.swing.JComponent

class ShireDebugSettings : XDebuggerSettings<ShireDebugSettings>("shire"), Getter<ShireDebugSettings> {
    override fun get(): ShireDebugSettings = this
    override fun getState()= this
    override fun loadState(state: ShireDebugSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    var breakOnPanic: Boolean = true
    override fun createConfigurables(category: DebuggerSettingsCategory): MutableCollection<out Configurable> {
        val config = SimpleConfigurable.create(
            "ShireDebugSettings",
            "Shire Debugger",
            ShireDebugSettingsConfigurableUi::class.java,
            this
        )
        return mutableListOf(config)
    }

    companion object {
        @JvmStatic
        fun getInstance(): ShireDebugSettings = getInstance(ShireDebugSettings::class.java)
    }
}

class ShireDebugSettingsConfigurableUi : ConfigurableUi<ShireDebugSettings>, Disposable {
    private val components: List<ShireDebuggerUiComponent> = run {
        val components = mutableListOf<ShireDebuggerUiComponent>()
        components.add(RsBreakOnPanicConfigurableUi())
        components
    }

    override fun isModified(settings: ShireDebugSettings): Boolean = components.any { it.isModified(settings) }

    override fun reset(settings: ShireDebugSettings) {
        components.forEach { it.reset(settings) }
    }

    override fun apply(settings: ShireDebugSettings) {
        components.forEach { it.apply(settings) }
    }

    override fun getComponent(): JComponent {
        return panel {
            for (component in components) {
                component.buildUi(this)
            }
        }
    }

    override fun dispose() {
        components.forEach { it.dispose() }
    }
}

abstract class ShireDebuggerUiComponent: ConfigurableUi<ShireDebugSettings>, Disposable {
    abstract fun buildUi(panel: Panel)

    override fun getComponent(): JComponent {
        return panel {
            buildUi(this)
        }
    }

    override fun dispose() {}
}

class RsBreakOnPanicConfigurableUi : ShireDebuggerUiComponent() {
    private val breakOnPanicCheckBox: JBCheckBox = JBCheckBox("Debug", ShireDebugSettings.getInstance().breakOnPanic)

    override fun reset(settings: ShireDebugSettings) {
        breakOnPanicCheckBox.isSelected = settings.breakOnPanic
    }

    override fun isModified(settings: ShireDebugSettings): Boolean
            = settings.breakOnPanic != breakOnPanicCheckBox.isSelected

    override fun apply(settings: ShireDebugSettings) {
        settings.breakOnPanic = breakOnPanicCheckBox.isSelected
    }

    override fun buildUi(panel: Panel) {
        with(panel) {
            row { cell(breakOnPanicCheckBox) }
        }
    }
}
