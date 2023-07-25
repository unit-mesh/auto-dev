package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.prompting.code.TestStack
import cc.unitmesh.devti.provider.TechStackProvider
import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.javascript.nodejs.PackageJsonDependency
import com.intellij.javascript.nodejs.packageJson.PackageJsonFileManager
import com.intellij.openapi.project.ProjectManager

class JavaScriptTechStackService : TechStackProvider() {
    override fun prepareLibrary(): TestStack {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return TestStack()

        val packageFile =
            PackageJsonFileManager.getInstance(project).validPackageJsonFiles.firstOrNull() ?: return TestStack()
        val packageJsonData = PackageJsonData.getOrCreate(packageFile)

        val devDependencies = mutableMapOf<String, String>()
        val dependencies = mutableMapOf<String, String>()

        // merge frameworks, if start with start name: like: "react", "react-dom", "react-router-dom"
        val frameworks = mutableMapOf<String, Boolean>()
        val frameworkNames = listOf("react", "vue", "angular", "jquery", "bootstrap", "antd", "material-ui")

        packageJsonData.allDependencyEntries.forEach { (name, entry) ->
            entry.dependencyType.let {
                when (it) {
                    PackageJsonDependency.dependencies -> {
                        if (!name.startsWith("@types/")) {
                            devDependencies[name] = entry.versionRange
                        }

                        // merge frameworks
                        frameworkNames.forEach { frameworkName ->
                            if (name.startsWith(frameworkName)) {
                                frameworks[frameworkName] = true
                            }
                        }
                    }

                    PackageJsonDependency.devDependencies -> devDependencies[name] = entry.versionRange
                    PackageJsonDependency.peerDependencies -> {}
                    PackageJsonDependency.optionalDependencies -> {}
                    PackageJsonDependency.bundledDependencies -> {
                    }
                }
            }
        }


        return TestStack(frameworks, frameworks, dependencies, devDependencies)
    }
}
