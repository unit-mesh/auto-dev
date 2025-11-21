@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.serialization)
    // Note: npm publish plugin disabled temporarily due to wasmJs incompatibility
    // id("dev.petuska.npm.publish") version "3.5.3"
}

repositories {
    google()
    mavenCentral()
}

version = "0.1.0"

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    js(IR) {
        useCommonJs()
        browser()
        nodejs()
        binaries.library()
        generateTypeScriptDefinitions()

        compilerOptions {
            moduleKind.set(org.jetbrains.kotlin.gradle.dsl.JsModuleKind.MODULE_ES)
            sourceMap.set(true)
            sourceMapEmbedSources.set(org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS)
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
        binaries.library()
        // Use d8 optimizer instead of binaryen to avoid wasm-validator errors
        d8 {
        }
    }

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "CodeGraph"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
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
                // TreeSitter JVM bindings - matching SASK versions
                implementation("io.github.bonede:tree-sitter:0.25.3")
                implementation("io.github.bonede:tree-sitter-java:0.23.4")
                implementation("io.github.bonede:tree-sitter-kotlin:0.3.8.1")
                implementation("io.github.bonede:tree-sitter-c-sharp:0.23.1")
                implementation("io.github.bonede:tree-sitter-javascript:0.23.1")
                implementation("io.github.bonede:tree-sitter-python:0.23.4")
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        jsMain {
            dependencies {
                // web-tree-sitter for JS platform
                implementation(npm("web-tree-sitter", "0.22.2"))
                // TreeSitter WASM artifacts - matching autodev-workbench versions
                implementation(npm("@unit-mesh/treesitter-artifacts", "1.7.6"))
            }
        }

        jsTest {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        wasmJsMain {
            dependencies {
                // web-tree-sitter for WASM-JS platform (uses same npm packages as JS)
                implementation(npm("web-tree-sitter", "0.22.2"))
                // TreeSitter WASM artifacts
                implementation(npm("@unit-mesh/treesitter-artifacts", "1.7.6"))

                implementation(devNpm("copy-webpack-plugin", "12.0.2"))
            }
        }

        wasmJsTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        iosMain {
            dependencies {
                // iOS uses a simplified implementation without TreeSitter
                // TreeSitter native bindings are not available for iOS
            }
        }

        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        iosTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

// npmPublish configuration disabled temporarily due to wasmJs incompatibility
// To publish JS package, manually configure npm package.json and use npm publish
//
// npmPublish {
//     organization.set("autodev")
//     packages {
//         named("js") {
//             packageJson {
//                 name = "@autodev/codegraph"
//                 version = project.version.toString()
//                 main = "autodev-codegraph.js"
//                 types = "autodev-codegraph.d.ts"
//                 description.set("AutoDev Code Graph - TreeSitter-based code analysis for Kotlin Multiplatform")
//                 author {
//                     name.set("Unit Mesh")
//                     email.set("h@phodal.com")
//                 }
//                 license.set("MIT")
//                 private.set(false)
//                 repository {
//                     type.set("git")
//                     url.set("https://github.com/unit-mesh/auto-dev.git")
//                 }
//                 keywords.set(listOf("kotlin", "multiplatform", "treesitter", "code-analysis", "ast"))
//             }
//         }
//     }
// }

