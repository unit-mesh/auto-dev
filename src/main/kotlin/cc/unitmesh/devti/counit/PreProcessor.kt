package cc.unitmesh.devti.counit

import cc.unitmesh.devti.settings.configurable.coUnitSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class PreProcessor(val project: Project) {
    fun isCoUnit(input: String): Boolean {
        return project.coUnitSettings.enableCoUnit && input.startsWith("/counit")
    }
}

