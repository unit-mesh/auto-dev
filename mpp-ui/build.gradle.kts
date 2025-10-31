plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.application")
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

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
    
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "mpp-ui.js"
            }
        }
        nodejs {
            // Configure Node.js target for CLI
        }
        binaries.executable()
        compilerOptions {
            freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":mpp-core"))
//                implementation(compose.desktop.currentOs)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)

                // Rich text editor for Compose
                implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

                // JSON 处理
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

                // Multiplatform Markdown Renderer - using older stable version
                implementation("com.mikepenz:multiplatform-markdown-renderer:0.13.0")
                implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.13.0")
                
                // JSON 处理
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

//                testImplementation(kotlin("test"))
//                testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
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
                implementation(compose.desktop.currentOs)
//                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
                // Rich text editor for Compose Desktop
                implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13")
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
            }
        }
        
        val jsMain by getting {
            dependencies {
                implementation(compose.html.core)
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
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += setOf(
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

compose.desktop {
    application {
        mainClass = "cc.unitmesh.devins.ui.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "AutoDev Desktop"
            packageVersion = "1.0.0"
            description = "AutoDev Desktop Application with DevIns Support"
            copyright = "© 2024 AutoDev Team. All rights reserved."
            vendor = "AutoDev Team"

            windows {
                menuGroup = "AutoDev"
            }
            macOS {
                bundleID = "cc.unitmesh.devins.desktop"
            }
            linux {
                packageName = "autodev-desktop"
            }
        }
    }
}

tasks.register("printClasspath") {
    doLast {
        println(configurations["jvmRuntimeClasspath"].asPath)
    }
}
