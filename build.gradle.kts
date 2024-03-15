// The MIT License (MIT)
//
//Copyright (c) 2015 Aleksey Kladov, Evgeny Kurbatsky, Alexey Kudinkin and contributors
//Copyright (c) 2016 JetBrains
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

import groovy.xml.XmlParser
import org.gradle.api.JavaVersion.VERSION_17
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PublishPluginTask
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// The same as `--stacktrace` param
gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("java") // Java support
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
    alias(libs.plugins.serialization)
    alias(libs.plugins.gradleIntelliJPlugin)

    id("org.jetbrains.grammarkit") version "2022.3.2.2"

    kotlin("jvm") version "1.8.22"
    id("net.saliman.properties") version "1.5.2"
}

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

fun prop(name: String): String =
    extra.properties[name] as? String
        ?: error("Property `$name` is not defined in gradle.properties")

val basePluginArchiveName = "intellij-autodev"

val javaScriptPlugins = listOf("JavaScript")
val pycharmPlugins = listOf("PythonCore")
val javaPlugins = listOf("com.intellij.java", "org.jetbrains.kotlin")
//val kotlinPlugins = listOf("org.jetbrains.kotlin")
val clionVersion = prop("clionVersion")

// https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html#modules-specific-to-functionality
val clionPlugins = listOf(
    "com.intellij.cidr.base",
    "com.intellij.cidr.lang",
    "com.intellij.clion",
    prop("rustPlugin"),
    "org.toml.lang"
)
val cppPlugins = listOf(
    "com.intellij.cidr.lang",
    "com.intellij.clion",
    "com.intellij.cidr.base",
//    "com.intellij.nativeDebug",
    "org.jetbrains.plugins.clion.test.google",
    "org.jetbrains.plugins.clion.test.catch"
)
val rustPlugins = listOf(
    prop("rustPlugin"),
    "org.toml.lang"
)

val riderVersion = prop("riderVersion")
val riderPlugins: List<String> = listOf(
    "rider-plugins-appender",
    "org.intellij.intelliLang",
)
val scalaPlugin = prop("scalaPlugin")

val pluginProjects: List<Project> get() = rootProject.allprojects.toList()
val ideaPlugins =
    listOf(
        "Git4Idea",
        "com.intellij.java",
        "org.jetbrains.plugins.gradle",
        "org.jetbrains.kotlin",
        "JavaScript"
    )

var baseIDE = prop("baseIDE")
val platformVersion = prop("platformVersion").toInt()
val ideaVersion = prop("ideaVersion")
val golandVersion = prop("golandVersion")
val pycharmVersion = prop("pycharmVersion")
val webstormVersion = prop("webstormVersion")

var lang = extra.properties["lang"] ?: "java"

val baseVersion = when (baseIDE) {
    "idea" -> ideaVersion
    "pycharm" -> pycharmVersion
    "goland" -> golandVersion
    "clion" -> clionVersion
    "rider" -> riderVersion
    "javascript" -> webstormVersion
    else -> error("Unexpected IDE name: `$baseIDE`")
}

repositories {
    mavenCentral()
}

allprojects {
    apply {
        plugin("idea")
        plugin("kotlin")
        plugin("org.jetbrains.intellij")
        plugin("org.jetbrains.kotlinx.kover")
    }

    repositories {
        mavenCentral()
    }

    idea {
        module {
            generatedSourceDirs.add(file("src/gen"))
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

    intellij {
        version.set(baseVersion)
        updateSinceUntilBuild.set(true)
        instrumentCode.set(false)
        sandboxDir.set("$buildDir/$baseIDE-sandbox-$platformVersion")
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = VERSION_17
        targetCompatibility = VERSION_17
    }

    tasks {
        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = VERSION_17.toString()
                languageVersion = "1.8"
                // see https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
                apiVersion = "1.7"
                freeCompilerArgs = listOf("-Xjvm-default=all")
            }
        }

        withType<PatchPluginXmlTask> {
            sinceBuild.set(prop("pluginSinceBuild"))
            untilBuild.set(prop("pluginUntilBuild"))
        }

        // All these tasks don't make sense for non-root subprojects
        // Root project (i.e. `:plugin`) enables them itself if needed
        runIde { enabled = false }
        prepareSandbox { enabled = false }
        buildSearchableOptions { enabled = false }
    }

    val testOutput = configurations.create("testOutput")


    sourceSets {
        main {
            java.srcDirs("src/gen")
            resources.srcDirs("src/$platformVersion/main/resources")
        }
        test {
            resources.srcDirs("src/$platformVersion/test/resources")
        }
    }
    kotlin {
        sourceSets {
            main {
                kotlin.srcDirs("src/$platformVersion/main/kotlin")
            }
            test {
                kotlin.srcDirs("src/$platformVersion/test/kotlin")
            }
        }
    }

    dependencies {
        compileOnly(kotlin("stdlib-jdk8"))
        testOutput(sourceSets.getByName("test").output.classesDirs)
    }
}

