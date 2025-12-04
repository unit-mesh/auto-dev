import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
    kotlin("plugin.compose")
}

group = "cc.unitmesh.devins"
val mppVersion = project.findProperty("mppVersion") as String? ?: "0.3.2"
version = mppVersion

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
            )
        )
    }
}

repositories {
    mavenLocal()  // For locally published mpp-ui and mpp-core artifacts
    mavenCentral()
    google()
    // Required for mpp-ui's webview dependencies (jogamp)
    maven("https://jogamp.org/deployment/maven")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/")

    intellijPlatform {
        defaultRepositories()
    }
}

// Global exclusions for heavy dependencies not needed in IntelliJ plugin
// These exclusions apply to ALL configurations including transitive dependencies from includeBuild projects
configurations.all {
    exclude(group = "aws.sdk.kotlin")           // AWS SDK (~30MB) - from ai.koog:prompt-executor-bedrock-client
    exclude(group = "aws.smithy.kotlin")        // AWS Smithy runtime
    exclude(group = "org.apache.tika")          // Apache Tika (~100MB) - document parsing
    exclude(group = "org.apache.poi")           // Apache POI - Office document parsing (from Tika)
    exclude(group = "org.apache.pdfbox")        // PDFBox (~10MB) - PDF parsing
    exclude(group = "net.sourceforge.plantuml") // PlantUML (~20MB) - from dev.snipme:highlights
    exclude(group = "org.jsoup")                // Jsoup HTML parser
    exclude(group = "ai.koog", module = "prompt-executor-bedrock-client")  // Bedrock executor
    // Redis/Lettuce - not needed in IDEA plugin (~5MB)
    exclude(group = "io.lettuce")
    exclude(group = "io.projectreactor")
    // RxJava - not needed (~2.6MB)
    exclude(group = "io.reactivex.rxjava3")
    // RSyntaxTextArea - IDEA has its own editor (~1.3MB)
    exclude(group = "com.fifesoft")
    // Netty - not needed for IDEA plugin (~3MB)
    exclude(group = "io.netty")
    // pty4j/jediterm - IDEA has its own terminal (~3MB)
    exclude(group = "org.jetbrains.pty4j")
    exclude(group = "org.jetbrains.jediterm")
}

