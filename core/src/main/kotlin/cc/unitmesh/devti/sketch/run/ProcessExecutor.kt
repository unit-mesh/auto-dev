/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.unitmesh.devti.sketch.run

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.text.Strings
import kotlinx.coroutines.*
import org.jetbrains.ide.PooledThreadExecutor
import java.io.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

data class ProcessExecutorResult(
    val exitCode: Int,
    val stdOutput: String,
    val errOutput: String
)

@Service(Service.Level.PROJECT)
class ProcessExecutor(val project: Project) {
    fun executeCode(code: String): ProcessExecutorResult {
        val taskExecutor = PooledThreadExecutor.INSTANCE
        val future: CompletableFuture<ProcessExecutorResult> = CompletableFuture()
        val task = object : Task.Backgroundable(project, "Running shell command") {
            override fun run(indicator: ProgressIndicator) {
                runBlocking(taskExecutor.asCoroutineDispatcher()) {
                    val executor = ProcessExecutor(project)
                    val result = executor.executeCode(code, taskExecutor.asCoroutineDispatcher())
                    future.complete(result)
                }
            }
        }

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

        return future.get(120, TimeUnit.SECONDS)
    }

    suspend fun executeCode(code: String, dispatcher: CoroutineDispatcher): ProcessExecutorResult {
        val outputWriter = StringWriter()
        val errWriter = StringWriter()

        outputWriter.use {
            val exitCode = exec(code, outputWriter, errWriter, dispatcher)
            val stdOutput = outputWriter.toString()
            val errOutput = errWriter.toString()

            if (exitCode == 0) {
                AutoDevNotifications.notify(project, "Shell command $code executed successfully")
            } else {
                AutoDevToolWindowFactory.Companion.sendToSketchToolWindow(project, ChatActionType.SKETCH) { ui, _ ->
                    ui.putText("Error executing shell command: \n```bash\n$errOutput\n```")
                }
            }

            return ProcessExecutorResult(
                exitCode = exitCode,
                stdOutput = stdOutput,
                errOutput = errOutput
            )
        }
    }

    /**
     * Base on SqliteCliClientImpl
     */
    suspend fun exec(
        shellScript: String,
        stdWriter: Writer,
        errWriter: Writer,
        dispatcher: CoroutineDispatcher
    ): Int = withContext(dispatcher) {
        val process = createProcess(shellScript)

        val exitCode = async { process.awaitExit() }
        val errOutput = async { consumeProcessOutput(process.errorStream, errWriter, process, dispatcher) }
        val stdOutput = async { consumeProcessOutput(process.inputStream, stdWriter, process, dispatcher) }

        stdOutput.await()
        errOutput.await()
        exitCode.await()
    }

    /**
     * for share process
     */
    fun createInteractiveProcess(): Process {
        val commandLine = PtyCommandLine()
        commandLine.withConsoleMode(false)
        commandLine.withUnixOpenTtyToPreserveOutputAfterTermination(true)
        commandLine.withInitialColumns(240)
        commandLine.withInitialRows(80)
        commandLine.withEnvironment("TERM", "dumb")
        commandLine.withEnvironment("BASH_SILENCE_DEPRECATION_WARNING", "1")
        commandLine.withEnvironment("GIT_PAGER", "cat")
        val commands: List<String> = listOf("bash", "--noprofile", "--norc", "-i")
        return commandLine.startProcessWithPty(commands)
    }

    private fun createProcess(shellScript: String): Process {
        val basedir = project.basePath
        val commandLine = PtyCommandLine()
        commandLine.withConsoleMode(false)
        commandLine.withUnixOpenTtyToPreserveOutputAfterTermination(true)
        commandLine.withInitialColumns(240)
        commandLine.withInitialRows(80)
        commandLine.withEnvironment("TERM", "dumb")
        commandLine.withEnvironment("BASH_SILENCE_DEPRECATION_WARNING", "1")
        commandLine.withEnvironment("GIT_PAGER", "cat")
        try {
            getJdkVersion(project)?.let { javaHomePath ->
                commandLine.withEnvironment("JAVA_HOME", javaHomePath)
            }
        } catch (e: Exception) {
            AutoDevNotifications.notify(project, "Failed to get JAVA_HOME: ${e.message}")
        }

        val shell = ShellUtil.detectShells().firstOrNull() ?: "bash"
        val commands: List<String> = listOf(shell, "--noprofile", "--norc", "-c", formatCommand(shellScript))

        if (basedir != null) {
            commandLine.withWorkDirectory(basedir)
        }

        return commandLine.startProcessWithPty(commands)
    }

    private fun formatCommand(command: String): String {
        //         return "{ $command; EXIT_CODE=$?; } 2>&1 && echo \"EXIT_CODE: ${'$'}EXIT_CODE\""
        return "{ $command; } 2>&1"
    }

    private fun feedProcessInput(outputStream: OutputStream, inputLines: List<String>) {
        outputStream.writer(Charsets.UTF_8).use { writer ->
            inputLines.forEach { line ->
                writer.write(line + System.lineSeparator())
                writer.flush()
            }
        }
    }

    private suspend fun consumeProcessOutput(
        source: InputStream?,
        outputWriter: Writer,
        process: Process,
        dispatcher: CoroutineDispatcher
    ) = withContext(dispatcher) {
        if (source == null) return@withContext

        var isFirstLine = true
        BufferedReader(InputStreamReader(source, Charsets.UTF_8.name())).use { reader ->
            do {
                val line = reader.readLine()
                if (Strings.isNotEmpty(line)) {
                    if (!isFirstLine) outputWriter.append(System.lineSeparator())
                    isFirstLine = false
                    outputWriter.append(line)
                } else {
                    yield()
                }
                ensureActive()
            } while (process.isAlive || line != null)
        }
    }

    companion object {
        fun getJdkVersion(project: Project): String? {
            val projectSdk = ProjectRootManager.getInstance(project).projectSdk
            if (projectSdk != null && projectSdk.sdkType.name == "JavaSDK") {
                return projectSdk.homePath
            }

            val projectJdkTable = ProjectJdkTable.getInstance()
            if (projectJdkTable.allJdks.isNotEmpty()) {
                for (jdk in projectJdkTable.allJdks) {
                    if (jdk.sdkType.name == "JavaSDK") {
                        return jdk.homePath
                    }
                }
            }

            val javaHome = System.getenv("JAVA_HOME")
            if (javaHome != null && javaHome.isNotEmpty()) {
                return javaHome
            }

            val javaHomeSdk: Sdk? = try {
                ExternalSystemJdkUtil.resolveJdkName(null, "#JAVA_HOME")
            } catch (e: Exception) {
                null
            }

            if (javaHomeSdk != null) {
                return javaHomeSdk.homePath
            }

            return null
        }
    }
}
