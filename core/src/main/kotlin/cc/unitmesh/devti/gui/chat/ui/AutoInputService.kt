package cc.unitmesh.devti.gui.chat.ui

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class AutoInputService(val project: Project) {
    private var autoDevInput: AutoDevInput? = null

    fun registerAutoDevInput(input: AutoDevInput) {
        autoDevInput = input
    }

    fun putText(text: String) {
        autoDevInput?.appendText(text)
    }

    fun deregisterAutoDevInput(input: AutoDevInput) {
        autoDevInput = null
    }

    companion object {
        fun getInstance(project: Project): AutoInputService {
            return project.getService(AutoInputService::class.java)
        }
    }
}