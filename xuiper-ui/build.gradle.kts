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

// Configure integration test source set
sourceSets {
    create("integrationTest") {
        kotlin.srcDir("src/integrationTest/kotlin")
        resources.srcDir("src/integrationTest/resources")
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

val integrationTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
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

    // Integration test dependencies
    integrationTestImplementation(kotlin("test"))
    integrationTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

// Integration test task
tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Run NanoDSL integration tests that call LLM and verify DSL compilation"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform()

    // Pass environment variables for LLM configuration
    environment("OPENAI_API_KEY", System.getenv("OPENAI_API_KEY") ?: "")
    environment("ANTHROPIC_API_KEY", System.getenv("ANTHROPIC_API_KEY") ?: "")
    environment("DEEPSEEK_API_KEY", System.getenv("DEEPSEEK_API_KEY") ?: "")

    // Integration tests may take longer
    systemProperty("junit.jupiter.execution.timeout.default", "5m")

    // Show output for debugging
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
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

// Task to validate DSL files
tasks.register<JavaExec>("validateDsl") {
    group = "verification"
    description = "Validate NanoDSL files in a directory"
    mainClass.set("cc.unitmesh.xuiper.eval.DslValidatorKt")
    classpath = sourceSets["main"].runtimeClasspath

    val dslDir = project.findProperty("dslDir") as? String ?: "testcases/actual/integration"
    val verbose = if (project.hasProperty("verbose")) "verbose" else ""
    args = listOf(dslDir, verbose)
}

// Task to render DSL files to HTML
tasks.register<JavaExec>("renderHtml") {
    group = "verification"
    description = "Render NanoDSL files to HTML"
    mainClass.set("cc.unitmesh.xuiper.eval.DslToHtmlRendererKt")
    classpath = sourceSets["main"].runtimeClasspath

    val inputDir = project.findProperty("dslDir") as? String ?: "testcases/actual/integration"
    val outputDir = project.findProperty("outputDir") as? String ?: "testcases/html/integration"
    args = listOf(inputDir, outputDir)
}

