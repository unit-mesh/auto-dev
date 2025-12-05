import groovy.util.Node
import groovy.xml.XmlParser
import org.gradle.api.JavaVersion.VERSION_17
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.IntellijIdeaUltimate
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformDependenciesExtension
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformTestingExtension
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
import org.jetbrains.intellij.platform.gradle.utils.extensionProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*
import kotlin.collections.plus

// The same as `--stacktrace` param
gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("java") // Java support
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.compose") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("org.jetbrains.intellij.platform") version "2.10.2"
    id("org.jetbrains.changelog") version "2.2.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

val basePluginArchiveName = "autodev-jetbrains"

val javaScriptPlugins = listOf("JavaScript")
val pycharmPlugins = listOf(prop("pythonPlugin"))
val javaPlugins = listOf("com.intellij.java", "org.jetbrains.kotlin")

val rustPlugins = listOf(
    prop("rustPlugin"),
    "org.toml.lang"
)

val platformVersion = prop("platformVersion").toInt()
val ideaPlugins = listOf(
    "com.intellij.java",
    "org.jetbrains.plugins.gradle",
    "org.jetbrains.idea.maven",
    "org.jetbrains.kotlin",
    "JavaScript"
)

var lang = extra.properties["lang"] ?: "java"

changelog {
    version.set(properties("pluginVersion"))
    groups.empty()
    path.set(rootProject.file("CHANGELOG.md").toString())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
    itemPrefix.set("*")
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

configure(subprojects) {
    apply {
        plugin("idea")
        plugin("kotlin")
        plugin("org.jetbrains.intellij.platform.module")
    }

    repositories {
        mavenCentral()

        intellijPlatform {
            defaultRepositories()
            jetbrainsRuntime()
        }
    }

    intellijPlatform {
        instrumentCode = false
        buildSearchableOptions = false
    }

    idea {
        module {
            generatedSourceDirs.add(file("src/gen"))
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = VERSION_17
        targetCompatibility = VERSION_17
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    tasks {
        prepareSandbox { enabled = false }
    }

    val testOutput = configurations.create("testOutput")

    if (this.name != "ext-database" && this.name != "ext-terminal" && this.name != "mpp-idea") {
        sourceSets {
            main {
                java.srcDirs("src/gen")
                if (platformVersion == 241 || platformVersion == 243) {
                    resources.srcDirs("src/233/main/resources")
                }
                resources.srcDirs("src/$platformVersion/main/resources")
            }
            test {
                resources.srcDirs("src/$platformVersion/test/resources")
            }
        }
        kotlin {
            sourceSets {
                main {
                    // share 233 code to 241
                    if (platformVersion == 241 || platformVersion == 243) {
                        kotlin.srcDirs("src/233/main/kotlin")
                    }
                    kotlin.srcDirs("src/$platformVersion/main/kotlin")
                }
                test {
                    if (platformVersion == 241 || platformVersion == 243) {
                        kotlin.srcDirs("src/233/test/kotlin")
                    }
                    kotlin.srcDirs("src/$platformVersion/test/kotlin")
                }
            }
        }
    }

    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        implementation("com.knuddels:jtokkit:1.1.0")

        testOutput(sourceSets.test.get().output.classesDirs)

        if (platformVersion == 223) {
            // https://mvnrepository.com/artifact/org.jetbrains/annotations
            implementation("org.jetbrains:annotations:26.0.1")
        }

        testImplementation("junit:junit:4.13.2")
        testImplementation("org.opentest4j:opentest4j:1.3.0")
        testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.9.3")
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.7.0") {
            exclude(group = "net.java.dev.jna", module = "jna-platform")
            exclude(group = "net.java.dev.jna", module = "jna")
        }

        // Note: testFramework is configured in each individual project's dependencies block
        // because it requires IntelliJ Platform IDE to be resolved first
    }
}


project(":") {
    apply {
        plugin("org.jetbrains.changelog")
        plugin("org.jetbrains.intellij.platform")
    }

    repositories {
        intellijPlatform {
            defaultRepositories()
            jetbrainsRuntime()
        }
    }

    intellijPlatform {
        projectName = basePluginArchiveName
        pluginConfiguration {
            id = "cc.unitmesh.devti"
            name = "AutoDev"
            version = prop("pluginVersion")

            ideaVersion {
                sinceBuild = prop("pluginSinceBuild")
                untilBuild = prop("pluginUntilBuild")
            }

            vendor {
                name = "Phodal Huang"
            }
        }

        pluginVerification {
            freeArgs = listOf("-mute", "TemplateWordInPluginId,ForbiddenPluginIdPrefix")
            ides {
                ide(IntellijIdeaUltimate, "2024.1")
                select {
                    types = listOf(IntellijIdeaUltimate)
                    sinceBuild = "241"
                    untilBuild = "241"
                }
            }
        }

        instrumentCode = false
        buildSearchableOptions = false
    }

    dependencies {
        intellijPlatform {
            pluginVerifier()
            intellijIde(prop("ideaRunVersion", prop("ideaVersion")))
            if (hasProp("jbrVersion")) {
                jetbrainsRuntime(prop("jbrVersion"))
            } else {
                jetbrainsRuntime()
            }

            val pluginList: MutableList<String> = mutableListOf("Git4Idea")
            when (lang) {
                "idea" -> {
                    pluginList += javaPlugins
                }

                "python" -> {
                    pluginList += pycharmPlugins
                }

                "go" -> {
                    pluginList += listOf("org.jetbrains.plugins.go")
                }

                "rust" -> {
                    pluginList += rustPlugins
                }
            }
            intellijPlugins(pluginList)

            pluginModule(implementation(project(":mpp-idea-core")))
            pluginModule(implementation(project(":mpp-idea-lang:java")))
            pluginModule(implementation(project(":mpp-idea-lang:kotlin")))
            pluginModule(implementation(project(":mpp-idea-lang:pycharm")))
            pluginModule(implementation(project(":mpp-idea-lang:javascript")))
            pluginModule(implementation(project(":mpp-idea-lang:goland")))
            pluginModule(implementation(project(":mpp-idea-lang:rust")))

            pluginModule(implementation(project(":mpp-idea-exts:ext-database")))
            pluginModule(implementation(project(":mpp-idea-exts:ext-git")))
            pluginModule(implementation(project(":mpp-idea-exts:ext-terminal")))
            pluginModule(implementation(project(":mpp-idea-exts:devins-lang")))

            testFramework(TestFrameworkType.Bundled)
            testFramework(TestFrameworkType.Platform)
        }

        implementation(project(":mpp-idea-core"))
        implementation(project(":mpp-idea-lang:java"))
        implementation(project(":mpp-idea-lang:kotlin"))
        implementation(project(":mpp-idea-lang:pycharm"))
        implementation(project(":mpp-idea-lang:javascript"))
        implementation(project(":mpp-idea-lang:goland"))
        implementation(project(":mpp-idea-lang:rust"))

        implementation(project(":mpp-idea-exts:ext-database"))
        implementation(project(":mpp-idea-exts:ext-git"))
        implementation(project(":mpp-idea-exts:ext-terminal"))
        implementation(project(":mpp-idea-exts:devins-lang"))
        
        // mpp-core dependency for root project source code
        implementation("cc.unitmesh:mpp-core:${prop("mppVersion")}")
    }

    tasks {
        val projectName = project.extensionProvider.flatMap { it.projectName }

        composedJar {
            archiveBaseName.convention(projectName)
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

        patchPluginXml {
            pluginDescription.set(provider { file("../src/description.html").readText() })

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

        buildPlugin {
            archiveBaseName.set(basePluginArchiveName)
            archiveVersion.set(prop("pluginVersion") + "-" + prop("platformVersion"))
        }

        publishPlugin {
            dependsOn("patchChangelog")
            token.set(environment("PUBLISH_TOKEN"))
            channels.set(properties("pluginVersion").map {
                listOf(it.split('-').getOrElse(1) { "default" }.split('.').first())
            })
        }

        intellijPlatformTesting {
            // Generates event scheme for JetBrains Academy plugin FUS events to `build/eventScheme.json`
            runIde.register("buildEventsScheme") {
                task {
                    args(
                        "buildEventsScheme",
                        "--outputFile=${buildDir()}/eventScheme.json",
                        "--pluginId=com.jetbrains.edu"
                    )
                    // Force headless mode to be able to run command on CI
                    systemProperty("java.awt.headless", "true")
                    // BACKCOMPAT: 2024.1. Update value to 242 and this comment
                    // `IDEA_BUILD_NUMBER` variable is used by `buildEventsScheme` task to write `buildNumber` to output json.
                    // It will be used by TeamCity automation to set minimal IDE version for new events
                    environment("IDEA_BUILD_NUMBER", "241")
                }
            }

            runIde.register("runInSplitMode") {
                splitMode = true

                // Specify custom sandbox directory to have a stable path to log file
                sandboxDirectory =
                    intellijPlatform.sandboxContainer.dir("split-mode-sandbox-${prop("platformVersion")}")

                plugins {
                    plugins(ideaPlugins)
                }
            }

            customRunIdeTask(IntellijIdeaUltimate, prop("ideaVersion"), baseTaskName = "Idea")
        }
    }
}

project(":mpp-idea-core") {
    apply {
        plugin("org.jetbrains.kotlin.plugin.serialization")
    }

    dependencies {
        implementation("cc.unitmesh:mpp-core:${prop("mppVersion")}")

        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins)
            bundledPlugin("com.jetbrains.sh")
            testFramework(TestFrameworkType.Bundled)
            testFramework(TestFrameworkType.Platform)
        }

        implementation("org.jetbrains.xodus:xodus-openAPI:2.0.1")
        implementation("org.jetbrains.xodus:xodus-environment:2.0.1")
        implementation("org.jetbrains.xodus:xodus-entity-store:2.0.1")
        implementation("org.jetbrains.xodus:xodus-vfs:2.0.1")

        implementation("io.modelcontextprotocol:kotlin-sdk:0.7.2")
        /// # Ktor
        implementation("io.ktor:ktor-client-cio:3.2.3")
        implementation("io.ktor:ktor-server-sse:3.2.3")

        implementation("io.github.a2asdk:a2a-java-sdk-client:0.3.0.Beta1")

        implementation("io.reactivex.rxjava3:rxjava:3.1.10")

        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
        testImplementation(kotlin("test"))
        testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0") {
            excludeKotlinDeps()
        }
        implementation("com.squareup.okhttp3:okhttp:4.12.0") {
            excludeKotlinDeps()
        }
        implementation("com.squareup.okhttp3:okhttp-sse:4.12.0") {
            excludeKotlinDeps()
        }

        implementation("org.reflections:reflections:0.10.2") {
            exclude(group = "org.slf4j", module = "slf4j-api")
        }

        implementation("com.squareup.retrofit2:converter-jackson:2.11.0")
        implementation("com.squareup.retrofit2:converter-gson:2.11.0")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.1")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")

        implementation("org.commonmark:commonmark:0.21.0")
        implementation("org.commonmark:commonmark-ext-gfm-tables:0.21.0")

        implementation("com.jayway.jsonpath:json-path:2.9.0")
        implementation("com.jsoizo:kotlin-csv-jvm:1.10.0") {
            excludeKotlinDeps()
        }

        // kanban
        implementation("org.kohsuke:github-api:1.326")
        implementation("org.gitlab4j:gitlab4j-api:5.8.0")

        // template engine
        implementation("org.apache.velocity:velocity-engine-core:2.4.1")

        // token count
        implementation("com.knuddels:jtokkit:1.1.0")

        // gitignore parsing library for fallback engine
        implementation("nl.basjes.gitignore:gitignore-reader:1.6.0")

        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
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

project(":mpp-idea-lang:pycharm") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins + pycharmPlugins)
        }

        implementation(project(":mpp-idea-core"))
    }
}


