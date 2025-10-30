plugins {
    kotlin("multiplatform") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("app.cash.sqldelight") version "2.1.0"
}

repositories {
    google()
    mavenCentral()
}

sqldelight {
    databases {
        create("DevInsDatabase") {
            packageName.set("cc.unitmesh.devins.db")
        }
    }
}

kotlin {
    compilerOptions {
//        freeCompilerArgs.add("-Xdata-flow-based-exhaustiveness")
    }

    jvm {
        compilations.all {
//            kotlinOptions {
//                jvmTarget = "17"
//            }
        }
    }

    js(IR) {
        browser()
        nodejs()
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
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            }
        }

        jvmMain {
            dependencies {
                // Koog AI Framework - JVM only for now
                implementation("ai.koog:koog-agents:0.5.1")
                // Koog needs these executors
                implementation("ai.koog:prompt-executor-llms-all:0.5.1")

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
