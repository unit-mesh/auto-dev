import groovy.util.Node
import groovy.xml.XmlParser
import org.gradle.api.JavaVersion.VERSION_17
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformDependenciesExtension
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

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

val basePluginArchiveName = "autodev-jetbrains"

val javaScriptPlugins = listOf("JavaScript")
val pycharmPlugins = listOf(prop("pythonPlugin"))
val javaPlugins = listOf("com.intellij.java", "org.jetbrains.kotlin")
val clionVersion = prop("clionVersion")

// https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html#modules-specific-to-functionality
val clionPlugins = listOf(
    "com.intellij.cidr.base",
    "com.intellij.cidr.lang",
    "com.intellij.clion",
    prop("rustPlugin"),
    "org.toml.lang"
)
var cppPlugins: List<String> = listOf(
    "com.intellij.cidr.lang",
    "com.intellij.clion",
    "com.intellij.cidr.base",
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
        "com.intellij.java",
        "org.jetbrains.plugins.gradle",
        "org.jetbrains.idea.maven",
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

changelog {
    version.set(properties("pluginVersion"))
    groups.empty()
    path.set(rootProject.file("CHANGELOG.md").toString())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

repositories {
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

allprojects {
    apply {
        plugin("idea")
        plugin("kotlin")
        plugin("org.jetbrains.kotlinx.kover")
        plugin("org.jetbrains.intellij.platform.module")
    }

    repositories {
        mavenCentral()

        intellijPlatform {
            defaultRepositories()
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

        prepareSandbox { enabled = false }
    }

    val testOutput = configurations.create("testOutput")

    if (this.name != "ext-terminal") {
        sourceSets {
            main {
                java.srcDirs("src/gen")
                if (platformVersion == 241) {
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
                    if (platformVersion == 241) {
                        kotlin.srcDirs("src/233/main/kotlin")
                    }
                    kotlin.srcDirs("src/$platformVersion/main/kotlin")
                }
                test {
                    kotlin.srcDirs("src/$platformVersion/test/kotlin")
                }
            }
        }
    }

    dependencies {
        compileOnly(kotlin("stdlib-jdk8"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

        testOutput(sourceSets.getByName("test").output.classesDirs)
    }
}

project(":") {
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
                select {
                    sinceBuild = "242"
                    untilBuild = "243"
                }
            }
        }

        instrumentCode = false
        buildSearchableOptions = false
    }

    dependencies {
        intellijPlatform {
            pluginVerifier()
            intellijIde(prop("ideaVersion"))
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
            intellijPlugins(pluginList)

            pluginModule(implementation(project(":java")))
            pluginModule(implementation(project(":kotlin")))
            pluginModule(implementation(project(":pycharm")))
            pluginModule(implementation(project(":javascript")))
            pluginModule(implementation(project(":goland")))
            pluginModule(implementation(project(":rust")))
            pluginModule(implementation(project(":cpp")))
            pluginModule(implementation(project(":scala")))
            pluginModule(implementation(project(":local-bundle")))
            pluginModule(implementation(project(":exts:ext-database")))
            pluginModule(implementation(project(":exts:ext-android")))
            pluginModule(implementation(project(":exts:ext-harmonyos")))
            pluginModule(implementation(project(":exts:ext-git")))
            pluginModule(implementation(project(":exts:ext-http-client")))
            pluginModule(implementation(project(":exts:ext-terminal")))
            pluginModule(implementation(project(":exts:devins-lang")))

            testFramework(TestFrameworkType.Bundled)
        }

        implementation(libs.bundles.openai)
        implementation(libs.bundles.markdown)
        implementation(libs.yaml)

        implementation(libs.json.pathkt)

        implementation("org.jetbrains:markdown:0.6.1")
        implementation(libs.kotlinx.serialization.json)

        // chocolate factory
        // follow: https://onnxruntime.ai/docs/get-started/with-java.html
//        implementation("com.microsoft.onnxruntime:onnxruntime:1.18.0")
//        implementation("ai.djl.huggingface:tokenizers:0.29.0")

        implementation("cc.unitmesh:cocoa-core:1.0.0")
        implementation("cc.unitmesh:document:1.0.0")

        // kanban
        implementation(libs.github.api)
        implementation("org.gitlab4j:gitlab4j-api:5.3.0")

        // template engine
        implementation("org.apache.velocity:velocity-engine-core:2.3")

        // http request/response
        implementation(libs.jackson.module.kotlin)

        // token count
        implementation("com.knuddels:jtokkit:1.0.0")

        implementation("org.apache.commons:commons-text:1.12.0")

        // junit
        testImplementation("io.kotest:kotest-assertions-core:5.7.2")
        testImplementation("junit:junit:4.13.2")
        testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.9.3")

        kover(project(":cpp"))
//        kover(project(":csharp"))
        kover(project(":goland"))
        kover(project(":java"))
        kover(project(":javascript"))
        kover(project(":kotlin"))
        kover(project(":pycharm"))
        kover(project(":rust"))
        kover(project(":scala"))

        kover(project(":exts:ext-database"))
        kover(project(":exts:ext-android"))
        kover(project(":exts:devins-lang"))
    }

    task("resolveDependencies") {
        doLast {
            rootProject.allprojects
                .map { it.configurations }
                .flatMap { it.filter { c -> c.isCanBeResolved } }
                .forEach { it.resolve() }
        }
    }

    tasks {
        buildPlugin {
            val newName = basePluginArchiveName + "-" + properties("pluginVersion").get()
            archiveBaseName.set(newName)
        }

        runIde { enabled = true }

        buildSearchableOptions {
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

        patchPluginXml {
            pluginDescription.set(provider { file("src/description.html").readText() })

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

        publishPlugin {
            dependsOn("patchChangelog")
            token.set(environment("PUBLISH_TOKEN"))
            channels.set(properties("pluginVersion").map {
                listOf(it.split('-').getOrElse(1) { "default" }.split('.').first())
            })
        }
    }
}

project(":core") {
    repositories {
        intellijPlatform {
            defaultRepositories()
            jetbrainsRuntime()
        }
    }

    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins)
            testFramework(TestFrameworkType.Bundled)
        }

        implementation("com.theokanning.openai-gpt3-java:service:0.18.2")
        implementation("com.squareup.retrofit2:converter-jackson:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")

        implementation("org.commonmark:commonmark:0.21.0")
        implementation("org.commonmark:commonmark-ext-gfm-tables:0.21.0")

        implementation("org.yaml:snakeyaml:2.2")

        implementation("com.nfeld.jsonpathkt:jsonpathkt:2.0.1")

        implementation("org.jetbrains:markdown:0.6.1")
//        implementation(libs.kotlinx.serialization.json)

        // chocolate factory
        // follow: https://onnxruntime.ai/docs/get-started/with-java.html
//        implementation("com.microsoft.onnxruntime:onnxruntime:1.18.0")
//        implementation("ai.djl.huggingface:tokenizers:0.29.0")

        implementation("cc.unitmesh:cocoa-core:1.0.0")
        implementation("cc.unitmesh:document:1.0.0")

        // kanban
        implementation("org.kohsuke:github-api:1.314")
        implementation("org.gitlab4j:gitlab4j-api:5.3.0")

        // template engine
        implementation("org.apache.velocity:velocity-engine-core:2.3")

        // http request/response
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")

        // token count
        implementation("com.knuddels:jtokkit:1.0.0")

        implementation("org.apache.commons:commons-text:1.12.0")

        // junit
        testImplementation("io.kotest:kotest-assertions-core:5.7.2")
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
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins + pycharmPlugins)
        }

        implementation(project(":core"))
    }
}


