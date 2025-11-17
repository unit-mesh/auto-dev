import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.application")
    id("app.cash.sqldelight") version "2.1.0"
    id("de.comahe.i18n4k") version "0.11.1"
    alias(libs.plugins.ktlint)
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/")
    maven("https://jogamp.org/deployment/maven")
}

sqldelight {
    databases {
        create("DevInsDatabase") {
            packageName.set("cc.unitmesh.devins.db")
        }
    }
}

i18n4k {
    sourceCodeLocales = listOf("en", "zh")
}

version = project.findProperty("mppVersion") as String? ?: "0.1.5"

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        }
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        }
    }

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "AutoDevUI"
            isStatic = true

            // Set bundle ID
            binaryOption("bundleId", "com.phodal.autodev")

            // Export dependencies to make them available in Swift
            export(project(":mpp-core"))
            export(compose.runtime)
            export(compose.foundation)
            export(compose.material3)
            export(compose.ui)
        }
    }

    js(IR) {
        // Node.js CLI only - no browser compilation
        // Web UI uses pure TypeScript/React + mpp-core (similar to CLI architecture)
        nodejs {
            // Configure Node.js target for CLI
        }
        useCommonJs()
        binaries.executable()
        compilerOptions {
            freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "mpp-ui.js"
            }
        }
        binaries.executable()
        compilerOptions {
            freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        }
//        d8 {
//            // Use d8 instead of binaryen (wasm-opt) for now
//        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":mpp-core"))
                implementation(project(":mpp-codegraph"))
                implementation(project(":mpp-viewer"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                // Animation (needed for animateContentSize in LiveTerminalItem)
                implementation(compose.animation)

                // Rich text editor for Compose
                implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

                // DateTime for KMP
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

                implementation("com.charleskorn.kaml:kaml:0.61.0")

                // JSON 处理
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

                // FileKit - Cross-platform file picker
                implementation("io.github.vinceglb:filekit-core:0.12.0")
                implementation("io.github.vinceglb:filekit-dialogs:0.12.0")

//                implementation("javax.naming:jndi:1.2.1")

                // Ktor HTTP Client (for remote agent)
                implementation("io.ktor:ktor-client-core:3.2.2")

                // i18n4k - Internationalization
                implementation("de.comahe.i18n4k:i18n4k-core:0.11.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(project(":mpp-viewer-web"))
                implementation(compose.desktop.currentOs)
                // Rich text editor for Compose Desktop
                implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13")

                // Bonsai Tree View (JVM only)
                implementation("cafe.adriel.bonsai:bonsai-core:1.2.0")
                implementation("cafe.adriel.bonsai:bonsai-file-system:1.2.0")

                // SQLDelight - JVM SQLite driver
                implementation("app.cash.sqldelight:sqlite-driver:2.1.0")

                // Multiplatform Markdown Renderer for JVM
                implementation("com.mikepenz:multiplatform-markdown-renderer:0.38.1")
                implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.38.1")

                // Logback for JVM logging backend with file storage
                implementation("ch.qos.logback:logback-classic:1.5.19") {
                    exclude(group = "javax.naming", module = "javax.naming-api")
                }

                // RSyntaxTextArea for syntax highlighting in JVM
                implementation("com.fifesoft:rsyntaxtextarea:3.6.0")

                implementation("org.jetbrains.pty4j:pty4j:0.13.10")
                implementation("org.jetbrains.jediterm:jediterm-core:3.57")
                implementation("org.jetbrains.jediterm:jediterm-ui:3.57")

                // Coroutines Swing for Dispatchers.Main on JVM Desktop
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")

                // Ktor HTTP Client CIO engine for JVM
                implementation("io.ktor:ktor-client-cio:3.2.2")

                // i18n4k - JVM
                implementation("de.comahe.i18n4k:i18n4k-core-jvm:0.11.1")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:1.11.0")
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("androidx.core:core-ktx:1.17.0")

                // Bonsai Tree View (Android)
                implementation("cafe.adriel.bonsai:bonsai-core:1.2.0")
                implementation("cafe.adriel.bonsai:bonsai-file-system:1.2.0")

                // SQLDelight - Android SQLite driver
                implementation("app.cash.sqldelight:android-driver:2.1.0")

                // Multiplatform Markdown Renderer for Android
                implementation("com.mikepenz:multiplatform-markdown-renderer:0.38.1")
                implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.38.1")

                // Coroutines Android for Dispatchers.Main on Android
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

                // Ktor HTTP Client CIO engine for Android
                implementation("io.ktor:ktor-client-cio:3.2.2")

                // i18n4k - Android
                implementation("de.comahe.i18n4k:i18n4k-core-android:0.11.1")
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        iosMain {
            dependencies {
                // API dependencies (exported in framework)
                api(project(":mpp-core"))
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material3)
                api(compose.ui)

                // SQLDelight - iOS SQLite driver
                implementation("app.cash.sqldelight:native-driver:2.1.0")

                // Ktor HTTP Client Darwin engine for iOS
                implementation("io.ktor:ktor-client-darwin:3.2.2")

                // Multiplatform Markdown Renderer for iOS
                implementation("com.mikepenz:multiplatform-markdown-renderer:0.38.1")
                implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.38.1")
            }
        }

        val jsMain by getting {
            dependencies {
                // Node.js CLI dependencies
                implementation(compose.html.core)

                // SQLDelight - JS driver
                implementation("app.cash.sqldelight:web-worker-driver:2.1.0")

                // Ktor HTTP Client JS engine
                implementation("io.ktor:ktor-client-js:3.2.2")

                // i18n4k - JS
                implementation("de.comahe.i18n4k:i18n4k-core-js:0.11.1")
            }
        }

        val wasmJsMain by getting {
            dependencies {
                // Force kotlin-stdlib to 2.2.0 to match compiler version
                implementation("org.jetbrains.kotlin:kotlin-stdlib") {
                    version {
                        strictly(libs.versions.kotlin.get())
                    }
                }

                // WASM browser dependencies
                // SQLDelight - Web Worker driver (same as JS)
                implementation("app.cash.sqldelight:web-worker-driver:2.1.0")

                // Ktor HTTP Client JS engine (works for WASM too)
                implementation("io.ktor:ktor-client-js:3.2.2")

                // i18n4k - WASM
                implementation("de.comahe.i18n4k:i18n4k-core-wasm-js:0.11.1")
            }
        }
    }
}

