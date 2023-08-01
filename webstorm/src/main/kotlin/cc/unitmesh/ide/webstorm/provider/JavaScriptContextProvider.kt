package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.prompting.code.TestStack
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.ide.webstorm.DependenciesSnapshot
import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.javascript.nodejs.PackageJsonDependency
import com.intellij.json.JsonLanguage
import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.javascript.JavaScriptSupportLoader
import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiFile
import com.intellij.util.PlatformUtils

class JavaScriptContextProvider : ChatContextProvider {
    private val supportedLanguages = setOf(JavascriptLanguage.INSTANCE.id, JavaScriptSupportLoader.TYPESCRIPT.id)
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        if (PlatformUtils.isWebStorm()) return true

        val sourceFile: PsiFile = creationContext.sourceFile ?: return false
        val language = sourceFile.language

        return supportedLanguages.contains(language.id) || language is HTMLLanguage || language is JsonLanguage
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val results = mutableListOf<ChatContextItem>()
        val snapshot = DependenciesSnapshot.create(project, creationContext)

        val typeScriptLanguageContext = getTypeScriptLanguageContext(snapshot)
        if (typeScriptLanguageContext != null) results.add(typeScriptLanguageContext)

        val techStack = prepareLibrary()
        if (techStack.coreFrameworks().isNotEmpty()) {
            results.add(
                ChatContextItem(
                    JavaScriptContextProvider::class,
                    "The project uses the following JavaScript component frameworks: ${techStack.coreFrameworks()}"
                )
            )
        }

        if (techStack.testFrameworks().isNotEmpty()) {
            results.add(
                ChatContextItem(
                    JavaScriptContextProvider::class,
                    "The project uses ${techStack.testFrameworks()} to test."
                )
            )
        }

        return results
    }

    private fun getTypeScriptLanguageContext(snapshot: DependenciesSnapshot): ChatContextItem? {
        val packageJson = snapshot.packages["typescript"] ?: return null
        val version = packageJson.parseVersion()
        return ChatContextItem(
            JavaScriptContextProvider::class,
            "The project uses TypeScript language" + (version?.let { ", version: $version" } ?: ""))
    }

    fun prepareLibrary(): TestStack {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return TestStack()

        val baseDir = project.guessProjectDir() ?: return TestStack()
        val packageFile = PackageJsonUtil.findUpPackageJson(baseDir) ?: return TestStack()
        val packageJsonData = PackageJsonData.getOrCreate(packageFile)

        val devDependencies = mutableMapOf<String, String>()
        val dependencies = mutableMapOf<String, String>()

        val frameworks = mutableMapOf<String, Boolean>()
        val testFrameworks = mutableMapOf<String, Boolean>()

        packageJsonData.allDependencyEntries.forEach { (name, entry) ->
            entry.dependencyType.let {
                when (it) {
                    PackageJsonDependency.dependencies -> {
                        // also remove `eslint`
                        if (!name.startsWith("@types/")) {
                            devDependencies[name] = entry.versionRange
                        }

                        JsWebFrameworks.values().forEach { framework ->
                            if (name.startsWith(framework.name) || name == framework.name) {
                                frameworks[framework.name] = true
                            }
                        }
                        JsTestFrameworks.values().forEach { testFramework ->
                            if (name.startsWith(testFramework.name) || name == testFramework.name) {
                                testFrameworks[testFramework.name] = true
                            }
                        }
                    }

                    PackageJsonDependency.devDependencies -> {
                        devDependencies[name] = entry.versionRange

                        JsWebFrameworks.values().forEach { frameworkName ->
                            if (name.startsWith(frameworkName.name) || name == frameworkName.name) {
                                frameworks[frameworkName.name] = true
                            }
                        }
                        JsTestFrameworks.values().forEach { testFramework ->
                            if (name.startsWith(testFramework.name) || name == testFramework.name) {
                                testFrameworks[testFramework.name] = true
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
