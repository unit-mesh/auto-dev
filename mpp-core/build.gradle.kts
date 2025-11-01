plugins {
    kotlin("multiplatform") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("app.cash.sqldelight") version "2.1.0"
    id("com.android.library") version "8.10.0"

    id("dev.petuska.npm.publish") version "3.5.3"
}

repositories {
    google()
    mavenCentral()
}

version = "0.1.3"

sqldelight {
    databases {
        create("DevInsDatabase") {
            packageName.set("cc.unitmesh.devins.db")
        }
    }
}

android {
    namespace = "cc.unitmesh.devins.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm {
        compilations.all {
//            kotlinOptions {
//                jvmTarget = "17"
//            }
        }
    }

    js(IR) {
        moduleName = "autodev-mpp-core"
        useCommonJs() // 建议
        browser()
        nodejs()
        binaries.library()
        // Generate TypeScript definitions for better interop
        generateTypeScriptDefinitions()

        compilations.all {
            kotlinOptions {
                moduleKind = "es"
                sourceMap = true
                sourceMapEmbedSources = "always"
            }
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
                implementation("com.charleskorn.kaml:kaml:0.61.0")
                // kotlinx-io for cross-platform file system operations
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")

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
                // SQLDelight - Android SQLite driver
                implementation("app.cash.sqldelight:android-driver:2.1.0")
            }
        }

        jvmMain {
            dependencies {
                // SQLDelight - JVM SQLite driver
                implementation("app.cash.sqldelight:sqlite-driver:2.1.0")
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        jsMain {
            dependencies {
                // SQLDelight - JS driver
                implementation("app.cash.sqldelight:web-worker-driver:2.1.0")
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
