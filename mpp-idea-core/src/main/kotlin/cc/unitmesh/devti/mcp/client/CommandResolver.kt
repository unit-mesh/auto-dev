package cc.unitmesh.devti.mcp.client

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.SystemInfo
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

fun resolveCommand(command: String): String {
    if (SystemInfo.isWindows) {
        try {
            val pb = ProcessBuilder("where", command)
            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val resolved = reader.readLine() // take first non-null output
            if (!resolved.isNullOrBlank()) return resolved.trim()
        } catch (e: Exception) {
            logger<CustomMcpServerManager>().warn("Failed to resolve command using where: $e")
        }
    } else {
        val homeDir = System.getProperty("user.home")
        if (command == "npx") {
            val knownPaths = listOf(
                "/opt/homebrew/bin/npx",
                "/usr/local/bin/npx",
                "/usr/bin/npx",
                "$homeDir/.volta/bin/npx",
                "$homeDir/.nvm/current/bin/npx",
                "$homeDir/.npm-global/bin/npx"
            )
            knownPaths.forEach { path ->
                if (File(path).exists()) return path
            }
        }
        try {
            val pb = ProcessBuilder("which", command)
            val currentPath = System.getenv("PATH") ?: ""
            val additionalPaths = if (command == "npx") {
                listOf(
                    "/opt/homebrew/bin",
                    "/opt/homebrew/sbin",
                    "/usr/local/bin",
                    "$homeDir/.volta/bin",
                    "$homeDir/.nvm/current/bin",
                    "$homeDir/.npm-global/bin"
                ).joinToString(":")
            } else ""
            pb.environment()["PATH"] =
                if (additionalPaths.isNotBlank()) "$additionalPaths:$currentPath" else currentPath
            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val resolved = reader.readLine()
            if (!resolved.isNullOrBlank()) return resolved.trim()
        } catch (e: Exception) {
            logger<CustomMcpServerManager>().warn("Failed to resolve command using which: $e")
        }
    }
    return command
}