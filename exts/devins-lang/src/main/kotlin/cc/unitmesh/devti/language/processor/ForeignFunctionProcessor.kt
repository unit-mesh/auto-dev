package cc.unitmesh.devti.language.processor

import cc.unitmesh.devti.language.ast.ForeignFunction
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.provider.RunService
import cc.unitmesh.devti.util.findFile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project

object ForeignFunctionProcessor {
    fun execute(
        project: Project, funcName: String, args: List<Any>, allVariables: Map<String, Any?>, func: ForeignFunction,
    ): Any {
        val filename = func.funcPath

        val virtualFile = runReadAction {
            project.findFile(filename)
        } ?: return "$DEVINS_ERROR: File not found: $filename"


        /// last args will be file path, should be skip
        val argList: List<String> = args.dropLast(1).map {
            // handle for arrayList and map type
            when (it) {
                is List<*> -> it.joinToString(",")
                is Map<*, *> -> it.entries.joinToString(",") { (k, v) -> "$k=$v" }
                else -> it.toString()
            }
        }

        return RunService.runInCli(project, virtualFile, argList)
            ?: "$DEVINS_ERROR: [ForeignFunctionProcessor] No run service found for file: $filename"
    }
}
