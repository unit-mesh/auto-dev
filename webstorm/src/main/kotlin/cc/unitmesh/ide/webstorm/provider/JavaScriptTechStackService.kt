package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.prompting.code.TestStack
import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.javascript.nodejs.PackageJsonDependency
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir

class JavaScriptTechStackService {
    fun prepareLibrary(): TestStack {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return TestStack()

        val baseDir = project.guessProjectDir() ?: return TestStack()
        val packageFile = PackageJsonUtil.findUpPackageJson(baseDir) ?: return TestStack()
        val packageJsonData = PackageJsonData.getOrCreate(packageFile)

        val devDependencies = mutableMapOf<String, String>()
        val dependencies = mutableMapOf<String, String>()

        // merge frameworks, if start with start name: like: "react", "react-dom", "react-router-dom"
        val frameworks = mutableMapOf<String, Boolean>()
        val frameworkNames = listOf("react", "vue", "angular", "jquery", "bootstrap", "antd", "material-ui")

        val testFrameworks = mutableMapOf<String, Boolean>()
        val testFrameworksNames = listOf(
            "jest",
            "mocha",
            "jasmine",
            "karma",
            "ava",
            "tape",
            "qunit",
            "tap",
            "ava",
            "cypress",
            "protractor",
            "nightwatch",
            "selenium",
            "webdriverio"
        )

        packageJsonData.allDependencyEntries.forEach { (name, entry) ->
            entry.dependencyType.let {
                when (it) {
                    PackageJsonDependency.dependencies -> {
                        // also remove `eslint`
                        if (!name.startsWith("@types/")) {
                            devDependencies[name] = entry.versionRange
                        }

                        // merge frameworks
                        frameworkNames.forEach { frameworkName ->
                            if (name.startsWith(frameworkName) || name == frameworkName) {
                                frameworks[frameworkName] = true
                            }
                        }
                        testFrameworksNames.forEach { testFrameworkName ->
                            if (name.startsWith(testFrameworkName) || name == testFrameworkName) {
                                testFrameworks[testFrameworkName] = true
                            }
                        }
                    }

                    PackageJsonDependency.devDependencies -> {
                        devDependencies[name] = entry.versionRange

                        frameworkNames.forEach { frameworkName ->
                            if (name.startsWith(frameworkName) || name == frameworkName) {
                                frameworks[frameworkName] = true
                            }
                        }
                        testFrameworksNames.forEach { testFrameworkName ->
                            if (name.startsWith(testFrameworkName) || name == testFrameworkName) {
                                testFrameworks[testFrameworkName] = true
                            }
                        }
                    }

                    PackageJsonDependency.peerDependencies -> {}
                    PackageJsonDependency.optionalDependencies -> {}
                    PackageJsonDependency.bundledDependencies -> {
                    }
                }
            }
        }


        return TestStack(frameworks, testFrameworks, dependencies, devDependencies)
    }
}
