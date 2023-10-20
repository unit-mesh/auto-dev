package cc.unitmesh.devti.template

class DockerfileContext(
    val buildSystemName: String,
    val buildSystemVersion: String,
    val languageName: String,
    val languageVersion: String,
    val port: Int = 3000,
) : TemplateContext