project(":java") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins)
        }

        implementation(project(":core"))
    }
}

project(":javascript") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins)
            intellijPlugins(javaScriptPlugins)
        }

        implementation(project(":core"))
    }
}

project(":kotlin") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins)
        }

        implementation(project(":core"))
        implementation(project(":java"))
    }
}

project(":scala") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins + scalaPlugin)
        }

        implementation(project(":core"))
        implementation(project(":java"))
    }
}

project(":rust") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(rustPlugins)
        }

        implementation(project(":core"))
    }
}

project(":cpp") {
    if (platformVersion == 233 || platformVersion == 241) {
        cppPlugins += "com.intellij.nativeDebug"
    }

    dependencies {
        intellijPlatform {
            intellijIde(clionVersion)
            intellijPlugins(cppPlugins)
        }

        implementation(project(":core"))
    }
}

//project(":csharp") {
//    dependencies {
//        intellijPlatform {
//            intellijIde(riderVersion)
//            intellijPlugins(riderPlugins)
//        }
//
//        implementation(project(":core"))
//    }
//}

project(":goland") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(prop("goPlugin").split(',').map(String::trim).filter(String::isNotEmpty))
        }

        implementation(project(":core"))
    }
}

project(":exts:ext-database") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins + "com.intellij.database")
        }
        implementation(project(":core"))
    }
}

project(":exts:ext-android") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins((ideaPlugins + prop("androidPlugin").ifBlank { "" }).filter(String::isNotEmpty))
        }

        implementation(project(":core"))
    }
}

project(":exts:ext-harmonyos") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins((ideaPlugins + prop("androidPlugin").ifBlank { "" }).filter(String::isNotEmpty))
        }


        implementation(project(":core"))
    }
}

