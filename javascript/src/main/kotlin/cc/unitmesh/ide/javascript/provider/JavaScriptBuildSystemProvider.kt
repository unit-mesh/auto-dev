package cc.unitmesh.ide.javascript.provider

import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.template.context.DockerfileContext
import cc.unitmesh.ide.javascript.JsDependenciesSnapshot
import com.intellij.lang.javascript.buildTools.npm.NpmScriptsUtil
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir


class JavaScriptBuildSystemProvider : BuildSystemProvider() {
    override fun collect(project: Project): DockerfileContext? {
        val snapshot = JsDependenciesSnapshot.create(project, null)
        if (snapshot.packageJsonFiles.isEmpty()) {
            return null
        }

        var language = "JavaScript"
        var languageVersion = "ES5"
        val buildTool = "NPM"

        val packageJson = snapshot.packages["typescript"]
        val tsVersion = packageJson?.parseVersion()
        if (tsVersion != null) {
            language = "TypeScript"
            languageVersion = tsVersion.rawVersion
        }

        var taskString = ""
        runReadAction {
            val root = PackageJsonUtil.findChildPackageJsonFile(project.guessProjectDir()) ?: return@runReadAction
            NpmScriptsUtil.listTasks(project, root).scripts.forEach { task ->
                taskString += task.name + " "
            }
        }

        return DockerfileContext(
            buildToolName = buildTool,
            buildToolVersion = "",
            languageName = language,
            languageVersion = languageVersion,
            taskString = taskString
        )
    }
}
