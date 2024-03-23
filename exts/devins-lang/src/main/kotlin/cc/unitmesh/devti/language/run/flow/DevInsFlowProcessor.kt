package cc.unitmesh.devti.language.run.flow

import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInVisitor
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement

@Service(Service.Level.PROJECT)
class DevInsFlowProcessor(val project: Project) {
    /**
     * The flag comment is the comment that starts with `[devins]`
     */
    fun lookupFlagComment(devInFile: DevInFile): List<PsiElement> {
        val comments = mutableListOf<PsiElement>()
        devInFile.accept(object : DevInVisitor() {
            override fun visitComment(comment: PsiComment) {
                comments.add(comment)
            }
        })

        return comments
    }

    /**
     * continue get last compile result
     */
    fun process(output: String, event: ProcessEvent, scriptPath: String) {
        val devInFile: DevInFile? = runReadAction { DevInFile.lookup(project, scriptPath) }
        project.service<DevInsConversationService>().updateIdeOutput(scriptPath, output)
        if (event.exitCode == 0) {
            val lookUpFlagComment = lookupFlagComment(devInFile!!)
            if (lookUpFlagComment.isNotEmpty()) {
                // TODO
            }
        }
        if (event.exitCode != 0) {
            project.service<DevInsConversationService>().tryFixWithLlm(scriptPath)
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