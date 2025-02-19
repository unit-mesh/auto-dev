package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.provider.DevPackage
import cc.unitmesh.devti.template.context.DockerfileContext
import cc.unitmesh.idea.detectLanguageLevel
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.ui.completion.TextCompletionInfo
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiFile
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.plugins.gradle.service.project.GradleTasksIndices
import org.jetbrains.plugins.gradle.util.GradleConstants


open class JavaBuildSystemProvider : BuildSystemProvider() {
    override fun collect(project: Project): DockerfileContext? {
        val projectDataManager = ProjectDataManager.getInstance()
        val buildToolName: String
        var taskString = ""

        val gradleInfo = projectDataManager.getExternalProjectsData(project, GradleConstants.SYSTEM_ID)
        val mavenProjects = MavenProjectsManager.getInstance(project).projects

        if (gradleInfo.isNotEmpty()) {
            buildToolName = "Gradle"
            val indices = GradleTasksIndices.getInstance(project)

            val tasks = indices.findTasks(project.guessProjectDir()!!.path)
                .filterNot { it.isInherited }
                .groupBy { it.name }
                .map { TextCompletionInfo(it.key, it.value.first().description) }
                .sortedWith(Comparator.comparing({ it.text }, GRADLE_COMPLETION_COMPARATOR))

            taskString = tasks.joinToString(" ") { it.text }
        } else if (mavenProjects.isNotEmpty()) {
            buildToolName = "Maven"
        } else {
            return null
        }

        val javaVersion = detectLanguageLevel(project, null)
        if (javaVersion == null) {
            return null
        }

        return DockerfileContext(
            buildToolName = buildToolName,
            buildToolVersion = "",
            languageName = "Java",
            languageVersion = "$javaVersion",
            taskString = taskString
        )
    }

    override fun isDeclarePackageFile(filename: String): Boolean {
        return filename == "build.gradle" || filename == "pom.xml" || filename == "build.gradle.kts"
    }

    override fun collectDependencies(
        project: Project,
        buildFilePsi: PsiFile
    ): List<DevPackage> {
        val mavenProject = MavenProjectsManager.getInstance(project).findProject(buildFilePsi.virtualFile)
        var results = mutableListOf<DevPackage>()
        if (mavenProject != null) {
            results += mavenProject.dependencies
                .mapNotNull { mavenArtifact ->
                    ProgressManager.checkCanceled()
                    val name: String = mavenArtifact.artifactId ?: return@mapNotNull null
                    val version: String = mavenArtifact.version ?: return@mapNotNull null

                    DevPackage("maven", name = name, version = version)
                }
        }

//        ModuleUtilCore.findModuleForFile(psiFile)?.also {
//            val moduleData = CachedModuleDataFinder.findModuleData(it)
//            val libDepData = ExternalSystemApiUtil.findAllRecursively<LibraryDependencyData?>(
//                moduleData,
//                ProjectKeys.LIBRARY_DEPENDENCY
//            )
//
//            results += libDepData.mapNotNull { node ->
//                val target = node.data.target
//                val namespace = target.groupId ?: return@mapNotNull null
//                val name = target.artifactId ?: return@mapNotNull null
//                val version = target.version ?: return@mapNotNull null
//
//                DevPackage("maven", namespace, name, version)
//            }
//        }


        return results
    }

    companion object {
        val GRADLE_COMPLETION_COMPARATOR = Comparator<String> { o1, o2 ->
            when {
                o1.startsWith("--") && o2.startsWith("--") -> o1.compareTo(o2)
                o1.startsWith("-") && o2.startsWith("--") -> -1
                o1.startsWith("--") && o2.startsWith("-") -> 1
                o1.startsWith(":") && o2.startsWith(":") -> o1.compareTo(o2)
                o1.startsWith(":") && o2.startsWith("-") -> -1
                o1.startsWith("-") && o2.startsWith(":") -> 1
                o2.startsWith("-") -> -1
                o2.startsWith(":") -> -1
                o1.startsWith("-") -> 1
                o1.startsWith(":") -> 1
                else -> o1.compareTo(o2)
            }
        }
    }
}