project(":mpp-idea-lang:java") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins)
            testFramework(TestFrameworkType.Plugin.Java)
        }

        implementation(project(":mpp-idea-core"))
        implementation(project(":mpp-idea-exts:devins-lang"))
    }
}


val cssPlugins = listOf(
    "com.intellij.css",
    "org.jetbrains.plugins.sass",
    "org.jetbrains.plugins.less",
)

project(":mpp-idea-lang:javascript") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins)
            intellijPlugins(javaScriptPlugins)
            intellijPlugins(cssPlugins)
            testFramework(TestFrameworkType.Plugin.JavaScript)
        }

        implementation(project(":mpp-idea-core"))
    }
}

project(":mpp-idea-lang:kotlin") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins)
            testFramework(TestFrameworkType.Plugin.Java)
        }

        implementation(project(":mpp-idea-core"))
        implementation(project(":mpp-idea-lang:java"))
    }
}

project(":mpp-idea-lang:rust") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins + rustPlugins)
        }

        implementation(project(":mpp-idea-core"))
    }
}

project(":mpp-idea-lang:goland") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(prop("goPlugin").split(',').map(String::trim).filter(String::isNotEmpty))
        }

        implementation(project(":mpp-idea-core"))
    }
}

project(":mpp-idea-exts:ext-database") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins + "com.intellij.database")
        }

        implementation("cc.unitmesh:mpp-core:${prop("mppVersion")}")
        implementation(project(":mpp-idea-core"))
        implementation(project(":mpp-idea-exts:devins-lang"))
    }

    sourceSets {
        main {
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
}

project(":mpp-idea-exts:ext-git") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins + "Git4Idea")
        }

        implementation("cc.unitmesh:mpp-core:${prop("mppVersion")}")
        implementation(project(":mpp-idea-core"))
        implementation(project(":mpp-idea-exts:devins-lang"))

        // kanban
        implementation("org.kohsuke:github-api:1.326")
        implementation("org.gitlab4j:gitlab4j-api:5.8.0")

        implementation("cc.unitmesh:git-commit-message:0.4.6") {
            excludeKotlinDeps()
        }
    }
}