dependencies {
    // Depend on mpp-ui and mpp-core JVM targets for shared UI components and ConfigManager
    // For KMP projects, we need to depend on the JVM target specifically
    // IMPORTANT: Exclude ALL transitive dependencies that conflict with IntelliJ's bundled libraries
    // Note: For KMP projects, the module is published as "group:artifact-jvm" but the project
    // dependency substitution should map "group:artifact" to the project ":artifact"
    implementation("AutoDev-Intellij:mpp-ui:$mppVersion") {
        // Exclude all Compose dependencies - IntelliJ provides its own via bundledModules
        exclude(group = "org.jetbrains.compose")
        exclude(group = "org.jetbrains.compose.runtime")
        exclude(group = "org.jetbrains.compose.foundation")
        exclude(group = "org.jetbrains.compose.material3")
        exclude(group = "org.jetbrains.compose.material")
        exclude(group = "org.jetbrains.compose.ui")
        exclude(group = "org.jetbrains.compose.desktop")
        exclude(group = "org.jetbrains.compose.components")
        exclude(group = "org.jetbrains.compose.animation")
        exclude(group = "org.jetbrains.skiko")
        // Exclude kotlinx libraries - IntelliJ provides its own
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-swing")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json-io")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json-io-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-io-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-io-core-jvm")
        // Exclude webview/KCEF - not needed in IntelliJ and causes issues
        exclude(group = "io.github.kevinnzou")
        exclude(group = "dev.datlag")
        // Exclude other UI libraries that may conflict
        exclude(group = "com.mohamedrejeb.richeditor")
        exclude(group = "cafe.adriel.bonsai")
        exclude(group = "com.mikepenz")
        exclude(group = "org.jetbrains.jediterm")
        exclude(group = "org.jetbrains.pty4j")
        exclude(group = "io.github.vinceglb")
    }
    implementation("cc.unitmesh:mpp-core:$mppVersion") {
        // Exclude Compose dependencies from mpp-core as well
        exclude(group = "org.jetbrains.compose")
        exclude(group = "org.jetbrains.compose.runtime")
        exclude(group = "org.jetbrains.compose.foundation")
        exclude(group = "org.jetbrains.compose.material3")
        exclude(group = "org.jetbrains.compose.material")
        exclude(group = "org.jetbrains.compose.ui")
        exclude(group = "org.jetbrains.skiko")
        // Exclude kotlinx libraries - IntelliJ provides its own
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json-io")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json-io-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-io-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-io-core-jvm")
        // Note: Heavy dependencies (AWS, Tika, POI, PDFBox, PlantUML, Jsoup) are excluded globally above
    }

    // Use platform-provided kotlinx libraries to avoid classloader conflicts
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // Gson for JSON serialization (used by IdeaRemoteAgentClient)
    compileOnly("com.google.code.gson:gson:2.11.0")

    // Note: We use SimpleJewelMarkdown with intellij-markdown parser instead of mikepenz
    // to avoid Compose runtime version mismatch with IntelliJ's bundled Compose

    // SQLite JDBC driver for SQLDelight (required at runtime)
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")

    // DevIn language support for @ and / completion
    // These provide the DevIn language parser, completion contributors, and core functionality
    implementation("AutoDev-Intellij:exts-devins-lang:$mppVersion") {
        // Exclude kotlinx libraries - IntelliJ provides its own
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-swing")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core-jvm")
    }
    implementation("AutoDev-Intellij:core:$mppVersion") {
        // Exclude kotlinx libraries - IntelliJ provides its own
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-swing")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core-jvm")
    }

    // Ktor HTTP Client for LLM API calls - use compileOnly for libraries that may conflict
    compileOnly("io.ktor:ktor-client-core:3.2.2")
    compileOnly("io.ktor:ktor-client-cio:3.2.2")
    compileOnly("io.ktor:ktor-client-content-negotiation:3.2.2")
    compileOnly("io.ktor:ktor-serialization-kotlinx-json:3.2.2")
    compileOnly("io.ktor:ktor-client-logging:3.2.2")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    // JUnit 4 is required by IntelliJ Platform test infrastructure (JUnit5TestEnvironmentInitializer)
    testRuntimeOnly("junit:junit:4.13.2")

    intellijPlatform {
        // Target IntelliJ IDEA 2025.2+ for Compose support
        create("IC", "2025.2.1")

        bundledPlugins("com.intellij.java", "org.intellij.plugins.markdown", "com.jetbrains.sh", "Git4Idea")

        // Compose support dependencies (bundled in IDEA 252+)
        bundledModules(
            "intellij.libraries.skiko",
            "intellij.libraries.compose.foundation.desktop",
            "intellij.platform.jewel.foundation",
            "intellij.platform.jewel.ui",
            "intellij.platform.jewel.ideLafBridge",
            "intellij.platform.compose"
        )

        // Note: testFramework(TestFrameworkType.Platform) is removed because:
        // 1. It requires JUnit 4 (junit.framework.TestCase) which conflicts with JUnit 5
        // 2. JewelRendererTest uses JUnit 5 and doesn't need IntelliJ Platform
        // 3. IdeaAgentViewModelTest (which needs Platform) is temporarily disabled
        // To run platform tests, uncomment testFramework and add JUnit 4 dependency
        // testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "AutoDev Experiment"
        version = mppVersion

        ideaVersion {
            sinceBuild = "252"
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    buildSearchableOptions = false
    instrumentCode = false
}

tasks {
    test {
        useJUnitPlatform()
    }

    // Exclude large font files from mpp-ui that are not needed in IDEA plugin
    // These fonts are for Desktop/WASM apps, IDEA has its own fonts
    named<org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask>("prepareSandbox") {
        // Exclude font files from the plugin distribution
        // NotoSansSC-Regular.ttf (~18MB), NotoColorEmoji.ttf (~11MB x2), FiraCode fonts (~1MB)
        exclude("**/fonts/**")
        exclude("**/composeResources/**/font/**")
        exclude("**/*.ttf")
        exclude("**/*.otf")
        // Also exclude icon files meant for desktop app
        exclude("**/icon.icns")
        exclude("**/icon.ico")
        exclude("**/icon-512.png")
    }

    // Task to verify no conflicting dependencies are included
    register("verifyNoDuplicateDependencies") {
        group = "verification"
        description = "Verifies that no Compose/Kotlinx dependencies are included that would conflict with IntelliJ's bundled versions"

        doLast {
            val forbiddenPatterns = listOf(
                "org.jetbrains.compose",
                "org.jetbrains.skiko",
                "kotlinx-coroutines-core",
                "kotlinx-coroutines-swing",
                "kotlinx-serialization-json",
                "kotlinx-serialization-core"
            )

            val runtimeClasspath = configurations.getByName("runtimeClasspath")
            val violations = mutableListOf<String>()

            runtimeClasspath.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
                val id = artifact.moduleVersion.id
                val fullName = "${id.group}:${id.name}:${id.version}"
                forbiddenPatterns.forEach { pattern ->
                    if (fullName.contains(pattern)) {
                        violations.add(fullName)
                    }
                }
            }

            if (violations.isNotEmpty()) {
                throw GradleException("""
                    |DEPENDENCY CONFLICT DETECTED!
                    |The following dependencies will conflict with IntelliJ's bundled libraries:
                    |${violations.joinToString("\n") { "  - $it" }}
                    |
                    |These dependencies must be excluded from mpp-ui and mpp-core.
                """.trimMargin())
            } else {
                println("✓ No conflicting dependencies found in runtime classpath")
            }
        }
    }

    // Run verification before build
    named("build") {
        dependsOn("verifyNoDuplicateDependencies")
    }
}
