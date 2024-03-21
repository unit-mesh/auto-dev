package cc.unitmesh.devti.language.run.flow

import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class DevInsFlowProcessor(val project: Project) {
    /**
     * continue get last compile result
     */
    fun process(output: String, event: ProcessEvent) {
        if (event.exitCode == 0) {
            // continue
        }
        if (event.exitCode != 0) {
            // stop
        }
    }
}