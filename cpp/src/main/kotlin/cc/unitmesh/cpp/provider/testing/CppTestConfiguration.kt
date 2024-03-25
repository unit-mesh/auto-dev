package cc.unitmesh.cpp.provider.testing

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.cpp.cmake.model.CMakeTarget
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cidr.cpp.execution.testing.CMakeTestRunConfiguration
import com.jetbrains.cidr.execution.ExecutableData
import com.jetbrains.cidr.execution.BuildTargetData
import com.jetbrains.cidr.execution.BuildTargetAndConfigurationData

object CppTestConfiguration {
    fun createConfiguration(project: Project, file: VirtualFile, configurationFactory: ConfigurationFactory): List<RunnerAndConfigurationSettings> {
        val targets = getAllCmakeTargetsForTestFiles(project, file)
        return targets.map { createConfiguration(project, it, configurationFactory) }
    }

    private fun getAllCmakeTargetsForTestFiles(project: Project, file: VirtualFile): List<CMakeTarget> {
        val model = CMakeWorkspace.getInstance(project).model ?: return emptyList()

        val targets: CMakeTarget? = model.targets.find { target ->
            target.buildConfigurations.find { config ->
                config.sources.map { FileUtil.toSystemIndependentName(it.path) }.contains(file.path)
            } != null
        }

        return listOfNotNull(targets)
    }

    fun createConfiguration(
        project: Project,
        cMakeTarget: CMakeTarget,
        factory: ConfigurationFactory
    ): RunnerAndConfigurationSettings {
        val configurationSettings = RunManager.getInstance(project).createConfiguration(cMakeTarget.name, factory)
        val cMakeTestRunConfiguration = configurationSettings.configuration as CMakeTestRunConfiguration
        val buildTargetData = BuildTargetData(cMakeTarget)
        cMakeTestRunConfiguration.targetAndConfigurationData =
            BuildTargetAndConfigurationData(buildTargetData, cMakeTarget.name)
        cMakeTestRunConfiguration.executableData = ExecutableData(buildTargetData)
        return configurationSettings
    }
}