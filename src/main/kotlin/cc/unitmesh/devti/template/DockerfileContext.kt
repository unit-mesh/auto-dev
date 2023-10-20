package cc.unitmesh.devti.template

class DockerfileContext(
    val buildToolName: String,
    val buildToolVersion: String,
    val languageName: String,
    val languageVersion: String,
    val port: Int = 3000,
) : TemplateContext
