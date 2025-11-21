package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import java.util.Collections
import io.ktor.server.http.content.*
import com.example.model.*

import java.io.File

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val sessions = Collections.synchronizedList<WebSocketServerSession>(mutableListOf())

    routing {
        staticFiles("/static", File("/Users/Lucas.William.Bateson/Github/getting-started-with-ktor/05-websockets/src/main/resources/static"))
        
        get("/debug-resource") {
            val resource = this.javaClass.classLoader.getResource("static/index.html")
            call.respondText("Resource: $resource")
        }

        webSocket("/tasks") {
            for (task in TaskRepository.allTasks()) {
                sendSerialized(task)
                delay(1000)
            }
            close(CloseReason(CloseReason.Codes.NORMAL, "All tasks sent"))
        }

        webSocket("/tasks2") {
            sessions.add(this)
            try {
                // Send all existing tasks first
                for (task in TaskRepository.allTasks()) {
                    sendSerialized(task)
                }

                // Listen for new tasks
                while (true) {
                    val newTask = receiveDeserialized<Task>()
                    TaskRepository.addTask(newTask)
                    
                    // Broadcast to all sessions
                    synchronized(sessions) {
                        for (session in sessions) {
                            // Launch in a coroutine to avoid blocking? 
                            // For simplicity in this tutorial context, we might just call it directly 
                            // but sendSerialized is suspend, so we need to be careful.
                            // The prompt says: "For each session in sessions, session.sendSerialized(newTask)"
                            // We can't launch inside here easily without a scope, but we are in a suspend function.
                            // However, iterating and suspending might block receiving for this session if we are not careful.
                            // But since we are in the loop of *this* session receiving, we are the broadcaster.
                            // We should probably iterate and send.
                        }
                    }
                    // Actually, we need to iterate and send. 
                    // Since sendSerialized is suspend, we can just call it.
                    // But we should probably do it for ALL sessions including us? Yes.
                    
                    val activeSessions = synchronized(sessions) { sessions.toList() }
                    for (session in activeSessions) {
                        try {
                            session.sendSerialized(newTask)
                        } catch (e: Exception) {
                            // Handle disconnection
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                sessions.remove(this)
            }
        }

        get("/") {
            call.respondText("Hello from Ktor WebSockets")
        }
    }
}
