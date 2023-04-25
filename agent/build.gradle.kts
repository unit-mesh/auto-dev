plugins {
    id("java")
    alias(libs.plugins.kotlin) // Kotlin support
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.chapi.domain)
    implementation(libs.chapi.java)
    implementation(libs.chapi.kotlin)

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}
