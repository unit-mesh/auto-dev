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

// Include source directories from mpp-idea-core, mpp-idea-exts, and mpp-idea-lang
sourceSets {
    main {
        java {
            // Core module sources
            srcDir("../mpp-idea-core/src/main/kotlin")
            srcDir("../mpp-idea-core/src/main/java")
            
            // Extension modules sources
            srcDir("../mpp-idea-exts/devins-lang/src/main/kotlin")
            srcDir("../mpp-idea-exts/devins-lang/src/main/java")
            srcDir("../mpp-idea-exts/devins-lang/src/gen")
            srcDir("../mpp-idea-exts/ext-database/src/main/kotlin")
            srcDir("../mpp-idea-exts/ext-git/src/main/kotlin")
            srcDir("../mpp-idea-exts/ext-terminal/src/main/kotlin")
            
            // Language modules sources
            srcDir("../mpp-idea-lang/java/src/main/kotlin")
            srcDir("../mpp-idea-lang/kotlin/src/main/kotlin")
            srcDir("../mpp-idea-lang/pycharm/src/main/kotlin")
            srcDir("../mpp-idea-lang/javascript/src/main/kotlin")
            srcDir("../mpp-idea-lang/goland/src/main/kotlin")
            srcDir("../mpp-idea-lang/rust/src/main/kotlin")
        }
        resources {
            // Core resources
            srcDir("../mpp-idea-core/src/main/resources")
            
            // Extension resources
            srcDir("../mpp-idea-exts/devins-lang/src/main/resources")
            srcDir("../mpp-idea-exts/ext-database/src/main/resources")
            srcDir("../mpp-idea-exts/ext-git/src/main/resources")
            srcDir("../mpp-idea-exts/ext-terminal/src/main/resources")
            
            // Language resources
            srcDir("../mpp-idea-lang/java/src/main/resources")
            srcDir("../mpp-idea-lang/kotlin/src/main/resources")
            srcDir("../mpp-idea-lang/pycharm/src/main/resources")
            srcDir("../mpp-idea-lang/javascript/src/main/resources")
            srcDir("../mpp-idea-lang/goland/src/main/resources")
            srcDir("../mpp-idea-lang/rust/src/main/resources")
        }
    }
    test {
        java {
            srcDir("../mpp-idea-core/src/test/kotlin")
            srcDir("../mpp-idea-exts/devins-lang/src/test/kotlin")
        }
        resources {
            srcDir("../mpp-idea-core/src/test/resources")
            srcDir("../mpp-idea-exts/devins-lang/src/test/resources")
        }
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
    implementation("Xiiu:mpp-ui:$mppVersion") {
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

    // ======== Dependencies from old core module ========
    
    // Xodus embedded database
    implementation("org.jetbrains.xodus:xodus-openAPI:2.0.1")
    implementation("org.jetbrains.xodus:xodus-environment:2.0.1")
    implementation("org.jetbrains.xodus:xodus-entity-store:2.0.1")
    implementation("org.jetbrains.xodus:xodus-vfs:2.0.1")
    
    // MCP (Model Context Protocol)
    implementation("io.modelcontextprotocol:kotlin-sdk:0.7.2")
    
    // Ktor (implementation, not compileOnly, for SSE)
    implementation("io.ktor:ktor-client-cio:3.2.3")
    implementation("io.ktor:ktor-server-sse:3.2.3")
    
    // A2A SDK
    implementation("io.github.a2asdk:a2a-java-sdk-client:0.3.0.Beta1")
    
    // RxJava
    implementation("io.reactivex.rxjava3:rxjava:3.1.10")
    
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    
    // Retrofit
    implementation("com.squareup.retrofit2:converter-jackson:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    
    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")
    
    // Commonmark for Markdown
    implementation("org.commonmark:commonmark:0.21.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.21.0")
    
    // JSON Path
    implementation("com.jayway.jsonpath:json-path:2.9.0")
    
    // CSV
    implementation("com.jsoizo:kotlin-csv-jvm:1.10.0")
    
    // Kanban integrations
    implementation("org.kohsuke:github-api:1.326")
    implementation("org.gitlab4j:gitlab4j-api:5.8.0")
    
    // Template engine
    implementation("org.apache.velocity:velocity-engine-core:2.4.1")
    
    // Token count
    implementation("com.knuddels:jtokkit:1.1.0")
    
    // Gitignore parsing
    implementation("nl.basjes.gitignore:gitignore-reader:1.6.0")
    
    // Reflections
    implementation("org.reflections:reflections:0.10.2") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
    
    // ======== Dependencies from old exts modules ========
    
    // Git commit message
    implementation("cc.unitmesh:git-commit-message:0.4.6")

    // Ktor HTTP Client for LLM API calls - use compileOnly for libraries that may conflict
    compileOnly("io.ktor:ktor-client-core:3.2.2")
    compileOnly("io.ktor:ktor-client-content-negotiation:3.2.2")
    compileOnly("io.ktor:ktor-serialization-kotlinx-json:3.2.2")
    compileOnly("io.ktor:ktor-client-logging:3.2.2")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    // JUnit 4 is required by IntelliJ Platform test infrastructure (JUnit5TestEnvironmentInitializer)
    testRuntimeOnly("junit:junit:4.13.2")

    intellijPlatform {
        // Target IntelliJ IDEA 2025.2+ for Compose support
        create("IC", "2025.2.1")

        // Only specify core bundled plugins
        // Other plugins (JavaScript, Terminal, etc.) are dynamic dependencies handled by IntelliJ at runtime
        bundledPlugins(
            "com.intellij.java",
            "org.intellij.plugins.markdown",
            "com.jetbrains.sh",
            "Git4Idea",
            "org.jetbrains.kotlin",
            "org.jetbrains.plugins.gradle",
            "com.intellij.modules.json"
        )

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
