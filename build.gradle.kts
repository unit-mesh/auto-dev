import groovy.xml.XmlParser
import org.gradle.api.JavaVersion.VERSION_17
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PublishPluginTask
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java") // Java support
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
    alias(libs.plugins.serialization)

    kotlin("jvm") version "1.8.22"
    id("org.jetbrains.intellij") version "1.13.1"
    id("net.saliman.properties") version "1.5.2"
}

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

fun prop(name: String): String =
    extra.properties[name] as? String
        ?: error("Property `$name` is not defined in gradle.properties")

val basePluginArchiveName = "intellij-autodev"

val pycharmPlugins: List<String> = listOf()
val ideaPlugins = listOf("com.intellij.java", "org.jetbrains.plugins.gradle")

val pluginProjects: List<Project> get() = rootProject.allprojects.toList()

val javaPlugin = "com.intellij.java"
val baseIDE = prop("baseIDE")
val platformVersion = prop("globalPlatformVersion").toInt()
val ideaVersion = prop("ideaVersion")
val pycharmVersion = prop("pycharmVersion")

val baseVersion = when (baseIDE) {
    "idea" -> ideaVersion
    "pycharm" -> pycharmVersion
    else -> error("Unexpected IDE name: `$baseIDE`")
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {

}

allprojects {
    apply {
        plugin("idea")
        plugin("kotlin")
        plugin("org.jetbrains.intellij")
    }

    repositories {
        mavenCentral()
        maven("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    }

    idea {
        module {
            generatedSourceDirs.add(file("src/gen"))
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

    dependencies {
        compileOnly(kotlin("stdlib-jdk8"))
        testOutput(sourceSets.getByName("test").output.classesDirs)
    }
}

project(":plugin") {
    apply {
        plugin("org.jetbrains.changelog")
    }

    version = prop("pluginVersion")
    intellij {
        pluginName.set("autodev")
        val pluginList: MutableList<String> = mutableListOf()
        if (baseIDE == "idea") {
            pluginList += listOf(
                javaPlugin,
            )
        }
        plugins.set(pluginList)
    }

    dependencies {
        implementation(project(":"))
        implementation(project(":idea"))
        implementation(project(":pycharm"))
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
//    val createSourceJar = task<Jar>("createSourceJar") {
//        for (prj in pluginProjects) {
//            from(prj.kotlin.sourceSets.main.get().kotlin) {
//                include("**/*.java")
//                include("**/*.kt")
//            }
//        }
//
//        destinationDirectory.set(layout.buildDirectory.dir("libs"))
//        archiveBaseName.set(basePluginArchiveName)
//        archiveClassifier.set("src")
//    }

    tasks {
        buildPlugin {
//            dependsOn(createSourceJar)
//            from(createSourceJar) { into("lib/src") }
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
//            enabled = prop("enableBuildSearchableOptions").toBoolean()
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

            // Uncomment to enable FUS testing mode
            // jvmArgs("-Dfus.internal.test.mode=true")

            // Uncomment to enable localization testing mode
            // jvmArgs("-Didea.l10n=true")
        }

//        patchPluginXml {
//            version = properties("pluginVersion")
//            sinceBuild = properties("pluginSinceBuild")
//            untilBuild = properties("pluginUntilBuild")
//
//            // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
//            pluginDescription = properties("pluginDescription")
//
//            val changelog = project.changelog // local variable for configuration cache compatibility
//            // Get the latest available change notes from the changelog file
//            changeNotes = properties("pluginVersion").map { pluginVersion ->
//                with(changelog) {
//                    renderItem(
//                        (getOrNull(pluginVersion) ?: getUnreleased())
//                            .withHeader(false)
//                            .withEmptySections(false),
//                        Changelog.OutputType.HTML,
//                    )
//                }
//            }
//        }

        // Configure UI tests plugin
        // Read more: https://github.com/JetBrains/intellij-ui-test-robot
//        runIdeForUiTests {
//            systemProperty("robot-server.port", "8082")
//            systemProperty("ide.mac.message.dialogs.as.sheets", "false")
//            systemProperty("jb.privacy.policy.text", "<!--999.999-->")
//            systemProperty("jb.consents.confirmation.enabled", "false")
//        }

        signPlugin {
            certificateChain = environment("CERTIFICATE_CHAIN")
            privateKey = environment("PRIVATE_KEY")
            password = environment("PRIVATE_KEY_PASSWORD")
        }

        publishPlugin {
            dependsOn("patchChangelog")
            token = environment("PUBLISH_TOKEN")
            // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
            // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
            // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
            channels =
                properties("pluginVersion").map { listOf(it.split('-').getOrElse(1) { "default" }.split('.').first()) }
        }
    }
}

project(":") {
    dependencies {
        implementation(libs.github.api)
        implementation(libs.dotenv)

        implementation(libs.bundles.openai)
        implementation(libs.bundles.markdown)

        implementation(libs.kotlinx.serialization.json)
        // jackson-module-kotlin
        implementation(libs.jackson.module.kotlin)

        implementation(libs.comate.spec.lang)

        implementation("com.knuddels:jtokkit:0.6.1")

        // junit
        testImplementation("junit:junit:4.13.2")
        testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.9.3")
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

project(":idea") {
    intellij {
        version.set(ideaVersion)
        plugins.set(ideaPlugins)
    }
    dependencies {
        implementation(project(":"))
    }
}

//
//// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
//intellij {
//    pluginName = properties("pluginName")
//    version = properties("platformVersion")
//    type = properties("platformType")
//
//    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
//    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
//}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
//changelog {
//    groups.empty()
//    repositoryUrl = properties("pluginRepositoryUrl")
//}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
//qodana {
//    cachePath = provider { file(".qodana").canonicalPath }
//    reportPath = provider { file("build/reports/inspections").canonicalPath }
//    saveReport = true
//    showReport = environment("QODANA_SHOW_REPORT").map { it.toBoolean() }.getOrElse(false)
//}

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