project(":exts:ext-git") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins + "Git4Idea")
        }

        implementation(project(":core"))
        implementation("cc.unitmesh:git-commit-message:0.4.6")
    }
}

project(":exts:ext-http-client") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins + "com.jetbrains.restClient")
        }

        implementation(project(":core"))
    }
}

project(":local-bundle") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
        }

        implementation(project(":core"))
    }
}

project(":exts:ext-terminal") {
    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins + "org.jetbrains.plugins.terminal")
        }

        implementation(project(":core"))
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

project(":exts:devins-lang") {
    apply {
        plugin("org.jetbrains.grammarkit")
    }

    dependencies {
        intellijPlatform {
            intellijIde(prop("ideaVersion"))
            intellijPlugins(ideaPlugins + "org.intellij.plugins.markdown" + "com.jetbrains.sh" + "Git4Idea")
        }

        implementation(project(":core"))
        implementation(project(":exts:ext-git"))
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

data class TypeWithVersion(val type: IntelliJPlatformType, val version: String)

fun String.toTypeWithVersion(): TypeWithVersion {
    val (code, version) = split("-", limit = 2)
    return TypeWithVersion(IntelliJPlatformType.fromCode(code), version)
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

fun prop(name: String): String =
    extra.properties[name] as? String ?: error("Property `$name` is not defined in gradle.properties")

fun withProp(name: String, action: (String) -> Unit) {
    if (hasProp(name)) {
        action(prop(name))
    }
}

fun withProp(filePath: String, name: String, action: (String) -> Unit) {
    if (!file(filePath).exists()) {
        println("$filePath doesn't exist")
        return
    }
    val properties = loadProperties(filePath)
    val value = properties.getProperty(name) ?: return
    action(value)
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

fun loadProperties(path: String): Properties {
    val properties = Properties()
    file(path).bufferedReader().use { properties.load(it) }
    return properties
}

fun parseManifest(file: File): Node {
    val node = XmlParser().parse(file)
    check(node.name() == "idea-plugin") {
        "Manifest file `$file` doesn't contain top-level `idea-plugin` attribute"
    }
    return node
}

fun manifestFile(project: Project): File? {
    var filePath: String? = null

    val mainOutput = project.sourceSets.main.get().output
    val resourcesDir = mainOutput.resourcesDir ?: error("Failed to find resources dir for ${project.name}")

    if (filePath != null) {
        return resourcesDir.resolve(filePath).takeIf { it.exists() }
            ?: error("Failed to find manifest file for ${project.name} module")
    }
    val rootManifestFile =
        manifestFile(project(":intellij-plugin")) ?: error("Failed to find manifest file for :intellij-plugin module")
    val rootManifest = parseManifest(rootManifestFile)
    val children = ((rootManifest["content"] as? List<*>)?.single() as? Node)?.children()
        ?: error("Failed to find module declarations in root manifest")
    return children.filterIsInstance<Node>()
        .flatMap { node ->
            if (node.name() != "module") return@flatMap emptyList()
            val name = node.attribute("name") as? String ?: return@flatMap emptyList()
            listOfNotNull(resourcesDir.resolve("$name.xml").takeIf { it.exists() })
        }.firstOrNull() ?: error("Failed to find manifest file for ${project.name} module")
}

fun findModulePackage(project: Project): String? {
    val moduleManifest = manifestFile(project) ?: return null
    val node = parseManifest(moduleManifest)
    return node.attribute("package") as? String ?: error("Failed to find package for ${project.name}")
}

fun verifyClasses(project: Project) {
    val pkg = findModulePackage(project) ?: return
    val expectedDir = pkg.replace('.', '/')

    var hasErrors = false
    for (classesDir in project.sourceSets.main.get().output.classesDirs) {
        val basePath = classesDir.toPath()
        for (file in classesDir.walk()) {
            if (file.isFile && file.extension == "class") {
                val relativePath = basePath.relativize(file.toPath())
                if (!relativePath.startsWith(expectedDir)) {
                    logger.error(
                        "Wrong package of `${
                            relativePath.joinToString(".").removeSuffix(".class")
                        }` class. Expected `$pkg`"
                    )
                    hasErrors = true
                }
            }
        }
    }

    if (hasErrors) {
        throw GradleException("Classes with wrong package were found. See https://docs.google.com/document/d/1pOy-qNlGOJe6wftHVYHkH8sZOoAfav1fdGDPJgkQWJo")
    }
}

fun DependencyHandler.implementationWithoutKotlin(dependencyNotation: Provider<*>) {
    implementation(dependencyNotation) {
        excludeKotlinDeps()
    }
}

fun DependencyHandler.testImplementationWithoutKotlin(dependencyNotation: Provider<*>) {
    testImplementation(dependencyNotation) {
        excludeKotlinDeps()
    }
}
