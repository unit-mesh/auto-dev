package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.template.DockerfileContext
import cc.unitmesh.ide.webstorm.JsDependenciesSnapshot
import com.intellij.javascript.nodejs.packageJson.PackageJsonFileManager
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager


class JavaScriptBuildSystemProvider : BuildSystemProvider() {
    override fun collect(project: Project): DockerfileContext? {
        val snapshot = JsDependenciesSnapshot.create(project, null)
        if (snapshot.packages.isEmpty()) {
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

//        runReadAction {
//            PackageJsonFileManager.getInstance(project).validPackageJsonFiles.map {
//                val psiFile = PsiManager.getInstance(project).findFile(it)
//                // read "scripts" from package.json
//                val jsonFile = psiFile as JsonFile
//                val scripts = jsonFile.allTopLevelValues.firstOrNull { value -> value.name == "scripts" }
//                println(scripts)
//            }
//        }

        return DockerfileContext(
            buildToolName = buildTool,
            buildToolVersion = "",
            languageName = language,
            languageVersion = languageVersion
        )
    }
}