project(":mpp-idea-exts:ext-terminal") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins + "org.jetbrains.plugins.terminal")
        }

        implementation(project(":mpp-idea-core"))
    }

    sourceSets {
        main {
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
}

project(":mpp-idea-exts:devins-lang") {
    apply {
        plugin("org.jetbrains.grammarkit")
        plugin("org.jetbrains.kotlin.plugin.serialization")
    }

    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins + "org.intellij.plugins.markdown" + "com.jetbrains.sh" + "Git4Idea")

            testFramework(TestFrameworkType.Plugin.Java)
        }

        implementation("com.jayway.jsonpath:json-path:2.9.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
        implementation("cc.unitmesh:mpp-core:${prop("mppVersion")}")
        implementation(project(":mpp-idea-core"))
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

tasks.test {
    useJUnitPlatform()
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

data class TypeWithVersion(val type: IntelliJPlatformType, val version: String)

fun String.toTypeWithVersion(): TypeWithVersion {
    val (code, version) = split("-", limit = 2)
    return TypeWithVersion(IntelliJPlatformType.fromCode(code), version)
}

/**
 * Creates `run$[baseTaskName]` Gradle task to run IDE of given [type]
 * via `runIde` task with plugins according to [ideToPlugins] map
 */
fun IntelliJPlatformTestingExtension.customRunIdeTask(
    type: IntelliJPlatformType,
    versionWithCode: String? = null,
    baseTaskName: String = type.name,
) {
    runIde.register("run$baseTaskName") {
        useInstaller = false

        if (versionWithCode != null) {
            val version = versionWithCode.toTypeWithVersion().version

            this.type = type
            this.version = version
        } else {
            val pathProperty = baseTaskName.replaceFirstChar { it.lowercaseChar() } + "Path"
            if (hasProp(pathProperty)) {
                localPath.convention(layout.dir(provider { file(prop(pathProperty)) }))
            } else {
                task {
                    doFirst {
                        throw GradleException("Property `$pathProperty` is not defined in gradle.properties")
                    }
                }
            }
        }

        // Specify custom sandbox directory to have a stable path to log file
        sandboxDirectory =
            intellijPlatform.sandboxContainer.dir("${baseTaskName.lowercase()}-sandbox-${prop("platformVersion")}")

        plugins {
            plugins(ideaPlugins)
        }
    }
}

fun IntelliJPlatformDependenciesExtension.intellijIde(versionWithCode: String) {
    val (type, version) = versionWithCode.toTypeWithVersion()
    create(type, version, useInstaller = false)
}

fun IntelliJPlatformDependenciesExtension.intellijPlugins(vararg notations: String) {
    for (notation in notations) {
        if (notation.contains(":")) {
            plugin(notation)
        } else {
            bundledPlugin(notation)
        }
    }
}

fun IntelliJPlatformDependenciesExtension.intellijPlugins(notations: List<String>) {
    intellijPlugins(*notations.toTypedArray())
}

fun hasProp(name: String): Boolean = extra.has(name)

fun prop(name: String, fallbackResult: String? = null): String {
    // Try local properties first
    val localProp = extra.properties[name] as? String
    if (localProp != null) return localProp
    
    // Try parent project properties
    val parentProp = rootProject.parent?.extra?.properties?.get(name) as? String
    if (parentProp != null) return parentProp
    
    // Try gradle.properties from parent
    val parentGradleProp = rootProject.parent?.findProperty(name) as? String
    if (parentGradleProp != null) return parentGradleProp
    
    return fallbackResult ?: error("Property `$name` is not defined in gradle.properties")
}

fun withProp(name: String, action: (String) -> Unit) {
    if (hasProp(name)) {
        action(prop(name))
    }
}

fun buildDir(): String {
    return project.layout.buildDirectory.get().asFile.absolutePath
}

fun <T : ModuleDependency> T.excludeKotlinDeps() {
    exclude(module = "kotlin-runtime")
    exclude(module = "kotlin-reflect")
    exclude(module = "kotlin-stdlib")
    exclude(module = "kotlin-stdlib-common")
    exclude(module = "kotlin-stdlib-jdk8")
}
