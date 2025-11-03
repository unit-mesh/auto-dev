@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.serialization)
    id("dev.petuska.npm.publish") version "3.5.3"
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
                implementation(npm("@unit-mesh/treesitter-artifacts", "1.7.3"))
            }
        }

        jsTest {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

npmPublish {
    organization.set("autodev")

    packages {
        named("js") {
            packageJson {
                name = "@autodev/codegraph"
                version = project.version.toString()
                main = "autodev-codegraph.js"
                types = "autodev-codegraph.d.ts"
                description.set("AutoDev Code Graph - TreeSitter-based code analysis for Kotlin Multiplatform")
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
                keywords.set(listOf("kotlin", "multiplatform", "treesitter", "code-analysis", "ast"))
            }
        }
    }
}

