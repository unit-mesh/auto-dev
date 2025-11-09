package cc.unitmesh.server.plugins

import cc.unitmesh.server.config.ServerConfig
import cc.unitmesh.server.model.*
import cc.unitmesh.server.service.AgentService
import cc.unitmesh.server.service.ProjectService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val config = ServerConfig.load()
    val projectService = ProjectService(config.projects)
    val agentService = AgentService(config.llm)

    routing {
        // Health check
        get("/health") {
            call.respond(HealthResponse(status = "ok"))
        }

        // API routes
        route("/api") {
            // Project management
            route("/projects") {
                get {
                    val projects = projectService.listProjects()
                    call.respond(ProjectListResponse(projects = projects))
                }

                get("/{id}") {
                    val projectId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing project ID")
                    )

                    val project = projectService.getProject(projectId)
                    if (project == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to "Project not found")
                        )
                    } else {
                        call.respond(project)
                    }
                }
            }

            // Agent execution
            route("/agent") {
                // Synchronous execution
                post("/run") {
                    val request = call.receive<AgentRequest>()

                    val project = projectService.getProject(request.projectId)
                    if (project == null) {
                        return@post call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to "Project not found")
                        )
                    }

                    try {
                        val response = agentService.executeAgent(project.path, request)
                        call.respond(response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to (e.message ?: "Unknown error"))
                        )
                    }
                }
            }
        }
    }
}