android {
    namespace = "cc.unitmesh.devins.ui"
    compileSdk = 36

    defaultConfig {
        applicationId = "cc.unitmesh.devins.ui"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = version.toString()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources {
            excludes +=
                setOf(
                    "META-INF/INDEX.LIST",
                    "META-INF/DEPENDENCIES",
                    "META-INF/LICENSE",
                    "META-INF/LICENSE.txt",
                    "META-INF/license.txt",
                    "META-INF/NOTICE",
                    "META-INF/NOTICE.txt",
                    "META-INF/notice.txt",
                    "META-INF/*.kotlin_module",
                    "META-INF/io.netty.versions.properties"
                )
        }
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}

compose.desktop {
    application {
        mainClass = "cc.unitmesh.devins.ui.MainKt"

        jvmArgs += listOf(
            "--add-modules", "java.naming,java.sql"
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "AutoDev Desktop"
            packageVersion = "1.0.5"
            description = "AutoDev Desktop Application with DevIns Support"
            copyright = "© 2024 AutoDev Team. All rights reserved."
            vendor = "AutoDev Team"

            modules("java.naming", "java.sql")

            // Custom app icon
            macOS {
                bundleID = "cc.unitmesh.devins.desktop"
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }
            windows {
                menuGroup = "AutoDev"
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }
            linux {
                packageName = "autodev-desktop"
                iconFile.set(project.file("src/jvmMain/resources/icon-512.png"))
            }
        }
    }
}

tasks.register("printClasspath") {
    doLast {
        println(configurations["jvmRuntimeClasspath"].asPath)
    }
}

// Task to run Remote Agent CLI
tasks.register<JavaExec>("runRemoteAgentCli") {
    group = "application"
    description = "Run Remote Agent CLI (Kotlin equivalent of TypeScript server command)"

    val jvmCompilation = kotlin.jvm().compilations.getByName("main")
    classpath(jvmCompilation.output, configurations["jvmRuntimeClasspath"])
    mainClass.set("cc.unitmesh.devins.ui.cli.RemoteAgentCli")

    // Allow passing arguments from command line
    // Usage: ./gradlew :mpp-ui:runRemoteAgentCli --args="--server http://localhost:8080 --project-id autocrud --task 'Write tests'"
    if (project.hasProperty("args")) {
        args = (project.property("args") as String).split(" ")
    }

    // Enable standard input for interactive mode (if needed in future)
    standardInput = System.`in`
}

// Task to run Code Review Demo
tasks.register<JavaExec>("runCodeReviewDemo") {
    group = "application"
    description = "Run Code Review Demo (Side-by-Side UI with Git integration)"

    val jvmCompilation = kotlin.jvm().compilations.getByName("main")
    classpath(jvmCompilation.output, configurations["jvmRuntimeClasspath"])
    mainClass.set("cc.unitmesh.devins.ui.compose.agent.codereview.demo.CodeReviewDemoKt")

    // Enable standard input
    standardInput = System.`in`
}

// Ktlint configuration
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.0.1")
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(true)

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

// Note: We enable wasm-opt optimizer for production webpack builds
// If this causes issues, use wasmJsBrowserDevelopmentExecutableDistribution instead
// tasks.named("compileProductionExecutableKotlinWasmJsOptimize") {
//     enabled = false
// }

// Ensure wasmJsBrowserDistribution runs webpack before copying files
tasks.named("wasmJsBrowserDistribution") {
    dependsOn("wasmJsBrowserProductionWebpack")
}

// Force webpack to run (remove onlyIf condition that skips it)
tasks.named("wasmJsBrowserProductionWebpack") {
    onlyIf { true }
}
