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
import io.ktor.server.sse.*
import io.ktor.sse.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

fun Application.configureRouting() {
    val config = ServerConfig.load()
    val projectService = ProjectService(config.projects)
    val agentService = AgentService(config.llm)

    // JSON serializer with polymorphic support for AgentEvent
    val json = Json {
        serializersModule = SerializersModule {
            polymorphic(AgentEvent::class) {
                subclass(AgentEvent.IterationStart::class)
                subclass(AgentEvent.LLMResponseChunk::class)
                subclass(AgentEvent.ToolCall::class)
                subclass(AgentEvent.ToolResult::class)
                subclass(AgentEvent.Error::class)
                subclass(AgentEvent.Complete::class)
            }
        }
    }

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

                sse("/stream") {
                    val projectId = call.parameters["projectId"] ?: run {
                        send(ServerSentEvent(json.encodeToString(AgentEvent.Error("Missing projectId parameter"))))
                        return@sse
                    }

                    val task = call.parameters["task"] ?: run {
                        send(ServerSentEvent(json.encodeToString(AgentEvent.Error("Missing task parameter"))))
                        return@sse
                    }

                    val project = projectService.getProject(projectId)
                    if (project == null) {
                        send(ServerSentEvent(json.encodeToString(AgentEvent.Error("Project not found"))))
                        return@sse
                    }

                    val request = AgentRequest(projectId = projectId, task = task)

                    try {
                        agentService.executeAgentStream(project.path, request).collect { event ->
                            val eventType = when (event) {
                                is AgentEvent.IterationStart -> "iteration"
                                is AgentEvent.LLMResponseChunk -> "llm_chunk"
                                is AgentEvent.ToolCall -> "tool_call"
                                is AgentEvent.ToolResult -> "tool_result"
                                is AgentEvent.Error -> "error"
                                is AgentEvent.Complete -> "complete"
                            }

                            val data = when (event) {
                                is AgentEvent.IterationStart -> json.encodeToString(event)
                                is AgentEvent.LLMResponseChunk -> json.encodeToString(event)
                                is AgentEvent.ToolCall -> json.encodeToString(event)
                                is AgentEvent.ToolResult -> json.encodeToString(event)
                                is AgentEvent.Error -> json.encodeToString(event)
                                is AgentEvent.Complete -> json.encodeToString(event)
                            }

                            send(ServerSentEvent(data = data, event = eventType))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val errorData = json.encodeToString(AgentEvent.Error("Execution failed: ${e.message}"))
                        send(ServerSentEvent(data = errorData, event = "error"))
                    }
                }
            }
        }
    }
}

