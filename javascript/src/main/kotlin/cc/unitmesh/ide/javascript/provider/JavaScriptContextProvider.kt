package cc.unitmesh.ide.javascript.provider

import cc.unitmesh.devti.prompting.code.TechStack
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.ide.javascript.JsDependenciesSnapshot
import cc.unitmesh.ide.javascript.util.LanguageApplicableUtil
import cc.unitmesh.ide.javascript.util.JsUtil.guessTestFrameworkName
import com.intellij.javascript.nodejs.PackageJsonDependency
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class JavaScriptContextProvider : ChatContextProvider {
    private val log = logger<JavaScriptContextProvider>()

    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        val sourceFile: PsiFile = creationContext.sourceFile ?: return false
        return LanguageApplicableUtil.isJavaScriptApplicable(sourceFile.language)
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val results = mutableListOf<ChatContextItem>()
        val snapshot = JsDependenciesSnapshot.create(project, creationContext?.sourceFile)
        val typeScriptLanguageContext = getTypeScriptLanguageContext(snapshot)
        val mostPopularPackagesContext = getMostPopularPackagesContext(snapshot)

        val techStack = prepareStack(snapshot)

        if (typeScriptLanguageContext != null) results.add(typeScriptLanguageContext)

        if (mostPopularPackagesContext != null) results.add(mostPopularPackagesContext)

        log.info("Tech stack: $techStack")
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
        } else {
            val testFrameworkName = guessTestFrameworkName(creationContext.sourceFile ?: return emptyList())
            if (testFrameworkName != null) {
                val testChatContext = ChatContextItem(
                    JavaScriptContextProvider::class,
                    "\nUse $testFrameworkName JavaScript test framework."
                )

                results.add(testChatContext)
            }
        }

        return results
    }

    /**
     * Retrieves the most popular packages used in a given JavaScript dependencies snapshot.
     *
     * @param snapshot the JavaScript dependencies snapshot to analyze
     * @return a ChatContextItem object representing the context of the most popular packages used in the project,
     *         or null if no popular packages are found
     */
    private fun getMostPopularPackagesContext(snapshot: JsDependenciesSnapshot): ChatContextItem? {
        val dependencies = snapshot.mostPopularFrameworks()

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

    private fun prepareStack(snapshot: JsDependenciesSnapshot): TechStack {
        val devDependencies = mutableMapOf<String, String>()
        val dependencies = mutableMapOf<String, String>()

        val frameworks = mutableMapOf<String, Boolean>()
        val testFrameworks = mutableMapOf<String, Boolean>()

        snapshot.packages.forEach { (name, entry) ->
            entry.dependencyType.let {
                when (it) {
                    PackageJsonDependency.dependencies,
                    PackageJsonDependency.devDependencies -> {
                        if (!name.startsWith("@types/")) {
                            devDependencies[name] = entry.versionRange
                        }
                        JsWebFrameworks.values().forEach { framework ->
                            if (name.startsWith(framework.packageName) || name == framework.packageName) {
                                frameworks[framework.packageName] = true
                            }
                        }

                        JsTestFrameworks.values().forEach { framework ->
                            if (name.startsWith(framework.packageName) || name == framework.packageName) {
                                testFrameworks[framework.packageName] = true
                            }
                        }
                    }

                    else -> {}
                }
            }
        }

        return TechStack(frameworks, testFrameworks, dependencies, devDependencies)
    }
}


