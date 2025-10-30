plugins {
    kotlin("multiplatform") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
}

repositories {
    mavenCentral()
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
                // kotlinx-datetime 已移除，使用 Kotlin 标准库的 kotlin.time API
                implementation("com.charleskorn.kaml:kaml:0.61.0")

                implementation("ai.koog:koog-agents:0.5.0")
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
                // JVM specific dependencies if needed
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        jsMain {
            dependencies {
                // JS specific dependencies if needed
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
