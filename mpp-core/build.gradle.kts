plugins {
    kotlin("multiplatform") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
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
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("com.charleskorn.kaml:kaml:0.61.0")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
            }
        }
        
        val jvmMain by getting {
            dependencies {
                // JVM specific dependencies if needed
            }
        }
        
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        
        val jsMain by getting {
            dependencies {
                // JS specific dependencies if needed
            }
        }
        
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        val wasmJsMain by getting {
            dependencies {
                // WASM specific dependencies if needed
            }
        }

        val wasmJsTest by getting {
            dependencies {
                implementation(kotlin("test-wasm-js"))
            }
        }
    }
}

// 添加运行演示的任务
tasks.register<JavaExec>("runDemo") {
    group = "application"
    description = "Run the DevIns Compiler Demo"
    classpath = kotlin.targets["jvm"].compilations["main"].output.allOutputs +
                kotlin.targets["jvm"].compilations["main"].runtimeDependencyFiles
    mainClass.set("cc.unitmesh.devins.compiler.demo.SimpleDemo")
}
