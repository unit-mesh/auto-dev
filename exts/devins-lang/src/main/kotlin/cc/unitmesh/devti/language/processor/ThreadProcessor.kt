package cc.unitmesh.devti.language.processor

import cc.unitmesh.devti.language.actions.DevInsRunFileAction
import cc.unitmesh.devti.language.ast.action.PatternActionFuncDef
import cc.unitmesh.devti.language.ast.action.PatternProcessor
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.processor.shell.ShireShellCommandRunner
import cc.unitmesh.devti.devins.provider.http.HttpHandler
import cc.unitmesh.devti.devins.provider.http.HttpHandlerType
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.service.ConsoleService
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.provider.RunService
import cc.unitmesh.devti.util.readText
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.sh.psi.ShFile
import com.intellij.sh.run.ShRunner


object ThreadProcessor : PatternProcessor {
    override val type: PatternActionFuncDef = PatternActionFuncDef.THREAD

    suspend fun execute(
        myProject: Project, fileName: String, variablesName: Array<String>, variableTable: MutableMap<String, Any?>,
    ): String {
        val file = myProject.lookupFile(fileName) ?: return "File not found: $fileName"
        
        val consoleService = ConsoleService.getInstance(myProject)
        consoleService.print("Executing thread for file: $fileName\n", ConsoleViewContentType.NORMAL_OUTPUT)

        val filename = file.name.lowercase()
        val content = file.readText()

        // if ends with .cURL.sh, try call cURL service
        if (filename.endsWith(".curl.sh")) {
            val execute = HttpHandler.provide(HttpHandlerType.CURL)
                ?.execute(myProject, content, variablesName, variableTable)

            if (execute != null) {
                consoleService.print("cURL execution completed\n", ConsoleViewContentType.NORMAL_OUTPUT)
                return execute
            }
        }

        val psiFile = ReadAction.compute<PsiFile, Throwable> {
            PsiManager.getInstance(myProject).findFile(file)
        } ?: return "Failed to find PSI file for $fileName"

        consoleService.print("Running $fileName...\n", ConsoleViewContentType.NORMAL_OUTPUT)
        
        when (psiFile) {
            is DevInFile -> {
                return when (val output = variableTable["output"]) {
                    is List<*> -> {
                        val results = output.mapNotNull {
                            try {
                                variableTable["output"] = it
                                executeTask(myProject, variablesName, variableTable, psiFile)
                            } catch (e: Exception) {
                                consoleService.print("Error: ${e.message}\n", ConsoleViewContentType.ERROR_OUTPUT)
                                null
                            }
                        }

                        results.joinToString("\n")
                    }

                    is Array<*> -> {
                        output.joinToString("\n") {
                            variableTable["output"] = it
                            executeTask(myProject, variablesName, variableTable, psiFile)
                                ?: "$DEVINS_ERROR - Thread: No run service found"
                        }
                    }

                    else -> {
                        return executeTask(myProject, variablesName, variableTable, psiFile)
                            ?: "$DEVINS_ERROR - Thread: No run service found"
                    }
                }
            }

            is ShFile -> {
                val processVariables: Map<String, String> =
                    variablesName.associateWith { (variableTable[it] as? String ?: "") }
                return executeShFile(psiFile, myProject, processVariables)
            }

            else -> {
                val fileRunService = RunService.provider(myProject, file)
                    ?: return "$DEVINS_ERROR No run service found for $psiFile, $fileName"

                return fileRunService.runFileAsync(myProject, file, psiFile)
                    ?: "$DEVINS_ERROR Run service failure: $fileName"
            }
        }
    }

    suspend fun executeShFile(psiFile: ShFile, myProject: Project, processVariables: Map<String, String>): String {
        val virtualFile = psiFile.virtualFile
        ApplicationManager.getApplication().getService(ShRunner::class.java) ?: return "$DEVINS_ERROR: Shell runner not found"

        return ShireShellCommandRunner.runShellCommand(virtualFile, myProject, processVariables)
    }

    private fun executeTask(
        myProject: Project,
        variables: Array<String>,
        variableTable: MutableMap<String, Any?>,
        psiFile: DevInFile,
    ): String? {
        return DevInsRunFileAction.suspendExecuteFile(myProject, psiFile, variables, variableTable)
    }
}

