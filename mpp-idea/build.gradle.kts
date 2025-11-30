import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.intellij.platform") version "2.10.2"
    kotlin("plugin.compose") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
}

group = "cc.unitmesh.devins"
version = project.findProperty("mppVersion") as String? ?: "0.3.2"

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                "-opt-in=org.jetbrains.jewel.foundation.ExperimentalJewelApi"
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
    maven("https://packages.jetbrains.team/maven/p/kpm/public/")
}

dependencies {
    // Kotlinx serialization for JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")

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
            "intellij.platform.compose",
            "intellij.platform.jewel.markdown.core",
            "intellij.platform.jewel.markdown.extension.gfmAlerts"
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
