package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.http.content.*
import com.example.model.*

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        staticResources("static", "static")

        get("/tasks") {
            call.respond(
                listOf(
                    Task("cleaning", "Clean the house", Priority.Low),
                    Task("gardening", "Mow the lawn", Priority.Medium),
                    Task("shopping", "Buy the groceries", Priority.High),
                    Task("painting", "Paint the fence", Priority.Medium)
                )
            )
        }

        get("/") {
            call.respondText("Hello from Ktor")
        }
    }
}
