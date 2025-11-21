package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.thymeleaf.Thymeleaf
import io.ktor.server.thymeleaf.ThymeleafContent
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.http.*
import com.example.model.*

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/thymeleaf/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }

    routing {
        staticResources("static", "static")

        route("/tasks") {
            get {
                val tasks = TaskRepository.allTasks()
                call.respond(ThymeleafContent("all-tasks", mapOf("tasks" to tasks)))
            }

            get("/byName") {
                val name = call.request.queryParameters["name"]
                if (name == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing name parameter")
                    return@get
                }
                val task = TaskRepository.taskByName(name)
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound, "Task not found")
                    return@get
                }
                call.respond(ThymeleafContent("single-task", mapOf("task" to task)))
            }

            get("/byPriority") {
                val priorityAsText = call.request.queryParameters["priority"]
                if (priorityAsText == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing priority parameter")
                    return@get
                }
                try {
                    val priority = Priority.valueOf(priorityAsText)
                    val tasks = TaskRepository.tasksByPriority(priority)
                    call.respond(ThymeleafContent("tasks-by-priority", mapOf("tasks" to tasks, "priority" to priority)))
                } catch (ex: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid priority")
                }
            }

            post {
                val formParameters = call.receiveParameters()
                val name = formParameters["name"]
                val description = formParameters["description"]
                val priorityAsText = formParameters["priority"]

                if (name == null || description == null || priorityAsText == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing parameters")
                    return@post
                }

                try {
                    val priority = Priority.valueOf(priorityAsText)
                    val task = Task(name, description, priority)
                    TaskRepository.addTask(task)
                    call.respondRedirect("/tasks")
                } catch (ex: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid priority")
                } catch (ex: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest, ex.message ?: "Error")
                }
            }
        }

        get("/") {
            call.respondRedirect("/static/index.html")
        }
    }
}
