package cc.unitmesh.devti.agent.tool.linecount

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.Key
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import java.util.concurrent.TimeUnit

interface CmdWrapper<T> {
    val timeoutMs: Long get() = 30000

    fun buildCommand(arguments: Array<out String>): GeneralCommandLine

    fun parseResult(json: String): List<T>

    /**
     * 同步执行 scc 命令
     * @param arguments scc 命令行参数
     */
    fun runSync(vararg arguments: String): List<T> {
        val command = buildCommand(arguments)
        val handler: OSProcessHandler = ColoredProcessHandler(command)

        handler.startNotify()
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        handler.addProcessListener(object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                if (outputType == ProcessOutputTypes.STDOUT) {
                    stdout.append(event.text)
                } else if (outputType == ProcessOutputTypes.STDERR) {
                    stderr.append(event.text)
                }
            }
        })

        if (!handler.waitFor(timeoutMs)) {
            throw IOException("SCC command timed out")
        }

        if (handler.exitCode != 0) {
            throw IOException("SCC command failed with exit code ${handler.exitCode}: $stderr")
        }

        return parseResult(stdout.toString())
    }


    @Throws(IOException::class)
    fun findBinary(name: String): Path? {
        val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
        val binName = if (osName.contains("win")) "$name.exe" else name

        // try get from /usr/local/bin/scc if macos
        if (osName.contains("mac")) {
            val path = Paths.get("/usr/local/bin/$name")
            if (path.toFile().exists()) {
                return path
            }
        }

        val pb = ProcessBuilder("which", binName)
        val process = pb.start()
        try {
            if (process.waitFor(1, TimeUnit.SECONDS) && process.exitValue() == 0) {
                val path = String(process.inputStream.readAllBytes(), StandardCharsets.UTF_8).trim { it <= ' ' }
                return Paths.get(path)
            }
        } catch (_: InterruptedException) {
            return null
        }

        return null
    }
}