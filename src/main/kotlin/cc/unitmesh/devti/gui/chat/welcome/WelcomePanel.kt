package cc.unitmesh.devti.gui.chat.welcome

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import com.intellij.ui.Gray
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
import javax.swing.JPanel

class WelcomePanel: JPanel(BorderLayout()) {
    private val welcomeItems: List<WelcomeItem> = listOf(
        WelcomeItem(AutoDevBundle.message("settings.welcome.feature.context")),
        WelcomeItem(AutoDevBundle.message("settings.welcome.feature.lifecycle")),
        WelcomeItem(AutoDevBundle.message("settings.welcome.feature.custom.action")),
        WelcomeItem(AutoDevBundle.message("settings.welcome.feature.custom.agent")),
    )

    init {
        val panel = panel {
            row {
                text(AutoDevBundle.message("settings.welcome.message"))
            }
            welcomeItems.forEach {
                row {
                    // icon
                    icon(AutoDevIcons.AI_COPILOT).gap(RightGap.SMALL)
                    text(it.text)
                }
            }
            row {
                text(AutoDevBundle.message("settings.welcome.feature.features"))
            }
        }.apply {
            // set margin 20
            border = javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20)
        }

        add(panel, BorderLayout.CENTER)
    }
}