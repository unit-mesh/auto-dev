package cc.unitmesh.server

import cc.unitmesh.server.config.ServerConfig
import cc.unitmesh.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val config = ServerConfig.load()
    
    embeddedServer(
        Netty,
        port = config.port,
        host = config.host,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureCORS()
    configureSSE()
    configureRouting()
}

