import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
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

dependencies {
    // Depend on mpp-ui and mpp-core JVM targets for shared UI components and ConfigManager
    // For KMP projects, we need to depend on the JVM target specifically
    // IMPORTANT: Exclude ALL transitive dependencies that conflict with IntelliJ's bundled libraries
    implementation("cc.unitmesh.devins:mpp-ui-jvm") {
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
        // Exclude SQLDelight - not needed in IntelliJ plugin
        exclude(group = "app.cash.sqldelight")
    }
    implementation("cc.unitmesh.devins:mpp-core-jvm") {
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
    }

    // Use platform-provided kotlinx libraries to avoid classloader conflicts
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // Ktor HTTP Client for LLM API calls - use compileOnly for libraries that may conflict
    compileOnly("io.ktor:ktor-client-core:3.2.2")
    compileOnly("io.ktor:ktor-client-cio:3.2.2")
    compileOnly("io.ktor:ktor-client-content-negotiation:3.2.2")
    compileOnly("io.ktor:ktor-serialization-kotlinx-json:3.2.2")
    compileOnly("io.ktor:ktor-client-logging:3.2.2")

    testImplementation(kotlin("test"))

    intellijPlatform {
        // Target IntelliJ IDEA 2025.2+ for Compose support
        create("IC", "2025.2.1")

        bundledPlugins("com.intellij.java")

        // Compose support dependencies (bundled in IDEA 252+)
        bundledModules(
            "intellij.libraries.skiko",
            "intellij.libraries.compose.foundation.desktop",
            "intellij.platform.jewel.foundation",
            "intellij.platform.jewel.ui",
            "intellij.platform.jewel.ideLafBridge",
            "intellij.platform.compose"
        )

        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "AutoDev Compose UI"
        version = mppVersion

        ideaVersion {
            sinceBuild = "252"
        }
    }

    buildSearchableOptions = false
    instrumentCode = false
}

tasks {
    test {
        useJUnitPlatform()
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
