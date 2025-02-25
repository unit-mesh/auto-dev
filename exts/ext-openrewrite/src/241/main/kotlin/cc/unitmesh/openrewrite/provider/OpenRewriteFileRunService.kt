package cc.unitmesh.openrewrite.provider

import cc.unitmesh.devti.provider.RunService
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class OpenRewriteFileRunService : RunService {
    override fun isApplicable(project: Project, file: VirtualFile): Boolean {
        return file.extension == "yml" && isOpenWriteFile(project, file)
    }

    private fun isOpenWriteFile(project: Project, file: VirtualFile): Boolean {
        try {
            val clazz = Class.forName("com.intellij.openRewrite.OpenRewriteFileService")
            val getInstanceMethod = clazz.getDeclaredMethod("getInstance")
            val isRecipeMethod = clazz.getDeclaredMethod("isRecipe", PsiFile::class.java)

            val fileService = getInstanceMethod.invoke(null)
            val psiFile = runReadAction {
                PsiManager.getInstance(project).findFile(file)
            } ?: return false

            isRecipeMethod.isAccessible = true

            val result = isRecipeMethod.invoke(fileService, psiFile) as Boolean
            return result
        } catch (e: Exception) {
            return false
        }
    }

    override fun runConfigurationClass(project: Project): Class<out RunProfile>? = null

    override fun runFile(
        project: Project,
        virtualFile: VirtualFile,
        psiElement: PsiElement?,
        isFromToolAction: Boolean
    ): String? {
        if (!isOpenWriteFile(project, virtualFile)) {
            return ""
        }

        val runManager = RunManager.getInstance(project)
        val allSettings = runManager.allSettings

        val workingPath = virtualFile.parent.path

        var settings = allSettings.firstOrNull {
            try {
                val config = it.configuration
                val configClass = config::class.java

                if (configClass.name == "com.intellij.openRewrite.run.OpenRewriteRunConfiguration") {
                    val getExpandedWorkingDirectoryMethod = configClass.getMethod("getExpandedWorkingDirectory")
                    val workingDirectory = getExpandedWorkingDirectoryMethod.invoke(config) as? String
                    workingDirectory == workingPath
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }

        val list = try {
            getRecipeDescriptors(project, virtualFile) ?: return null
        } catch (e: Exception) {
            return null
        }
        if (list.isEmpty()) return null

        if (settings == null) {
            try {
                val configurationType = ConfigurationTypeUtil.findConfigurationType("OpenRewriteRunConfigurationType")!!
                settings = runManager.createConfiguration("", configurationType.configurationFactories[0])
                val configuration = settings.configuration

                if (configuration.javaClass.name == "com.intellij.openRewrite.run.OpenRewriteRunConfiguration") {
                    configureOpenRewrite(configuration, project, virtualFile, list)
                }

                runManager.setUniqueNameIfNeeded(settings)
                runManager.setTemporaryConfiguration(settings)
            } catch (e: Exception) {
                return null
            }
        }

        val builder = ExecutionEnvironmentBuilder.createOrNull(DefaultRunExecutor.getRunExecutorInstance(), settings)
        builder?.let {
            ExecutionManager.getInstance(project).restartRunProfile(it.build())
        }

        return ""
    }

    private fun configureOpenRewrite(
        configuration: RunConfiguration,
        project: Project,
        virtualFile: VirtualFile,
        list: java.util.LinkedHashMap<*, *>,
    ) {
        val directoryMethod =
            configuration::class.java.getMethod("setWorkingDirectory", String::class.java)
        val projectRoot = project.basePath ?: ""
        directoryMethod.invoke(configuration, projectRoot)

        val setConfigLocationMethod =
            configuration::class.java.getMethod("setConfigLocation", String::class.java)
        setConfigLocationMethod.invoke(configuration, virtualFile.path)

        val setActiveRecipesMethod =
            configuration::class.java.getMethod("setActiveRecipes", String::class.java)
        setActiveRecipesMethod.invoke(configuration, list.keys.first())

        val nameMethod = configuration::class.java.getMethod("setGeneratedName")
        nameMethod.invoke(configuration)
    }

    private fun getRecipeDescriptors(project: Project, virtualFile: VirtualFile): LinkedHashMap<*, *>? {
        val clazz = Class.forName("com.intellij.openRewrite.recipe.OpenRewriteRecipeService")
        val getInstanceMethod = clazz.getDeclaredMethod("getInstance", Project::class.java)
        val recipeService = getInstanceMethod.invoke(null, project)
        val OpenRewriteType = Class.forName("com.intellij.openRewrite.recipe.OpenRewriteType").getEnumConstants()[0]

        val method =
            clazz.getDeclaredMethod("getLocalDescriptors", PsiFile::class.java, OpenRewriteType::class.java)
        method.isAccessible = true

        val psiFile = runReadAction {
            PsiManager.getInstance(project).findFile(virtualFile)
        } ?: return null

        val list = runReadAction {
            val type = OpenRewriteType::class.java.enumConstants[0]
            method.invoke(recipeService, psiFile, type) as LinkedHashMap<*, *>
        }

        return list
    }
}