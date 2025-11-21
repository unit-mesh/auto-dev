import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.net.URL

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://jogamp.org/deployment/maven")
}

group = "cc.unitmesh.viewer.web"
version = project.findProperty("mppVersion") as String? ?: "0.1.5"

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":mpp-viewer"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)

                // JSON serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

                // compose-webview-multiplatform
                implementation("io.github.kevinnzou:compose-webview-multiplatform:2.0.3")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

// Desktop configuration for KCEF (Chromium Embedded Framework)
compose.desktop {
    application {
        mainClass = "cc.unitmesh.viewer.web.MermaidPreviewKt"
    }
}

// Add JVM flags for KCEF
afterEvaluate {
    tasks.withType<JavaExec> {
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }
}

// Task to download Mermaid.js library
abstract class DownloadMermaidTask : DefaultTask() {
    @get:Input
    val mermaidVersion = "11.4.0"
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun download() {
        val output = outputFile.get().asFile
        
        if (output.exists()) {
            logger.lifecycle("Mermaid.js already exists: ${output.absolutePath}")
            return
        }
        
        output.parentFile.mkdirs()
        
        logger.lifecycle("Downloading mermaid.min.js version $mermaidVersion...")
        val jsUrl = "https://cdn.jsdelivr.net/npm/mermaid@$mermaidVersion/dist/mermaid.min.js"
        
        try {
            URL(jsUrl).openStream().use { input ->
                output.outputStream().use { outputStream ->
                    input.copyTo(outputStream)
                }
            }
            logger.lifecycle("Downloaded: ${output.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to download Mermaid.js: ${e.message}")
            throw e
        }
    }
}

val downloadMermaid = tasks.register<DownloadMermaidTask>("downloadMermaid") {
    group = "build"
    description = "Download Mermaid.js library"
    outputFile.set(file("src/commonMain/resources/mermaid/mermaid.min.js"))
}

// Make jvmProcessResources depend on downloadMermaid
tasks.named("jvmProcessResources") {
    dependsOn(downloadMermaid)
}

