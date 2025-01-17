package cc.unitmesh.devti.sketch

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class AutoSketchMode() {
    var isEnable: Boolean = false

    companion object {
        fun getInstance(project: Project): AutoSketchMode {
            return project.service<AutoSketchMode>()
        }
    }
}