import org.jetbrains.changelog.Changelog
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.gradle.api.JavaVersion.VERSION_17

buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
}

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
//    alias(libs.plugins.grammarKit) // Grammar Kit Plugin
    id("org.jetbrains.grammarkit") version "2021.2.2"
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
//    implementation(libs.annotations)
    implementation("org.kohsuke:github-api:1.314")
    implementation("com.aallam.openai:openai-client:3.2.0")
    implementation("org.jetbrains:markdown:0.4.1")

    // for local testting
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation(libs.ktor.client.apache)
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName = properties("pluginName")
    version = properties("platformVersion")
    type = properties("platformType")

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath = provider { file(".qodana").canonicalPath }
    reportPath = provider { file("build/reports/inspections").canonicalPath }
    saveReport = true
    showReport = environment("QODANA_SHOW_REPORT").map { it.toBoolean() }.getOrElse(false)
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover.xmlReport {
    onCheck = true
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    val generateDevtiLexer = task<GenerateLexerTask>("generateFeakinLexer") {
        source.set("src/main/grammars/SilveryLexer.flex")
        targetDir.set("src/main/gen/cc/unitmesh/devti/lexer")
        targetClass.set("SilveryLexer")
        purgeOldFiles.set(true)
    }

    val generateDevtiParser = task<GenerateParserTask>("generateFeakinParser") {
        source.set("src/main/grammars/SilveryParser.bnf")
        targetRoot.set("src/main/gen")
        pathToParser.set("src/main/cc/unitmesh/devti/parser/SilveryParser.java")
        pathToPsiRoot.set("src/main/cc/unitmesh/devti/psi")
        purgeOldFiles.set(true)
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = VERSION_17
        targetCompatibility = VERSION_17
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = VERSION_17.toString()
            languageVersion = "1.8"
            // see https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
            apiVersion = "1.7"
            freeCompilerArgs = listOf("-Xjvm-default=all")
        }

        dependsOn(
            generateDevtiLexer,
            generateDevtiParser
        )
    }

    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription = "AutoCRUD is a fully automated AI-assisted programming tool and an implementation of the DevTi Intellij IDE designed for use in-flight."
        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

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
        channels = properties("pluginVersion").map { listOf(it.split('-').getOrElse(1) { "default" }.split('.').first()) }
    }
}
