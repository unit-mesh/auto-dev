plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(project(":mpp-core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Rich text editor for Compose
    implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")

    // JSON 处理
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

compose.desktop {
    application {
        mainClass = "cc.unitmesh.devins.ui.EditorDemoMainKt"

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
                // upgradeUuid = "18159995-d967-4CD2-8885-77BFA97CFA9F"
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
        println(configurations.runtimeClasspath.get().asPath)
    }
}
