package cc.unitmesh.agent.tool.shell

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Utilities for loading and managing shell environment variables.
 * Provides login shell environment inheritance to resolve user-installed tools
 * like Homebrew packages that are not available in GUI-launched processes.
 */
object ShellEnvironmentUtils {
    private val loginEnvCache = ConcurrentHashMap<String, Map<String, String>>()

    /**
     * Load environment variables from user's login shell (e.g., .zshrc, .bash_profile).
     * Results are cached per shell to avoid repeated process spawns.
     */
    fun loadLoginShellEnvironment(shell: String? = null): Map<String, String> {
        val effectiveShell = shell ?: getDefaultShell() ?: return emptyMap()
        
        return loginEnvCache.getOrPut(effectiveShell) {
            try {
                val args = when {
                    effectiveShell.endsWith("zsh") -> listOf(effectiveShell, "-lic", "env")
                    effectiveShell.endsWith("bash") -> listOf(effectiveShell, "-lc", "env")
                    effectiveShell.endsWith("fish") -> listOf(effectiveShell, "-lc", "env")
                    else -> listOf(effectiveShell, "-lc", "env")
                }
                val process = ProcessBuilder(args)
                    .redirectErrorStream(true)
                    .start()
                process.waitFor(3, TimeUnit.SECONDS)
                val exit = runCatching { process.exitValue() }.getOrNull()
                if (exit != null && exit != 0) return@getOrPut emptyMap()
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.mapNotNull { line ->
                        val idx = line.indexOf('=')
                        if (idx > 0) {
                            val key = line.substring(0, idx)
                            val value = line.substring(idx + 1)
                            key to value
                        } else null
                    }.toMap()
                }
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    /**
     * Get the default shell for the current platform.
     */
    fun getDefaultShell(): String? {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("windows") -> {
                listOf("powershell.exe", "cmd.exe").firstOrNull { shellExists(it) }
            }
            os.contains("mac") || os.contains("darwin") -> {
                listOf("/bin/zsh", "/bin/bash", "/bin/sh").firstOrNull { shellExists(it) }
            }
            else -> {
                listOf("/bin/bash", "/bin/sh", "/bin/zsh").firstOrNull { shellExists(it) }
            }
        }
    }

    /**
     * Check if a shell executable exists and is executable.
     */
    fun shellExists(shellPath: String): Boolean {
        return try {
            val file = File(shellPath)
            file.exists() && file.canExecute()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Merge two PATH values, removing duplicates while preserving order.
     * Login paths come first to give user customizations priority.
     */
    fun mergePath(login: String?, current: String?): String? {
        if (login == null && current == null) return null
        if (login == null) return current
        if (current == null) return login
        val loginParts = login.split(':').filter { it.isNotBlank() }
        val currentParts = current.split(':').filter { it.isNotBlank() }
        return (loginParts + currentParts).distinct().joinToString(":")
    }

    /**
     * Ensure Homebrew path is present on macOS (Apple Silicon default location).
     */
    fun ensureHomebrewPath(path: String): String {
        val brewBin = "/opt/homebrew/bin"
        return if (!path.contains(brewBin) && File(brewBin).exists()) {
            "$brewBin:$path".trim(':')
        } else path
    }

    /**
     * Apply login shell environment to an existing environment map.
     * Merges PATH specially and adds missing variables from login shell.
     */
    fun applyLoginEnvironment(
        environment: MutableMap<String, String>,
        shell: String? = null
    ) {
        val loginEnv = loadLoginShellEnvironment(shell)
        if (loginEnv.isNotEmpty()) {
            val mergedPath = mergePath(loginEnv["PATH"], environment["PATH"]) 
                ?: environment["PATH"] 
                ?: loginEnv["PATH"]
            
            loginEnv.forEach { (k, v) ->
                if (k != "PATH" && !environment.containsKey(k)) {
                    environment[k] = v
                }
            }
            
            if (mergedPath != null) {
                environment["PATH"] = ensureHomebrewPath(mergedPath)
            }
        } else {
            // Fallback: ensure Homebrew path if on macOS
            environment["PATH"] = ensureHomebrewPath(environment["PATH"] ?: "")
        }
    }
}
