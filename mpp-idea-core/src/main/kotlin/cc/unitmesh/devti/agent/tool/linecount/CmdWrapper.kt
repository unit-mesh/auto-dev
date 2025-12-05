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
            throw IOException("Command timed out")
        }

        if (handler.exitCode != 0) {
            throw IOException("Command failed with exit code ${handler.exitCode}: $stderr")
        }

        return parseResult(stdout.toString())
    }

    @Throws(IOException::class)
    fun findBinary(name: String): Path? {
        val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
        val binName = if (osName.contains("win")) "$name.exe" else name

        // 定义要搜索的路径列表（按优先级排序）
        val searchPaths = mutableListOf<String>()
        
        when {
            osName.contains("mac") -> {
                // macOS 路径按优先级排序
                searchPaths.addAll(listOf(
                    "/opt/homebrew/bin",          // Apple Silicon Homebrew
                    "/usr/local/bin",             // Intel Homebrew 或其他本地安装
                    "/usr/bin",                   // 系统自带工具
                    "/bin",                       // 核心系统工具
                    "/Library/Developer/CommandLineTools/usr/bin", // Xcode Command Line Tools
                    "/opt/local/bin",             // MacPorts
                    "/sw/bin"                     // Fink
                ))
            }
            osName.contains("linux") -> {
                // Linux 路径
                searchPaths.addAll(listOf(
                    "/usr/local/bin",             // 本地安装
                    "/usr/bin",                   // 系统工具
                    "/bin",                       // 核心系统工具
                    "/snap/bin",                  // Snap packages
                    "/usr/local/sbin",            // 本地系统管理工具
                    "/usr/sbin",                  // 系统管理工具
                    "/sbin"                       // 核心系统管理工具
                ))
            }
            osName.contains("win") -> {
                // Windows 路径（如果需要的话）
                searchPaths.addAll(listOf(
                    "C:\\Program Files\\Git\\bin",
                    "C:\\msys64\\usr\\bin",
                    "C:\\cygwin64\\bin"
                ))
            }
        }

        // 首先尝试使用 which/where 命令在 PATH 中查找
        val whichCommand = if (osName.contains("win")) "where" else "which"
        try {
            val pb = ProcessBuilder(whichCommand, binName)
            val process = pb.start()
            if (process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0) {
                val path = String(process.inputStream.readAllBytes(), StandardCharsets.UTF_8).trim()
                if (path.isNotEmpty()) {
                    val pathObj = Paths.get(path)
                    if (pathObj.toFile().exists() && pathObj.toFile().canExecute()) {
                        return pathObj
                    }
                }
            }
        } catch (e: Exception) {
            // 如果 which/where 命令失败，继续使用路径搜索
        }

        // 如果 which/where 失败，尝试在预定义路径中查找
        for (searchPath in searchPaths) {
            val fullPath = Paths.get(searchPath, binName)
            if (fullPath.toFile().exists() && fullPath.toFile().canExecute()) {
                return fullPath
            }
        }

        // 最后尝试在当前工作目录查找
        val currentDirPath = Paths.get(".", binName)
        if (currentDirPath.toFile().exists() && currentDirPath.toFile().canExecute()) {
            return currentDirPath.toAbsolutePath()
        }

        return null
    }
}