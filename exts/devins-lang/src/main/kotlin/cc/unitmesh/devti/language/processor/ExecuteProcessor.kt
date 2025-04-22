package cc.unitmesh.devti.language.processor

import cc.unitmesh.devti.language.actions.DevInsRunFileAction
import cc.unitmesh.devti.language.ast.action.PatternActionFuncDef
import cc.unitmesh.devti.language.ast.action.PatternProcessor
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.compiler.exec.RunInsCommand
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.provider.RunService
import cc.unitmesh.devti.startup.ShireActionStartupActivity
import cc.unitmesh.devti.util.workerThread
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object ExecuteProcessor : PatternProcessor {
    override val type: PatternActionFuncDef = PatternActionFuncDef.EXECUTE

    private val logger = logger<ExecuteProcessor>()

    fun execute(
        myProject: Project,
        filename: Any,
        variableNames: Array<String>,
        variableTable: MutableMap<String, Any?>,
    ): Any {
        val file = filename.toString()
        if (file.endsWith(".shire")) {
            return executeShireFile(myProject, filename, variableNames, variableTable)
        }

        if (file.startsWith(":")) {
            CoroutineScope(workerThread).launch {
                RunInsCommand(myProject, file).execute()
            }
        }

        val virtualFile = myProject.lookupFile(file) ?: return "$DEVINS_ERROR: File not found: $filename"

        val runService = RunService.provider(myProject, virtualFile)
        return runService?.runFileAsync(myProject, virtualFile, null)
            ?: "$DEVINS_ERROR: [ExecuteProcessor] No run service found for file: $filename"
    }

    private fun executeShireFile(
        myProject: Project,
        filename: Any,
        variableNames: Array<String>,
        variableTable: MutableMap<String, Any?>,
    ): String {
        try {
            val file = runReadAction {
                ShireActionStartupActivity.findShireFile(myProject, filename.toString())
            }

            if (file == null) {
                logger.warn("execute shire error: file not found")
                return ""
            }

            return DevInsRunFileAction.suspendExecuteFile(myProject, file, variableNames, variableTable) ?: ""
        } catch (e: Exception) {
            logger.warn("execute shire error: $e")
            return ""
        }
    }

}
