package cc.unitmesh.devti.language.processor

import cc.unitmesh.devti.language.actions.DevInsRunFileAction
import cc.unitmesh.devti.language.ast.FunctionStatementProcessor
import cc.unitmesh.devti.language.ast.action.PatternActionFuncDef
import cc.unitmesh.devti.language.ast.action.PatternProcessor
import cc.unitmesh.devti.language.startup.ShireActionStartupActivity
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

object BatchProcessor : PatternProcessor {
    override val type: PatternActionFuncDef = PatternActionFuncDef.BATCH
    fun execute(
        myProject: Project,
        filename: String,
        inputs: List<String>,
        batchSize: Int,
        variableTable: MutableMap<String, Any?>,
    ): Any {
        val file = runReadAction {
            ShireActionStartupActivity.findShireFile(myProject, filename)
        }

        if (file == null) {
            logger<FunctionStatementProcessor>().error("execute shire error: file not found")
            return ""
        }

        var files = inputs
        /// maybe inputs ["a.txt\nb.txt", "c.txt\nd.txt"] or ["a.txt", "b.txt", "c.txt", "d.txt"] we need to split it
        if (inputs.size == 1) {
            files = inputs[0].split("\n")
        }


        return files.forEach { chunk: String ->
            try {
                val variableNames = arrayOf("input")
                variableTable["input"] = chunk
                DevInsRunFileAction.suspendExecuteFile(myProject, file, variableNames, variableTable) ?: ""
            } catch (e: Exception) {
                logger<FunctionStatementProcessor>().error("execute shire error: $e")
            }
        }
    }
}
