// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.devti.sketch.run

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WSLUtil
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.EnvironmentUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

object ShellUtil {
    private fun listWslShell() : List<String>? {
        if (WSLDistribution.findWslExe() == null) return listOf()
        val distributions = WslDistributionManager.getInstance().installedDistributions
        return distributions.mapNotNull { it.shellPath }
    }

    fun detectShells(): List<String> {
        val shells: MutableList<String> = ArrayList()
        if (SystemInfo.isUnix) {
            addIfExists(shells, "/bin/bash")
            addIfExists(shells, "/usr/bin/bash")
            addIfExists(shells, "/usr/local/bin/bash")
            addIfExists(shells, "/opt/homebrew/bin/bash")

            addIfExists(shells, "/bin/zsh")
            addIfExists(shells, "/usr/bin/zsh")
            addIfExists(shells, "/usr/local/bin/zsh")
            addIfExists(shells, "/opt/homebrew/bin/zsh")

            addIfExists(shells, "/bin/fish")
            addIfExists(shells, "/usr/bin/fish")
            addIfExists(shells, "/usr/local/bin/fish")
            addIfExists(shells, "/opt/homebrew/bin/fish")

            addIfExists(shells, "/opt/homebrew/bin/pwsh")
        } else if (SystemInfo.isWindows) {
            val powershell = PathEnvironmentVariableUtil.findInPath("powershell.exe")
            if (powershell != null &&
                StringUtil.startsWithIgnoreCase(powershell.absolutePath, "C:\\Windows\\System32\\WindowsPowerShell\\")
            ) {
                shells.add(powershell.absolutePath)
            }
            val cmd = PathEnvironmentVariableUtil.findInPath("cmd.exe")
            if (cmd != null && StringUtil.startsWithIgnoreCase(cmd.absolutePath, "C:\\Windows\\System32\\")) {
                shells.add(cmd.absolutePath)
            }
            val pwsh = PathEnvironmentVariableUtil.findInPath("pwsh.exe")
            if (pwsh != null && StringUtil.startsWithIgnoreCase(pwsh.absolutePath, "C:\\Program Files\\PowerShell\\")) {
                shells.add(pwsh.absolutePath)
            }
            val gitBash = File("C:\\Program Files\\Git\\bin\\bash.exe")
            if (gitBash.isFile) {
                shells.add(gitBash.absolutePath)
            }
            var cmderRoot = EnvironmentUtil.getValue("CMDER_ROOT")
            if (cmderRoot == null) {
                cmderRoot = EnvironmentVariablesData.DEFAULT.envs["CMDER_ROOT"]
            }
            if (cmderRoot != null && cmd != null &&
                StringUtil.startsWithIgnoreCase(cmd.absolutePath, "C:\\Windows\\System32\\")
            ) {
                shells.add("cmd.exe /k \"%CMDER_ROOT%\\vendor\\init.bat\"")
            }
        }

        return shells
    }

    private fun addIfExists(shells: MutableList<String>, filePath: String) {
        if (Files.exists(Path.of(filePath))) {
            shells.add(filePath)
        }
    }
}