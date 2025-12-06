plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "cc.unitmesh"
version = project.findProperty("mppVersion") as String? ?: "0.1.5"

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(project(":mpp-core"))

    // Serialization for test case format
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    
    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

// Custom task to run DSL evaluation tests
tasks.register<JavaExec>("runDslEval") {
    group = "verification"
    description = "Run NanoDSL AI evaluation tests"
    mainClass.set("cc.unitmesh.xuiper.eval.DslEvalRunnerKt")
    classpath = sourceSets["main"].runtimeClasspath
    
    // Pass environment variables for LLM configuration
    environment("OPENAI_API_KEY", System.getenv("OPENAI_API_KEY") ?: "")
    environment("ANTHROPIC_API_KEY", System.getenv("ANTHROPIC_API_KEY") ?: "")
    environment("DEEPSEEK_API_KEY", System.getenv("DEEPSEEK_API_KEY") ?: "")
}

