package cc.unitmesh.devti.sketch.ui.code.processor

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.provider.RunService
import com.intellij.icons.AllIcons
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.util.ui.JBUI
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Processor for handling write command operations
 */
class WriteCommandProcessor(private val project: Project) {
    /**
     * Process write command and return UI panel
     */
    fun processWriteCommand(currentText: String, fileName: String?): JPanel {
        val button = createWriteButton(currentText, fileName)
        
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.add(button)
        
        return panel
    }
    
    private fun createWriteButton(currentText: String, fileName: String?): JButton {
        return JButton(AutoDevBundle.message("sketch.write.to.file"), AllIcons.Actions.MenuSaveall).apply {
            preferredSize = JBUI.size(120, 30)
            
            addActionListener {
                executeWriteCommand(currentText, fileName, this)
            }
        }
    }
    
    private fun executeWriteCommand(currentText: String, fileName: String?, button: JButton) {
        val newFileName = "DevIn-${System.currentTimeMillis()}.devin"
        val language = Language.findLanguageByID("DevIn")
        val file = ScratchRootType.getInstance()
            .createScratchFile(project, newFileName, language, currentText)
        
        button.text = "Written to $fileName"
        button.isEnabled = false
        
        if (file == null) return
        
        val psiFile = PsiManager.getInstance(project).findFile(file)!!
        
        RunService.provider(project, file)
            ?.runFile(project, file, psiFile, isFromToolAction = true)
            ?: RunService.runInCli(project, psiFile)
            ?: AutoDevNotifications.notify(project, "No run service found for ${file.name}")
    }
}
