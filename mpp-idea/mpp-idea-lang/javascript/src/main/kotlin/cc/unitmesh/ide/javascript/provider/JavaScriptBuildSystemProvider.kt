package cc.unitmesh.ide.javascript.provider

import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.provider.DevPackage
import cc.unitmesh.devti.template.context.DockerfileContext
import cc.unitmesh.ide.javascript.JsDependenciesSnapshot
import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.lang.javascript.buildTools.npm.NpmScriptsUtil
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiFile


class JavaScriptBuildSystemProvider : BuildSystemProvider() {
    override fun collect(project: Project): DockerfileContext? {
        val snapshot = JsDependenciesSnapshot.create(project, null)
        if (snapshot.packageJsonFiles.isEmpty()) {
            return null
        }

        var language = "JavaScript"
        var languageVersion = ""
        var buildTool = "NPM" // default built tool

        val packageJson = snapshot.packages["typescript"]
        val tsVersion = packageJson?.parseVersion()
        if (tsVersion != null) {
            language = "TypeScript"
            languageVersion = tsVersion.rawVersion
        }

        // vite, webpack, parcel, rollup
        when {
            snapshot.packages.containsKey("vite") -> {
                buildTool = "Vite"
            }

            snapshot.packages.containsKey("webpack") -> {
                buildTool = "Webpack"
            }

            snapshot.packages.containsKey("parcel") -> {
                buildTool = "Parcel"
            }

            snapshot.packages.containsKey("rollup") -> {
                buildTool = "Rollup"
            }
        }

        when {
            snapshot.packages.containsKey("vue") -> {
                language = "Vue " + snapshot.packages["vue"]?.versionString()
            }

            snapshot.packages.containsKey("react") -> {
                language = "React " + snapshot.packages["react"]?.versionString()
            }

            snapshot.packages.containsKey("angular") -> {
                language = "Angular " + snapshot.packages["angular"]?.versionString()
            }

            snapshot.packages.containsKey("next") -> {
                language = "Next.js " + snapshot.packages["next"]?.versionString()
            }
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

    override fun isDeclarePackageFile(filename: String): Boolean {
        return filename == "package.json"
    }

    override fun collectDependencies(project: Project, buildFilePsi: PsiFile): List<DevPackage> {
        val packageJson = buildFilePsi as? JsonFile ?: return emptyList()
        return PackageJsonUtil.getDependencies(packageJson, PackageJsonUtil.PROD_DEV_DEPENDENCIES)
            .mapNotNull { jsonProperty ->
                val packageName = jsonProperty.name
                val version: String = (jsonProperty.value as? JsonStringLiteral)?.value ?: return@mapNotNull null

                DevPackage("npm", name = packageName, version = version)
            }.toList()
    }

}

fun PackageJsonData.PackageJsonDependencyEntry.versionString(): String {
    var version = this.parseVersion()
    return if (version != null) " " + version.rawVersion else ""
}
