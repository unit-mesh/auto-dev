package cc.unitmesh.devti.language.run.flow

import cc.unitmesh.devti.language.psi.DevInFile
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

    /**
     * 1. We need to call LLM to get the task list
     * 2. According to the input and output to decide the next step
     */
    fun createTasks(): List<DevInFile> {
        TODO()
    }

    /**
     * Generate DevIns Task file by LLM
     */
    fun createTempTaskFile(): DevInFile {
        // TODO
        return DevInFile.fromString(project, "")
    }
}