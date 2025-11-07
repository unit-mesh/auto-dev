plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")

    id("dev.petuska.npm.publish") version "3.5.3"
}

repositories {
    google()
    mavenCentral()
}

version = "0.1.4"

android {
    namespace = "cc.unitmesh.devins.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    lint {
        abortOnError = false
        warningsAsErrors = false
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    js(IR) {
        outputModuleName = "autodev-mpp-core"
        // Support both browser and Node.js with UMD (for compatibility)
        browser()
        nodejs()
        binaries.library()
        // Generate TypeScript definitions for better interop
        generateTypeScriptDefinitions()

        compilerOptions {
            // UMD is the most compatible format for both Node.js and browser (with bundlers)
            moduleKind.set(org.jetbrains.kotlin.gradle.dsl.JsModuleKind.MODULE_UMD)
            sourceMap.set(true)
            sourceMapEmbedSources.set(org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS)
        }
    }

    // Temporarily disable wasmJs due to configuration issues
    // @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    // wasmJs {
    //     browser()
    //     nodejs()
    // }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
                implementation("com.charleskorn.kaml:kaml:0.61.0")
                // kotlinx-io for cross-platform file system operations
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")

                // Ktor HTTP Client for web fetching (core only in common)
                implementation("io.ktor:ktor-client-core:3.2.2")

                // Kotlin Logging for multiplatform logging
                implementation("io.github.oshai:kotlin-logging:7.0.13")

                // Koog AI Framework - JVM only for now
                implementation("ai.koog:koog-agents:0.5.1")
                // Koog needs these executors
                implementation("ai.koog:prompt-executor-llms-all:0.5.1")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            }
        }

        androidMain {
            dependencies {
                // AndroidX DocumentFile for SAF support
                implementation("androidx.documentfile:documentfile:1.0.1")

                // Ktor CIO engine for Android
                implementation("io.ktor:ktor-client-cio:3.2.2")
            }
        }

        jvmMain {
            repositories {
                google()
                mavenCentral()
                maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/")
            }

            dependencies {
                // Ktor CIO engine for JVM
                implementation("io.ktor:ktor-client-cio:3.2.2")

                // MCP SDK for JVM
                implementation("io.modelcontextprotocol:kotlin-sdk:0.7.4")

                // Logback for JVM logging backend with file storage
                implementation("ch.qos.logback:logback-classic:1.5.19")

                // JediTerm for terminal emulation (uses pty4j under the hood)
                implementation("org.jetbrains.pty4j:pty4j:0.13.10")
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        jsMain {
            dependencies {
                // Ktor JS engine for JavaScript
                implementation("io.ktor:ktor-client-js:3.2.2")
            }
        }

        jsTest {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

//        val wasmJsMain by getting {
//            dependencies {
//                // WASM specific dependencies if needed
//            }
//        }
//
//        val wasmJsTest by getting {
//            dependencies {
//                implementation(kotlin("test-wasm-js"))
//            }
//        }
    }
}

npmPublish {
    organization.set("autodev")

    packages {
        named("js") {
            packageJson {
                name = "@autodev/mpp-core"
                version = project.version.toString()
                main = "autodev-mpp-core.js"
                types = "autodev-mpp-core.d.ts"
                description.set("AutoDev Multiplatform Core - AI Agent and DevIns Compiler")
                author {
                    name.set("Unit Mesh")
                    email.set("h@phodal.com")
                }
                license.set("MIT")
                private.set(false)
                repository {
                    type.set("git")
                    url.set("https://github.com/unit-mesh/auto-dev.git")
                }
                keywords.set(listOf("kotlin", "multiplatform", "ai", "llm", "devins"))
            }
        }
    }
}
