plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
    application
}

group = "com.phodal.server"
version = project.findProperty("mppVersion") as String? ?: "0.1.5"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

application {
    mainClass.set("cc.unitmesh.server.ServerApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    // Use JVM target from multiplatform project
    implementation(projects.mppCore)

    // Add kotlin-logging explicitly for JVM
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.16")

    // Ktor Server
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationJson)
    implementation(libs.ktor.serverCors)
    implementation(libs.ktor.serverSse)

    // Testing
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(kotlin("test"))
}

// Create a fat JAR task
tasks.register<Jar>("fatJar") {
    archiveBaseName.set("mpp-server")
    archiveClassifier.set("all")
    archiveVersion.set(project.version.toString())
    
    manifest {
        attributes["Main-Class"] = "cc.unitmesh.server.ServerApplicationKt"
    }
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    // Include all dependencies
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    
    // Include compiled classes
    with(tasks.jar.get())
}

// Make build depend on fatJar
tasks.named("build") {
    dependsOn("fatJar")
}
