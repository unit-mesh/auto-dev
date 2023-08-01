package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.prompting.code.TestStack
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.ide.webstorm.JsDependenciesSnapshot
import cc.unitmesh.ide.webstorm.JsDependenciesSnapshot.Companion.mostPopularPackages
import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.javascript.nodejs.PackageJsonDependency
import com.intellij.json.JsonLanguage
import com.intellij.lang.Language
import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.javascript.JavaScriptSupportLoader
import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.lang.javascript.dialects.TypeScriptJSXLanguageDialect
import com.intellij.openapi.diagnostic.logger
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
        val language: Language = sourceFile.language

        return supportedLanguages.contains(language.id) || language is HTMLLanguage || language is JsonLanguage || language is TypeScriptJSXLanguageDialect
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val results = mutableListOf<ChatContextItem>()
        val snapshot = JsDependenciesSnapshot.create(project, creationContext)

        val typeScriptLanguageContext = getTypeScriptLanguageContext(snapshot)
        if (typeScriptLanguageContext != null) results.add(typeScriptLanguageContext)

        val mostPopularPackagesContext = getMostPopularPackagesContext(snapshot)
        if (mostPopularPackagesContext != null) results.add(mostPopularPackagesContext)

        val techStack = prepareStack(snapshot)
        logger<JavaScriptContextProvider>().warn("Tech stack: $techStack")
        if (techStack.coreFrameworks().isNotEmpty()) {
            val element = ChatContextItem(
                JavaScriptContextProvider::class,
                "The project uses the following JavaScript component frameworks: ${techStack.coreFrameworks()}"
            )
            results.add(element)
        }

        if (techStack.testFrameworks().isNotEmpty()) {
            val testChatContext = ChatContextItem(
                JavaScriptContextProvider::class,
                "The project uses ${techStack.testFrameworks()} to test."
            )

            results.add(testChatContext)
        }

        return results
    }

    private fun getMostPopularPackagesContext(snapshot: JsDependenciesSnapshot): ChatContextItem? {
        val dependencies = snapshot.packages
            .asSequence()
            .filter { entry -> mostPopularPackages.contains(entry.key) && !entry.key.startsWith("@type") }
            .map { entry ->
                val dependency = entry.key
                val version = entry.value.parseVersion()
                if (version != null) "$dependency: $version" else dependency
            }
            .toList()

        if (dependencies.isEmpty()) return null

        return ChatContextItem(
            JavaScriptContextProvider::class,
            "The project uses the following JavaScript packages: ${dependencies.joinToString(", ")}"
        )
    }

    private fun getTypeScriptLanguageContext(snapshot: JsDependenciesSnapshot): ChatContextItem? {
        val packageJson = snapshot.packages["typescript"] ?: return null
        val version = packageJson.parseVersion()
        return ChatContextItem(
            JavaScriptContextProvider::class,
            "The project uses TypeScript language" + (version?.let { ", version: $version" } ?: ""))
    }

    fun prepareStack(snapshot: JsDependenciesSnapshot): TestStack {
        val devDependencies = mutableMapOf<String, String>()
        val dependencies = mutableMapOf<String, String>()

        val frameworks = mutableMapOf<String, Boolean>()
        val testFrameworks = mutableMapOf<String, Boolean>()

        snapshot.packages.forEach { (name, entry) ->
            entry.dependencyType.let {
                when (it) {
                    PackageJsonDependency.dependencies,
                    PackageJsonDependency.devDependencies -> {
                        // also remove `eslint`
                        if (!name.startsWith("@types/")) {
                            devDependencies[name] = entry.versionRange
                        }

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

                    else -> {}
                }
            }
        }


        return TestStack(frameworks, testFrameworks, dependencies, devDependencies)
    }
}
