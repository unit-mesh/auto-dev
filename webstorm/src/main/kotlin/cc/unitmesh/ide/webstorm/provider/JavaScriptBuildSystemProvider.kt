package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.template.DockerfileContext
import cc.unitmesh.ide.webstorm.JsDependenciesSnapshot
import com.intellij.javascript.nodejs.packageJson.PackageJsonFileManager
import com.intellij.openapi.project.Project

class JavaScriptBuildSystemProvider : BuildSystemProvider {
    override fun collect(project: Project): DockerfileContext {
        val snapshot = JsDependenciesSnapshot.create(project, null)
        var language = "JavaScript"
        var languageVersion = "ES5"
        val buildTool = "NPM"

        val packageJson = snapshot.packages["typescript"]
        val tsVersion = packageJson?.parseVersion()
        if (tsVersion != null) {
            language = "TypeScript"
            languageVersion = tsVersion.rawVersion
        }

        // read scripts from package.json

        return DockerfileContext(
            buildToolName = buildTool,
            buildToolVersion = "",
            languageName = language,
            languageVersion = languageVersion
        )
    }
}
