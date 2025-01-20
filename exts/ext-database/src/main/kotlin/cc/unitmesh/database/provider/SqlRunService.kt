package cc.unitmesh.database.provider

import cc.unitmesh.database.util.DatabaseSchemaAssistant
import cc.unitmesh.devti.provider.RunService
import com.intellij.database.console.runConfiguration.DatabaseScriptRunConfiguration
import com.intellij.database.console.runConfiguration.DatabaseScriptRunConfigurationOptions
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.sql.SqlFileType
import com.intellij.sql.psi.SqlFile

class SqlRunService : RunService {
    override fun isApplicable(project: Project, file: VirtualFile): Boolean {
        return file.extension == "sql"
    }

    override fun runFile(
        project: Project,
        virtualFile: VirtualFile,
        psiElement: PsiElement?,
        isFromToolAction: Boolean
    ): String? {
        val sql = runReadAction { PsiManager.getInstance(project).findFile(virtualFile) } as? SqlFile
            ?: return null

        if (sql.fileType !is SqlFileType) return null
        val content = runReadAction { sql.text }
        return DatabaseSchemaAssistant.executeSqlQuery(project, content)
    }

    override fun runConfigurationClass(project: Project): Class<out RunProfile>? =
        DatabaseScriptRunConfiguration::class.java

    override fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? {
        return createRunnerConfig(project, virtualFile)?.configuration
    }

    override fun createRunSettings(
        project: Project,
        virtualFile: VirtualFile,
        testElement: PsiElement?
    ): RunnerAndConfigurationSettings? {
        return createRunnerConfig(project, virtualFile)
    }

    private fun createRunnerConfig(project: Project, file: VirtualFile): RunnerAndConfigurationSettings? {
        val psiFile = runReadAction { PsiManager.getInstance(project).findFile(file) } as? SqlFile ?: return null
        val dataSource = DatabaseSchemaAssistant.getDataSources(project).firstOrNull() ?: return null

        val configurationsFromContext = runReadAction {
            ConfigurationContext(psiFile).configurationsFromContext.orEmpty()
        }

        val configurationSettings = configurationsFromContext
            .firstOrNull { it.configuration is DatabaseScriptRunConfiguration }
            ?.configurationSettings
            ?: return null

        val target = DatabaseScriptRunConfigurationOptions.Target(dataSource.uniqueId, null)
        // Safe cast because configuration was checked before
        (configurationSettings.configuration as DatabaseScriptRunConfiguration).options.targets.add(target)
        configurationSettings.isActivateToolWindowBeforeRun = false

        return configurationSettings
    }
}
