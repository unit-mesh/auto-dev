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
    implementation("cc.unitmesh.devins:mpp-ui-jvm")
    implementation("cc.unitmesh.devins:mpp-core-jvm")

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
}