changelog {
    version.set(properties("pluginVersion"))
    groups.empty()
    path.set(rootProject.file("CHANGELOG.md").toString())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

project(":plugin") {
    apply {
        plugin("org.jetbrains.changelog")
    }

    version = prop("pluginVersion") + "-$platformVersion"

    intellij {
        pluginName.set(basePluginArchiveName)
        val pluginList: MutableList<String> = mutableListOf("Git4Idea")
        when (lang) {
            "idea" -> {
                pluginList += javaPlugins
            }

            "scala" -> {
                pluginList += javaPlugins + scalaPlugin
            }

            "python" -> {
                pluginList += pycharmPlugins
            }

            "go" -> {
                pluginList += listOf("org.jetbrains.plugins.go")
            }

            "cpp" -> {
                pluginList += clionPlugins
            }

            "rust" -> {
                pluginList += rustPlugins
            }
        }

        plugins.set(pluginList)
    }

    dependencies {
        implementation(project(":"))
        implementation(project(":java"))
        implementation(project(":kotlin"))
        implementation(project(":pycharm"))
        implementation(project(":javascript"))
        implementation(project(":goland"))
        implementation(project(":rust"))
        implementation(project(":cpp"))
        implementation(project(":scala"))
        implementation(project(":exts:database"))
        implementation(project(":exts:ext-android"))
        implementation(project(":exts:ext-harmonyos"))
        implementation(project(":exts:devin-lang"))
    }

    // Collects all jars produced by compilation of project modules and merges them into singe one.
    // We need to put all plugin manifest files into single jar to make new plugin model work
    val mergePluginJarTask = task<Jar>("mergePluginJars") {
        duplicatesStrategy = DuplicatesStrategy.FAIL
        archiveBaseName.set(basePluginArchiveName)

        exclude("META-INF/MANIFEST.MF")
        exclude("**/classpath.index")

        val pluginLibDir by lazy {
            val sandboxTask = tasks.prepareSandbox.get()
            sandboxTask.destinationDir.resolve("${sandboxTask.pluginName.get()}/lib")
        }

        val pluginJars by lazy {
            pluginLibDir.listFiles().orEmpty().filter {
                it.isPluginJar()
            }
        }

        destinationDirectory.set(project.layout.dir(provider { pluginLibDir }))

        doFirst {
            for (file in pluginJars) {
                from(zipTree(file))
            }
        }

        doLast {
            delete(pluginJars)
        }
    }

    // Add plugin sources to the plugin ZIP.
    // gradle-intellij-plugin will use it as a plugin sources if the plugin is used as a dependency
    val createSourceJar = task<Jar>("createSourceJar") {
        for (prj in pluginProjects) {
            from(prj.kotlin.sourceSets.main.get().kotlin) {
                include("**/*.java")
                include("**/*.kt")
            }
        }

        destinationDirectory.set(layout.buildDirectory.dir("libs"))
        archiveBaseName.set(basePluginArchiveName)
        archiveClassifier.set("src")
    }

    tasks {
        buildPlugin {
            dependsOn(createSourceJar)
            from(createSourceJar) { into("lib/src") }
            // Set proper name for final plugin zip.
            // Otherwise, base name is the same as gradle module name
            archiveBaseName.set(basePluginArchiveName)
        }

        runIde { enabled = true }

        prepareSandbox {
            finalizedBy(mergePluginJarTask)
            enabled = true
        }

        buildSearchableOptions {
            // Force `mergePluginJarTask` be executed before `buildSearchableOptions`
            // Otherwise, `buildSearchableOptions` task can't load the plugin and searchable options are not built.
            // Should be dropped when jar merging is implemented in `gradle-intellij-plugin` itself
            dependsOn(mergePluginJarTask)
            enabled = false
        }

        withType<RunIdeTask> {
            // Default args for IDEA installation
            jvmArgs("-Xmx768m", "-XX:+UseG1GC", "-XX:SoftRefLRUPolicyMSPerMB=50")
            // Disable plugin auto reloading. See `com.intellij.ide.plugins.DynamicPluginVfsListener`
            jvmArgs("-Didea.auto.reload.plugins=false")
            // Don't show "Tip of the Day" at startup
            jvmArgs("-Dide.show.tips.on.startup.default.value=false")
            // uncomment if `unexpected exception ProcessCanceledException` prevents you from debugging a running IDE
            // jvmArgs("-Didea.ProcessCanceledException=disabled")
        }

        withType<PatchPluginXmlTask> {
            pluginDescription.set(provider { file("description.html").readText() })

            changelog {
                version.set(properties("pluginVersion"))
                groups.empty()
                path.set(rootProject.file("CHANGELOG.md").toString())
                repositoryUrl.set(properties("pluginRepositoryUrl"))
            }

            val changelog = project.changelog
            // Get the latest available change notes from the changelog file
            changeNotes.set(properties("pluginVersion").map { pluginVersion ->
                with(changelog) {
                    renderItem(
                        (getOrNull(pluginVersion) ?: getUnreleased())
                            .withHeader(false)
                            .withEmptySections(false),

                        Changelog.OutputType.HTML,
                    )
                }
            })
        }

        withType<PublishPluginTask> {
            dependsOn("patchChangelog")
            token.set(environment("PUBLISH_TOKEN"))
            channels.set(properties("pluginVersion").map {
                listOf(it.split('-').getOrElse(1) { "default" }.split('.').first())
            })
        }
    }
}

project(":") {
    intellij {
        version.set(ideaVersion)
        plugins.set(ideaPlugins)
    }

    dependencies {
        implementation(libs.bundles.openai)
        implementation(libs.bundles.markdown)
        implementation(libs.yaml)

        implementation(libs.json.pathkt)

        implementation("org.jetbrains:markdown:0.6.1")
        implementation(libs.kotlinx.serialization.json)

        implementation("cc.unitmesh:cocoa-core:0.4.5")
        implementation("cc.unitmesh:git-commit-message:0.4.5")

        // kanban
        implementation(libs.github.api)
        implementation("org.gitlab4j:gitlab4j-api:5.3.0")

        // template engine
        implementation("org.apache.velocity:velocity-engine-core:2.3")

        // http request/response
        implementation(libs.jackson.module.kotlin)

        // token count
        implementation("com.knuddels:jtokkit:1.0.0")

        // junit
        testImplementation("io.kotest:kotest-assertions-core:5.7.2")
        testImplementation("junit:junit:4.13.2")
        testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.9.3")

        kover(project(":"))
        kover(project(":cpp"))
        kover(project(":csharp"))
        kover(project(":goland"))
        kover(project(":java"))
        kover(project(":javascript"))
        kover(project(":kotlin"))
        kover(project(":pycharm"))
        kover(project(":rust"))
        kover(project(":scala"))

        kover(project(":exts:database"))
        kover(project(":exts:ext-android"))
        kover(project(":exts:devin-lang"))
    }

    task("resolveDependencies") {
        doLast {
            rootProject.allprojects
                .map { it.configurations }
                .flatMap { it.filter { c -> c.isCanBeResolved } }
                .forEach { it.resolve() }
        }
    }
}

project(":pycharm") {
    intellij {
        version.set(pycharmVersion)
        plugins.set(pycharmPlugins)
    }
    dependencies {
        implementation(project(":"))
    }
}


project(":java") {
    intellij {
        version.set(ideaVersion)
        plugins.set(ideaPlugins)
    }
    dependencies {
        implementation(project(":"))
    }
}

project(":javascript") {
    intellij {
        version.set(ideaVersion)
        plugins.set(javaScriptPlugins)
    }
    dependencies {
        implementation(project(":"))
    }
}

project(":kotlin") {
    intellij {
        version.set(ideaVersion)
        plugins.set(ideaPlugins)
    }
    dependencies {
        implementation(project(":"))
        implementation(project(":java"))
    }
}

project(":scala") {
    intellij {
        version.set(ideaVersion)
        plugins.set(ideaPlugins + scalaPlugin)
    }
    dependencies {
        implementation(project(":"))
        implementation(project(":java"))
    }
}

project(":rust") {
    intellij {
        version.set(ideaVersion)
        plugins.set(rustPlugins)

        sameSinceUntilBuild.set(true)
        updateSinceUntilBuild.set(false)
    }
    dependencies {
        implementation(project(":"))
    }
}

project(":cpp") {
    intellij {
        version.set(clionVersion)
        plugins.set(cppPlugins)
    }
    dependencies {
        implementation(project(":"))
    }
}

project(":csharp") {
    intellij {
        version.set(riderVersion)
        type.set("RD")
        plugins.set(riderPlugins)
    }
    dependencies {
        implementation(project(":"))
    }
}

project(":goland") {
    intellij {
        version.set(ideaVersion)
//        type.set("IU")
        updateSinceUntilBuild.set(false)
        // required if Go language API is needed:
        plugins.set(prop("goPlugin").split(',').map(String::trim).filter(String::isNotEmpty))
    }
    dependencies {
        implementation(project(":"))
    }
}

project(":exts:database") {
    intellij {
        version.set(ideaVersion)
        plugins.set(ideaPlugins + "com.intellij.database")
    }
    dependencies {
        implementation(project(":"))
    }
}

project(":exts:ext-android") {
    intellij {
        version.set(ideaVersion)
        type.set("AI")
        plugins.set((ideaPlugins + prop("androidPlugin").ifBlank { "" }).filter(String::isNotEmpty))
    }

    dependencies {
        implementation(project(":"))
    }
}

project(":exts:ext-harmonyos") {
    intellij {
        version.set(ideaVersion)
        type.set("AI")
        plugins.set((ideaPlugins + prop("androidPlugin").ifBlank { "" }).filter(String::isNotEmpty))
    }

    dependencies {
        implementation(project(":"))
    }
}

project(":exts:devin-lang") {
    apply {
        plugin("org.jetbrains.grammarkit")
    }

    intellij {
        version.set(ideaVersion)
        plugins.set((ideaPlugins + "org.intellij.plugins.markdown"))
    }

    dependencies {
        implementation(project(":"))
    }

    tasks {
        generateLexer {
            sourceFile.set(file("src/grammar/DevInLexer.flex"))
            targetOutputDir.set(file("src/gen/cc/unitmesh/devti/language/lexer"))
            purgeOldFiles.set(true)
        }

        generateParser {
            sourceFile.set(file("src/grammar/DevInParser.bnf"))
            targetRootOutputDir.set(file("src/gen"))
            pathToParser.set("cc/unitmesh/devti/language/parser/DevInParser.java")
            pathToPsiRoot.set("cc/unitmesh/devti/language/psi")
            purgeOldFiles.set(true)
        }

        withType<KotlinCompile> {
            dependsOn(generateLexer, generateParser)
        }
    }
}


fun File.isPluginJar(): Boolean {
    if (!isFile) return false
    if (extension != "jar") return false
    return zipTree(this).files.any { it.isManifestFile() }
}

fun File.isManifestFile(): Boolean {
    if (extension != "xml") return false
    val rootNode = try {
        val parser = XmlParser()
        parser.parse(this)
    } catch (e: Exception) {
        logger.error("Failed to parse $path", e)
        return false
    }
    return rootNode.name() == "idea-plugin"
}


