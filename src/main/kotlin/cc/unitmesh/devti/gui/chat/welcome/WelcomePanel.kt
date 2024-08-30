package cc.unitmesh.devti.gui.chat.welcome

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.settings.LanguageChangedCallback.componentStateChanged
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
import javax.swing.JPanel

class WelcomePanel: JPanel(BorderLayout()) {
    private val welcomeItems: List<WelcomeItem> = listOf(
        WelcomeItem("settings.welcome.feature.context"),
        WelcomeItem("settings.welcome.feature.lifecycle"),
        WelcomeItem("settings.welcome.feature.custom.action"),
        WelcomeItem("settings.welcome.feature.custom.agent"),
    )

    init {
        val panel = panel {
            row {
                text("").apply {
                    componentStateChanged("settings.welcome.message", this.component) { c, d -> c.text = d }
                }
            }
            welcomeItems.forEach {
                row {
                    // icon
                    icon(AutoDevIcons.AI_COPILOT).gap(RightGap.SMALL)
                    text(it.text).apply {
                        componentStateChanged(it.text, this.component) { c, d -> c.text = d }
                    }
                }
            }
            row {
                text("").apply {
                    componentStateChanged("settings.welcome.feature.features", this.component) { c, d -> c.text = d }
                }
            }
        }.apply {
            border = javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20)
        }

        add(panel, BorderLayout.CENTER)
    }
}