plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":mpp-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")

    // 用于代码编辑器功能
    implementation("com.fifesoft:rsyntaxtextarea:3.3.4")

    // 现代化 UI 主题
    implementation("com.formdev:flatlaf:3.2.5")
    implementation("com.formdev:flatlaf-extras:3.2.5")

    // JSON 处理
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

application {
    mainClass.set("cc.unitmesh.devins.ui.MainKt")
}

tasks.register("printClasspath") {
    doLast {
        println(configurations.runtimeClasspath.get().asPath)
    }
}
