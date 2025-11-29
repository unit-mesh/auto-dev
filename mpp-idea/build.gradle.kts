import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.gradleIntelliJPlugin)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)
}

group = "cc.unitmesh.devins"
version = project.findProperty("mppVersion") as String? ?: "0.3.2"

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

    intellijPlatform {
        defaultRepositories()
    }
    google()
}

dependencies {
    // Kotlinx serialization for JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)

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
        version = project.findProperty("mppVersion") as String? ?: "0.3.2"

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

