package cc.unitmesh.devins.idea.editor

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import cc.unitmesh.llm.ModelConfig
import org.jetbrains.jewel.bridge.compose
import java.awt.Dimension
import javax.swing.JComponent

/**
 * DialogWrapper for model configuration that uses IntelliJ's native dialog system.
 * This ensures proper z-index handling when used alongside SwingPanel components.
 */
class IdeaModelConfigDialogWrapper(
    private val project: Project?,
    private val currentConfig: ModelConfig,
    private val currentConfigName: String? = null,
    private val onSaveCallback: (configName: String, config: ModelConfig) -> Unit
) : DialogWrapper(project) {

    init {
        title = "Model Configuration"
        init()
        contentPanel.border = JBUI.Borders.empty()
        rootPane.border = JBUI.Borders.empty()
    }

    override fun createSouthPanel(): JComponent? = null

    override fun createCenterPanel(): JComponent {
        val dialogPanel = compose {
            IdeaModelConfigDialogContent(
                currentConfig = currentConfig,
                currentConfigName = currentConfigName,
                onDismiss = { close(CANCEL_EXIT_CODE) },
                onSave = { configName, config ->
                    onSaveCallback(configName, config)
                    close(OK_EXIT_CODE)
                }
            )
        }
        dialogPanel.preferredSize = Dimension(500, 500)
        return dialogPanel
    }

    companion object {
        /**
         * Show the model configuration dialog.
         * @return true if the dialog was closed with OK, false otherwise
         */
        fun show(
            project: Project?,
            currentConfig: ModelConfig,
            currentConfigName: String? = null,
            onSave: (configName: String, config: ModelConfig) -> Unit
        ): Boolean {
            val dialog = IdeaModelConfigDialogWrapper(
                project = project,
                currentConfig = currentConfig,
                currentConfigName = currentConfigName,
                onSaveCallback = onSave
            )
            return dialog.showAndGet()
        }
    }
}

